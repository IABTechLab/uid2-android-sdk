package com.uid2

import android.content.Context
import com.uid2.UID2ManagerState.Established
import com.uid2.UID2ManagerState.Expired
import com.uid2.UID2ManagerState.Invalid
import com.uid2.UID2ManagerState.NoIdentity
import com.uid2.UID2ManagerState.OptOut
import com.uid2.UID2ManagerState.RefreshExpired
import com.uid2.UID2ManagerState.Refreshed
import com.uid2.data.IdentityPackage
import com.uid2.data.IdentityStatus
import com.uid2.data.IdentityStatus.ESTABLISHED
import com.uid2.data.IdentityStatus.EXPIRED
import com.uid2.data.IdentityStatus.INVALID
import com.uid2.data.IdentityStatus.NO_IDENTITY
import com.uid2.data.IdentityStatus.OPT_OUT
import com.uid2.data.IdentityStatus.REFRESHED
import com.uid2.data.IdentityStatus.REFRESH_EXPIRED
import com.uid2.data.UID2Identity
import com.uid2.extensions.getMetadata
import com.uid2.network.DefaultNetworkSession
import com.uid2.network.NetworkSession
import com.uid2.storage.StorageManager
import com.uid2.utils.TimeUtils
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.retryWhen
import kotlinx.coroutines.flow.single
import kotlinx.coroutines.launch

/**
 * A listener interface allowing the consumer to be notified when either the identity or status of the identity changes
 * within the [UID2Manager].
 */
interface UID2ManagerIdentityChangedListener {

    /**
     * The identity or status of the identity has changed.
     *
     * @param identity If the status is [Established], or [Refreshed], this will represent the latest identity of the
     * user.
     * @param status The new status of the associated identity.
     */
    fun onIdentityStatusChanged(identity: UID2Identity?, status: IdentityStatus)
}

/**
 * A interface defining the flow of state communicated by the [UID2Manager].
 */
sealed interface UID2ManagerState {
    data class Established(val identity: UID2Identity) : UID2ManagerState
    data class Refreshed(val identity: UID2Identity) : UID2ManagerState
    object NoIdentity : UID2ManagerState
    data class Expired(val identity: UID2Identity) : UID2ManagerState
    object Invalid : UID2ManagerState
    object RefreshExpired : UID2ManagerState
    object OptOut : UID2ManagerState
}

/**
 * This class represents the key way to interact with the UID2 SDK. It is responsible for taking ownership of a given
 * [UID2Identity] and refreshes it when appropriate.
 *
 * The consuming application is expected to initialize the manager immediately upon Application creation. This can be
 * done by calling [UID2Manager.init]. This allows the manager to potentially load any previously stored identity
 * and make sure it's updated accordingly.
 *
 * After the manager is initialized, updates of the Identity or Status can be done via either the
 * [UID2ManagerIdentityChangedListener] or via [UID2Manager.state].
 */
class UID2Manager internal constructor(
    private val client: UID2Client,
    private val storageManager: StorageManager,
    private val timeUtils: TimeUtils,
    defaultDispatcher: CoroutineDispatcher,
    initialAutomaticRefreshEnabled: Boolean,
) {
    private val scope = CoroutineScope(defaultDispatcher + SupervisorJob())

    /**
     * Gets or sets the listener that will be notified when either the Identity or Identity Status changes.
     */
    var onIdentityChangedListener: UID2ManagerIdentityChangedListener? = null

    private val _state = MutableStateFlow<UID2ManagerState>(NoIdentity)

    /**
     * The flow representing the state of the UID2Manager.
     */
    val state: Flow<UID2ManagerState> = _state.asStateFlow()

    // The Job responsible for initialising the manager. This will include de-serialising our initial state from
    // storage.
    private var initialized: Job

    // An active Job that is scheduled to refresh the current identity
    private var refreshJob: Job? = null

    internal var checkExpiration: Boolean = true

    // The scheduled jobs to check identity expiration.
    private var checkRefreshExpiresJob: Job? = null
    private var checkIdentityExpiresJob: Job? = null

    /**
     * Gets the current Identity, if available.
     */
    val currentIdentity: UID2Identity?
        get() = when (val state = _state.value) {
            is Established -> state.identity
            is Refreshed -> state.identity
            is Expired -> state.identity
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

    /**
     * Gets or sets whether tha Manager will automatically refresh the Identity. Setting this to False will cancel any
     * pending refresh.
     */
    var automaticRefreshEnabled: Boolean = initialAutomaticRefreshEnabled
        set(value) {
            field = value
            checkIdentityRefresh()
        }

    init {
        initialized = scope.launch {
            // Attempt to load the Identity from storage. If successful, we can notify any observers.
            storageManager.loadIdentity().let {
                validateAndSetIdentity(it.first, it.second, false)
            }
        }
    }

    /**
     * Sets the current Identity.
     *
     * Once set, assuming it's valid, it will be monitored so that we automatically refresh the token(s) when required.
     * This will also be persisted locally, so that when the application re-launches, we reload this Identity.
     */
    fun setIdentity(identity: UID2Identity) = afterInitialized {
        validateAndSetIdentity(identity, null)
    }

    /**
     * If a valid Identity has been set, this will reset our state along with clearing any persisted data.
     */
    fun resetIdentity() = afterInitialized {
        currentIdentity ?: return@afterInitialized

        setIdentityInternal(null, NO_IDENTITY, true)
    }

    /**
     * Forces a refresh of the current Identity, if set.
     */
    fun refreshIdentity() = afterInitialized {
        // If we have a valid Identity, let's refresh it.
        currentIdentity?.let { refreshIdentityInternal(it) }
    }

    /**
     * Helper function to ensure a task is run **after** the manager has been fully initialised. This is to ensure that
     * our public interface is not exposed to any race conditions with us initialising/loading our state from disk.
     */
    private fun afterInitialized(run: () -> Unit) {
        if (initialized.isCompleted) {
            run()
            return
        }

        scope.launch {
            initialized.join()
            run()
        }
    }

    private fun refreshIdentityInternal(identity: UID2Identity) = scope.launch {
        try {
            refreshToken(identity).retryWhen { _, attempt ->
                // The delay between retry attempts is based upon how many attempts we have previously had. After a
                // number of sequential failures, we will increase the delay.
                val delayMs = if (attempt < REFRESH_TOKEN_FAILURE_RETRY_THRESHOLD) {
                    REFRESH_TOKEN_FAILURE_RETRY_SHORT_MS
                } else {
                    REFRESH_TOKEN_FAILURE_RETRY_LONG_MS
                }

                delay(delayMs)

                // Keep trying to automatically refresh the identity, while it's considered valid.
                getIdentityPackage(identity, false).valid
            }.single().let {
                    result ->
                validateAndSetIdentity(result.identity, result.status)
            }
        } catch (_: UID2Exception) {
            // This will happen after we decide to no longer try to update the identity, e.g. it's no longer valid.
        }
    }

    private fun cstgInternal(cstgEnvelope: String) = scope.launch {
        try {

            actualCstgCall(cstgEnvelope).single().let {
                    result ->
//                validateAndSetIdentity(result.identity, result.status)
            }
        } catch (_: UID2Exception) {
            // This will happen after we decide to no longer try to update the identity, e.g. it's no longer valid.
        }
    }

    private suspend fun actualCstgCall(cstgEnvelope: String): Flow<RefreshResult> = flow {
        try {
            client.cstg(cstgEnvelope)
//            emit(RefreshResult(response.identity, response.status))
        } catch (ex: Exception) {
            throw UID2Exception("Error refreshing token", ex)
        }
    }



    /**
     * Gets the current Advertising Token, if available.
     */
    fun getAdvertisingToken(): String? = currentIdentity?.let {
        // For a known identity, we should only provide the advertising token if their status is established or
        // refreshed. It's possible they could have expired and be pending a refresh. In this case, the token is not
        // useful.
        return@let if (currentIdentityStatus == ESTABLISHED || currentIdentityStatus == REFRESHED) {
            it.advertisingToken
        } else {
            null
        }
    }

    private fun setIdentityInternal(identity: UID2Identity?, status: IdentityStatus, updateStorage: Boolean = true) {
        if (updateStorage) {
            scope.launch {
                if (identity == null) {
                    storageManager.clear()
                } else {
                    storageManager.saveIdentity(identity, status)
                }
            }
        }

        // Update the current identity.
        _state.tryEmit(getManagerState(identity, status))

        // If we have an attached listener, report.
        onIdentityChangedListener?.onIdentityStatusChanged(identity, status)

        // An identity's status can change based upon the current time and it's expiration. We will schedule some work
        // to detect when it changes so that we can report it accordingly.
        checkIdentityExpiration()

        // After a new identity has been set, we have to work out how we're going to potentially refresh it. If the
        // identity is null, because it's been reset of the identity has opted out, we don't need to do anything.
        checkIdentityRefresh()
    }

    private fun checkIdentityRefresh() {
        refreshJob?.cancel()
        refreshJob = null

        if (!automaticRefreshEnabled) {
            return
        }

        currentIdentity?.let {
            // If the identity is already suitable for a refresh, we can do so immediately. Otherwise, we will work out
            // how long it is until a refresh is required and schedule it accordingly.
            refreshJob = if (timeUtils.hasExpired(it.refreshFrom)) {
                refreshIdentityInternal(it)
            } else {
                scope.launch {
                    val timeToRefresh = timeUtils.diffToNow(it.refreshFrom)
                    delay(timeToRefresh)
                    refreshIdentityInternal(it)
                }
            }
        }
    }

    /**
     * The identity status can change as we reach specific time events. We want to observe these and make sure that when
     * they are reached, we can report them accordingly to our consumer.
     */
    private fun checkIdentityExpiration() {
        checkRefreshExpiresJob?.cancel()
        checkRefreshExpiresJob = null

        checkIdentityExpiresJob?.cancel()
        checkIdentityExpiresJob = null

        if (!checkExpiration) {
            return
        }

        currentIdentity?.let {
            // If the expiration time of being able to refresh is in the future, we will schedule a job to detect if we
            // pass it. This will allow us to reevaluate our state and update accordingly.
            if (!timeUtils.hasExpired(it.refreshExpires)) {
                checkRefreshExpiresJob = scope.launch {
                    val timeToCheck = timeUtils.diffToNow(it.refreshExpires) + EXPIRATION_CHECK_TOLERANCE_MS
                    delay(timeToCheck)
                    validateAndSetIdentity(it, null, true)
                }
            }

            // If the expiration time of the identity itself is in the future, we will schedule a job to detect if we
            // pass it. This will allow us to reevaluate our state and update accordingly.
            if (!timeUtils.hasExpired(it.identityExpires)) {
                checkIdentityExpiresJob = scope.launch {
                    val timeToCheck = timeUtils.diffToNow(it.identityExpires) + EXPIRATION_CHECK_TOLERANCE_MS
                    delay(timeToCheck)
                    validateAndSetIdentity(it, null, true)
                }
            }
        }
    }

    private fun validateAndSetIdentity(
        identity: UID2Identity?,
        status: IdentityStatus?,
        updateStorage: Boolean = true,
    ) {
        // Process Opt Out.
        if (status == OPT_OUT) {
            setIdentityInternal(null, OPT_OUT)
            return
        }

        // Check to see the validity of the Identity, updating our internal state.
        val validity = getIdentityPackage(identity, currentIdentity == null)
        setIdentityInternal(validity.identity, validity.status, updateStorage)
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
     * The different results from refreshing an identity.
     */
    private data class RefreshResult(val identity: UID2Identity?, val status: IdentityStatus)
    private data class CstgResult(val result: String)

    /**
     * Refreshes the given Identity.
     */
    private suspend fun refreshToken(identity: UID2Identity): Flow<RefreshResult> = flow {
        try {
            val response = client.refreshIdentity(identity.refreshToken, identity.refreshResponseKey)
            emit(RefreshResult(response.identity, response.status))
        } catch (ex: Exception) {
            throw UID2Exception("Error refreshing token", ex)
        }
    }

    // TODO should i use = afterInitialized on the function declaration?
    suspend fun cstg(dii: String) {
        try {
            client.cstg(dii)
//            cstgInternal(cstgEnvelope)
//            emit(RefreshResult(response.identity, response.status))
        } catch (ex: Exception) {
            throw UID2Exception("Error refreshing token", ex)
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

        // The number of milliseconds to wait before retrying after failing to refresh a token is dependent on the
        // number of consecutive failures we've had. After a threshold, we will increase the time.
        private const val REFRESH_TOKEN_FAILURE_RETRY_THRESHOLD = 5
        private const val REFRESH_TOKEN_FAILURE_RETRY_SHORT_MS = 5000L
        private const val REFRESH_TOKEN_FAILURE_RETRY_LONG_MS = 60000L // 1 minute

        // The additional time we will allow to pass before checking the expiration of the Identity.
        private const val EXPIRATION_CHECK_TOLERANCE_MS = 50

        private var api: String = UID2_API_URL_DEFAULT
        private var networkSession: NetworkSession = DefaultNetworkSession()
        private var storageManager: StorageManager? = null

        private var instance: UID2Manager? = null

        /**
         * Initializes the class with the given [Context].
         */
        @JvmStatic
        fun init(context: Context) = init(context, DefaultNetworkSession())

        /**
         * Initializes the class with the given [Context], along with a [NetworkSession] that will be responsible
         * for making any required network calls.
         *
         * @param context The context to initialise from. This will be used to obtain the package's metadata to extract
         * the API URL.
         * @param networkSession A custom [NetworkSession] which can be used for making any required network calls.
         * The default implementation supported by the SDK can be found as [DefaultNetworkSession].
         */
        @JvmStatic
        @Throws(InitializationException::class)
        fun init(context: Context, networkSession: NetworkSession) {
            if (instance != null) {
                throw InitializationException()
            }

            val metadata = context.getMetadata()
            // TODO get package name
//            val packageName = context.packageName
            this.api = metadata?.getString(UID2_API_URL_KEY, UID2_API_URL_DEFAULT) ?: UID2_API_URL_DEFAULT
            this.networkSession = networkSession
            this.storageManager = StorageManager.getInstance(context)
        }

        /**
         * Returns True if the manager is already initialised, otherwise False.
         */
        @JvmStatic
        fun isInitialized() = instance != null

        /**
         * Gets the current singleton instance of the manager.
         *
         * @throws InitializationException Thrown if the manager has not yet been initialised.
         */
        @JvmStatic
        fun getInstance(): UID2Manager {
            val storage = storageManager ?: throw InitializationException()

            return instance ?: UID2Manager(
                UID2Client(
                    api,
                    networkSession,
                ),
                storage,
                TimeUtils(),
                Dispatchers.Default,
                true,
            ).apply {
                instance = this
            }
        }

        /**
         * Helper function that translates a given Identity and IdentityStatus into it's Manager State.
         */
        internal fun getManagerState(identity: UID2Identity?, status: IdentityStatus): UID2ManagerState {
            val converted = when (status) {
                ESTABLISHED -> identity?.let { return Established(it) }
                REFRESHED -> identity?.let { return Refreshed(it) }
                NO_IDENTITY -> NoIdentity
                EXPIRED -> identity?.let { return Expired(it) }
                INVALID -> Invalid
                REFRESH_EXPIRED -> RefreshExpired
                OPT_OUT -> OptOut
            }

            return converted ?: Invalid
        }
    }
}
