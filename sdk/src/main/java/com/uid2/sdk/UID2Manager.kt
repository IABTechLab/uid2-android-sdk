package com.uid2.sdk

import android.content.Context
import com.uid2.sdk.UID2ManagerState.Established
import com.uid2.sdk.UID2ManagerState.Expired
import com.uid2.sdk.UID2ManagerState.Invalid
import com.uid2.sdk.UID2ManagerState.NoIdentity
import com.uid2.sdk.UID2ManagerState.OptOut
import com.uid2.sdk.UID2ManagerState.RefreshExpired
import com.uid2.sdk.UID2ManagerState.Refreshed
import com.uid2.sdk.data.IdentityPackage
import com.uid2.sdk.data.IdentityStatus
import com.uid2.sdk.data.IdentityStatus.ESTABLISHED
import com.uid2.sdk.data.IdentityStatus.EXPIRED
import com.uid2.sdk.data.IdentityStatus.INVALID
import com.uid2.sdk.data.IdentityStatus.NO_IDENTITY
import com.uid2.sdk.data.IdentityStatus.OPT_OUT
import com.uid2.sdk.data.IdentityStatus.REFRESHED
import com.uid2.sdk.data.IdentityStatus.REFRESH_EXPIRED
import com.uid2.sdk.data.UID2Identity
import com.uid2.sdk.extensions.getMetadata
import com.uid2.sdk.network.DefaultNetworkSession
import com.uid2.sdk.network.NetworkSession
import com.uid2.sdk.storage.StorageManager
import com.uid2.sdk.utils.TimeUtils
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * A listener interface allowing the consumer to be notified when either the identity or status of the identity changes
 * within the UID2Manager.
 */
interface UID2ManagerIdentityChangedListener {
    fun onIdentityStatusChanged(identity: UID2Identity?, status: IdentityStatus)
}

/**
 * A interface defining the flow of state communicated by the UID2Manager.
 */
sealed interface UID2ManagerState {
    data class Established(val identity: UID2Identity) : UID2ManagerState
    data class Refreshed(val identity: UID2Identity) : UID2ManagerState
    object NoIdentity : UID2ManagerState
    object Expired : UID2ManagerState
    object Invalid : UID2ManagerState
    object RefreshExpired : UID2ManagerState
    object OptOut : UID2ManagerState
}

class UID2Manager internal constructor(
    private val client: UID2Client,
    private val storageManager: StorageManager,
    private val timeUtils: TimeUtils,
    defaultDispatcher: CoroutineDispatcher
) {
    private val scope = CoroutineScope(defaultDispatcher + SupervisorJob())

    // A listener that will be notified when either the Identity or Identity Status changes.
    var onIdentityChangedListener: UID2ManagerIdentityChangedListener? = null

    // The flow representing the state of the UID2Manager.
    private val _state = MutableStateFlow<UID2ManagerState>(NoIdentity)
    val state: Flow<UID2ManagerState> = _state.asStateFlow()

    /**
     * Gets the current Identity, if available.
     */
    val currentIdentity: UID2Identity?
        get() = when(val state = _state.value) {
            is Established -> state.identity
            is Refreshed -> state.identity
            else -> null
    }

    /**
     * Gets the current Identity Status.
     */
    val currentIdentityStatus: IdentityStatus
        get() = when (_state.value) {
            is Established -> ESTABLISHED
            is Refreshed -> REFRESHED
            is NoIdentity -> NO_IDENTITY
            is Expired -> EXPIRED
            is Invalid -> INVALID
            is RefreshExpired -> REFRESH_EXPIRED
            is OptOut -> OPT_OUT
    }

    init {
        // Attempt to load the Identity from storage. If successful, we can notify any observers.
        storageManager.loadIdentity()?.let {
            setIdentityInternal(it, ESTABLISHED, false)

            // TODO: Monitor of expiration.
        }
    }

    /**
     * Sets the current Identity.
     *
     * Once set, assuming it's valid, it will be monitored so that we automatically refresh the token(s) when required.
     * This will also be persisted locally, so that when the application re-launches, we reload this Identity.
     */
    fun setIdentity(identity: UID2Identity) {
        validateAndSetIdentity(identity, null)?.let {
            // TODO: Monitor for expiration.
        }
    }

    /**
     * If a valid Identity has been set, this will reset our state along with clearing any persisted data.
     */
    fun resetIdentity() {
        currentIdentity ?: return

        setIdentityInternal(null, NO_IDENTITY, true)
    }

    /**
     * Forces a refresh of the current Identity, if set.
     */
    fun refreshIdentity() {
        // If we have a valid Identity, let's refresh it.
        currentIdentity?.let {
            refreshToken(it)
        }
    }

    /**
     * Gets the current Advertising Token, if available.
     */
    fun getAdvertisingToken(): String? = currentIdentity?.advertisingToken

    private fun setIdentityInternal(identity: UID2Identity?, status: IdentityStatus, updateStorage: Boolean = true) {
        if (updateStorage) {
            if (identity == null) {
                storageManager.clear()
            } else {
                storageManager.saveIdentity(identity)
            }
        }

        // Update the current identity.
        _state.tryEmit(getManagerState(identity, status))

        // If we have an attached listener, report.
        onIdentityChangedListener?.onIdentityStatusChanged(identity, status)
    }

    private fun validateAndSetIdentity(identity: UID2Identity?, status: IdentityStatus?): UID2Identity? {
        // Process Opt Out.
        if (status == OPT_OUT) {
            setIdentityInternal(null, OPT_OUT)
            return null
        }

        // Check to see the validity of the Identity, updating our internal state.
        val validity = getIdentityPackage(identity, currentIdentity == null)
        setIdentityInternal(validity.identity, validity.status)

        return validity.identity
    }

    /**
     * Method to determine the validity of the given UID2Identity and it's appropriate status. In order to calculate
     * this correctly, we need to also know if this is considered a new identity.
     */
    private fun getIdentityPackage(identity: UID2Identity?, newIdentity: Boolean): IdentityPackage {
        // First check to see if we have a valid (available) Identity.
        identity ?: return IdentityPackage(false, PACKAGE_NOT_AVAILABLE, null, NO_IDENTITY)

        // Next, check that the Identity contains valid tokens.
        if (identity.advertisingToken.isEmpty()) {
            return IdentityPackage(false, PACKAGE_AD_TOKEN_NOT_AVAILABLE, null, INVALID)
        }
        if (identity.refreshToken.isEmpty()) {
            return IdentityPackage(false, PACKAGE_REFRESH_TOKEN_NOT_AVAILABLE, null, INVALID)
        }

        // The Identity contains some expiration details. Check to see if either have expired.
        if (timeUtils.hasExpired(identity.refreshExpires)) {
            return IdentityPackage(false, PACKAGE_REFRESH_EXPIRED, null, REFRESH_EXPIRED)
        }
        if (timeUtils.hasExpired(identity.identityExpires)) {
            return IdentityPackage(true, PACKAGE_IDENTITY_EXPIRED, identity, EXPIRED)
        }

        // Check to see if this is a new (established) identity.
        if (newIdentity) {
            return IdentityPackage(true, PACKAGE_IDENTITY_ESTABLISHED, identity, ESTABLISHED)
        }

        // Lastly, if this is a valid/identity for a previously known Identity, then it has been successfully refresh.
        return IdentityPackage(true, PACKAGE_IDENTITY_REFRESHED, identity, REFRESHED)
    }

    /**
     * Refreshes the given Identity.
     */
    private fun refreshToken(identity: UID2Identity): Job = scope.launch {
        try {
            val response = client.refreshIdentity(identity.refreshToken, identity.refreshResponseKey)
            validateAndSetIdentity(response.identity, response.status)
        } catch (_: Exception) {
            // If an error occurs while we try and refresh the Identity, we should attempt to retry.
            delay(REFRESH_TOKEN_FAILURE_RETRY_MS)
            refreshToken(identity)
        }
    }

    companion object {
        // The default API server.
        private const val UID2_API_URL_KEY = "uid2_api_url"
        private const val UID2_API_URL_DEFAULT = "https://prod.uidapi.com"

        private const val PACKAGE_NOT_AVAILABLE = "Identity not available"
        private const val PACKAGE_AD_TOKEN_NOT_AVAILABLE = "advertising_token is not available or is not valid"
        private const val PACKAGE_REFRESH_TOKEN_NOT_AVAILABLE = "refresh_token is not available or is not valid"
        private const val PACKAGE_REFRESH_EXPIRED = "Identity expired, refresh expired"
        private const val PACKAGE_IDENTITY_EXPIRED = "Identity expired, refresh still valid"
        private const val PACKAGE_IDENTITY_ESTABLISHED = "Identity established"
        private const val PACKAGE_IDENTITY_REFRESHED = "Identity refreshed"

        // The number of milliseconds to wait before retrying after failing to refresh a token.
        private const val REFRESH_TOKEN_FAILURE_RETRY_MS = 5000L

        private var api: String = UID2_API_URL_KEY
        private var networkSession: NetworkSession = DefaultNetworkSession()
        private var storageManager: StorageManager? = null

        private var instance: UID2Manager? = null

        @JvmStatic
        fun init(context: Context) = init(context, DefaultNetworkSession())

        @JvmStatic
        @Throws(InitializationException::class)
        fun init(context: Context, networkSession: NetworkSession) {
            if (instance != null) {
                throw InitializationException()
            }

            val metadata = context.getMetadata()

            this.api = metadata.getString(UID2_API_URL_KEY, UID2_API_URL_DEFAULT)
            this.networkSession = networkSession
            this.storageManager = StorageManager.getInstance(context)
        }

        @JvmStatic
        fun getInstance(): UID2Manager {
            val storage = storageManager ?: throw InitializationException()

            return instance ?: UID2Manager(
                UID2Client(
                    api,
                    networkSession
                ),
                storage,
                TimeUtils(),
                Dispatchers.Default
            ).apply {
                instance = this
            }
        }

        /**
         * Helper function that translates a given Identity and IdentityStatus into it's Manager State.
         */
        internal fun getManagerState(identity: UID2Identity?, status: IdentityStatus): UID2ManagerState {
            val converted = when(status) {
                ESTABLISHED -> identity?.let { return Established(it) }
                REFRESHED -> identity?.let { return Refreshed(it) }
                NO_IDENTITY -> NoIdentity
                EXPIRED -> Expired
                INVALID -> Invalid
                REFRESH_EXPIRED -> RefreshExpired
                OPT_OUT -> OptOut
            }

            return converted ?: Invalid
        }
    }
}
