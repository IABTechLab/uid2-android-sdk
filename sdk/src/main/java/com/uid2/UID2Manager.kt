package com.uid2

import android.content.Context
import com.uid2.UID2ManagerState.Established
import com.uid2.UID2ManagerState.Expired
import com.uid2.UID2ManagerState.Invalid
import com.uid2.UID2ManagerState.Loading
import com.uid2.UID2ManagerState.NoIdentity
import com.uid2.UID2ManagerState.OptOut
import com.uid2.UID2ManagerState.RefreshExpired
import com.uid2.UID2ManagerState.Refreshed
import com.uid2.data.IdentityPackage
import com.uid2.data.IdentityRequest
import com.uid2.data.IdentityRequest.Email
import com.uid2.data.IdentityRequest.Phone
import com.uid2.data.IdentityStatus
import com.uid2.data.IdentityStatus.ESTABLISHED
import com.uid2.data.IdentityStatus.EXPIRED
import com.uid2.data.IdentityStatus.INVALID
import com.uid2.data.IdentityStatus.NO_IDENTITY
import com.uid2.data.IdentityStatus.OPT_OUT
import com.uid2.data.IdentityStatus.REFRESHED
import com.uid2.data.IdentityStatus.REFRESH_EXPIRED
import com.uid2.data.UID2Identity
import com.uid2.network.DefaultNetworkSession
import com.uid2.network.NetworkSession
import com.uid2.storage.FileStorageManager
import com.uid2.storage.FileStorageManager.Store.UID2
import com.uid2.storage.StorageManager
import com.uid2.utils.InputUtils
import com.uid2.utils.Logger
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
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * A listener interface allowing the consumer to be notified when either the identity or status of the identity changes
 * within the [UID2Manager].
 */
public interface UID2ManagerIdentityChangedListener {

    /**
     * The identity or status of the identity has changed.
     *
     * @param identity If the status is [Established], or [Refreshed], this will represent the latest identity of the
     * user.
     * @param status The new status of the associated identity.
     */
    public fun onIdentityStatusChanged(identity: UID2Identity?, status: IdentityStatus)
}

/**
 * A interface defining the flow of state communicated by the [UID2Manager].
 */
public sealed interface UID2ManagerState {
    public data object Loading : UID2ManagerState
    public data class Established(val identity: UID2Identity) : UID2ManagerState
    public data class Refreshed(val identity: UID2Identity) : UID2ManagerState
    public data object NoIdentity : UID2ManagerState
    public data class Expired(val identity: UID2Identity) : UID2ManagerState
    public data object Invalid : UID2ManagerState
    public data object RefreshExpired : UID2ManagerState
    public data object OptOut : UID2ManagerState
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
public class UID2Manager internal constructor(
    private val client: UID2Client,
    private val storageManager: StorageManager,
    private val timeUtils: TimeUtils,
    private val inputUtils: InputUtils,
    defaultDispatcher: CoroutineDispatcher,
    initialAutomaticRefreshEnabled: Boolean,
    @property:InternalUID2Api public val logger: Logger,
) {
    private val scope = CoroutineScope(defaultDispatcher + SupervisorJob())

    /**
     * Gets or sets the listener that will be notified when either the Identity or Identity Status changes.
     */
    public var onIdentityChangedListener: UID2ManagerIdentityChangedListener? = null

    private val _state = MutableStateFlow<UID2ManagerState>(Loading)

    /**
     * The flow representing the state of the UID2Manager.
     */
    public val state: Flow<UID2ManagerState> = _state.asStateFlow()

    // The Job responsible for initialising the manager. This will include de-serialising our initial state from
    // storage. We allow consumers to attach a listener to detect when this Job is complete.
    private var initialized: Job
    private val onInitializedListeners = mutableListOf<() -> Unit>()
    private val initializedLock = Mutex()

    // An active Job that is scheduled to refresh the current identity
    private var refreshJob: Job? = null

    internal var checkExpiration: Boolean = true

    // The scheduled jobs to check identity expiration.
    private var checkRefreshExpiresJob: Job? = null
    private var checkIdentityExpiresJob: Job? = null

    /**
     * Gets the current Identity, if available.
     */
    public val currentIdentity: UID2Identity?
        get() = when (val state = _state.value) {
            is Established -> state.identity
            is Refreshed -> state.identity
            is Expired -> state.identity
            else -> null
        }

    /**
     * Gets whether or not [UID2Manager] has a known [UID2Identity]. If not, a new identity should be generated either
     * via [generateIdentity] or generated externally and set via [setIdentity].
     */
    public fun hasIdentity(): Boolean = currentIdentity != null

    /**
     * Gets the current Identity Status.
     */
    public val currentIdentityStatus: IdentityStatus
        get() = when (_state.value) {
            is Loading -> NO_IDENTITY // Not available yet.
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
    public var automaticRefreshEnabled: Boolean = initialAutomaticRefreshEnabled
        set(value) {
            field = value
            checkIdentityRefresh()
        }

    /**
     * Adds a listener which can be used to determine if the [UID2Manager] instance has finished initializing.
     * Initializing includes any time required to restore a previously persisted [UID2Identity] from storage.
     *
     * If a listener is added *after* initialization is complete, the callback will be invoked immediately.
     */
    public fun addOnInitializedListener(listener: () -> Unit): UID2Manager = apply {
        runBlocking {
            initializedLock.withLock {
                // If we've already finished initializing, we should immediately invoke the callback.
                if (initialized.isCompleted) {
                    listener()
                } else {
                    onInitializedListeners += listener
                }
            }
        }
    }

    init {
        initialized = scope.launch {
            // Attempt to load the Identity from storage. If successful, we can notify any observers.
            storageManager.loadIdentity().let {
                if (it.first != null) {
                    logger.i(TAG) { "Restoring previously persisted identity" }
                }

                validateAndSetIdentity(it.first, it.second, false)
            }

            onInitialized()
        }
    }

    /**
     * Represents the result of a request to [generateIdentity].
     */
    public sealed class GenerateIdentityResult {

        /**
         * The identity was generated successfully and the [UID2Manager] as updated.
         */
        public data object Success : GenerateIdentityResult()

        /**
         * The generation of the identity failed.
         *
         * @param ex The exception which caused the request to fail.
         */
        public data class Error(public val ex: UID2Exception) : GenerateIdentityResult()
    }

    /**
     * Generates a new identity.
     *
     * Once set, assuming it's valid, it will be monitored so that we automatically refresh the token(s) when required.
     * This will also be persisted locally, so that when the application re-launches, we reload this Identity.
     *
     * @param identityRequest The identify for which the [UID2Identity] is required for.
     * @param subscriptionId The subscription id that was obtained when configuring your account.
     * @param publicKey The public key that was obtained when configuring your account.
     *
     * @throws InputValidationException Thrown if the given [IdentityRequest] is not valid. For a
     * [IdentityRequest.Phone] we expect the given number to conform to the ITU E.164 Standard
     * (https://en.wikipedia.org/wiki/E.164).
     */
    @Throws(InputValidationException::class)
    public fun generateIdentity(
        identityRequest: IdentityRequest,
        subscriptionId: String,
        publicKey: String,
        onResult: (GenerateIdentityResult) -> Unit,
    ): Unit = afterInitialized {
        // Normalize any given input to validate it.
        val request = when (identityRequest) {
            is Email -> inputUtils.normalize(identityRequest)
            is Phone -> inputUtils.normalize(identityRequest)
            else -> identityRequest
        }

        scope.launch {
            try {
                // Attempt to generate the new identity.
                val identity = client.generateIdentity(request, subscriptionId, publicKey)

                // Cancel any in-flight refresh job that could be processing a previously set identity.
                refreshJob?.cancel()
                refreshJob = null

                // Update our identity.
                validateAndSetIdentity(identity.identity, identity.status)

                // Report our result.
                onResult(GenerateIdentityResult.Success)
            } catch (ex: UID2Exception) {
                // The identity generation failed, so we will not modify our current state and report this to the
                // caller.
                onResult(GenerateIdentityResult.Error(ex))
            }
        }
    }

    /**
     * Sets the current Identity.
     *
     * Once set, assuming it's valid, it will be monitored so that we automatically refresh the token(s) when required.
     * This will also be persisted locally, so that when the application re-launches, we reload this Identity.
     */
    public fun setIdentity(identity: UID2Identity): Unit = afterInitialized {
        logger.i(TAG) { "Setting external identity" }
        validateAndSetIdentity(identity, null)
    }

    /**
     * If a valid Identity has been set, this will reset our state along with clearing any persisted data.
     */
    public fun resetIdentity(): Unit = afterInitialized {
        currentIdentity ?: return@afterInitialized

        logger.i(TAG) { "Resetting identity" }
        setIdentityInternal(null, NO_IDENTITY, true)
    }

    /**
     * Forces a refresh of the current Identity, if set.
     */
    public fun refreshIdentity(): Unit = afterInitialized {
        // If we have a valid Identity, let's refresh it.
        currentIdentity?.let {
            logger.i(TAG) { "Refreshing identity" }
            refreshIdentityInternal(it)
        }
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

    /**
     * After initialization is complete, all the attached listeners will be invoked.
     */
    private suspend fun onInitialized() {
        initializedLock.withLock {
            while (onInitializedListeners.isNotEmpty()) {
                onInitializedListeners.removeAt(0).invoke()
            }
        }
    }

    private fun refreshIdentityInternal(identity: UID2Identity) = scope.launch {
        try {
            refreshToken(identity).retryWhen { _, attempt ->
                logger.i(TAG) { "Refreshing (Attempt: $attempt)" }

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
                logger.i(TAG) { "Successfully refreshed identity" }
                validateAndSetIdentity(result.identity, result.status)
            }
        } catch (ex: UID2Exception) {
            // This will happen after we decide to no longer try to update the identity, e.g. it's no longer valid.
            logger.e(TAG, ex) { "Error when trying to refresh identity" }
        }
    }

    /**
     * Gets the current Advertising Token, if available.
     */
    public fun getAdvertisingToken(): String? = currentIdentity?.let {
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

                    logger.i(TAG) { "Detected refresh has expired" }
                    validateAndSetIdentity(it, null, true)
                }
            }

            // If the expiration time of the identity itself is in the future, we will schedule a job to detect if we
            // pass it. This will allow us to reevaluate our state and update accordingly.
            if (!timeUtils.hasExpired(it.identityExpires)) {
                checkIdentityExpiresJob = scope.launch {
                    val timeToCheck = timeUtils.diffToNow(it.identityExpires) + EXPIRATION_CHECK_TOLERANCE_MS
                    delay(timeToCheck)

                    logger.i(TAG) { "Detected identity has expired" }
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
            logger.i(TAG) { "User opt-out detected" }
            setIdentityInternal(null, OPT_OUT)
            return
        }

        // Check to see the validity of the Identity, updating our internal state.
        val validity = getIdentityPackage(identity, currentIdentity == null)

        logger.i(TAG) {
            "Updating identity (Identity: ${validity.identity != null}, Status: ${validity.status}, " +
                "Updating Storage: $updateStorage)"
        }
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

    public sealed interface Environment {
        public val serverUrl: String

        /**
         * AWS US East (Ohio).
         */
        public data object Ohio : Environment {
            override val serverUrl: String = "https://prod.uidapi.com"
        }

        /**
         * AWS US West (Oregon).
         */
        public data object Oregon : Environment {
            override val serverUrl: String = "https://usw.prod.uidapi.com"
        }

        /**
         * AWS Asia Pacific (Singapore).
         */
        public data object Singapore : Environment {
            override val serverUrl: String = "https://sg.prod.uidapi.com"
        }

        /**
         * AWS Asia Pacific (Sydney).
         */
        public data object Sydney : Environment {
            override val serverUrl: String = "https://au.prod.uidapi.com"
        }

        /**
         * AWS Asia Pacific (Tokyo).
         */
        public data object Tokyo : Environment {
            override val serverUrl: String = "https://jp.prod.uidapi.com"
        }

        /**
         * The default Environment, equivalent to [Ohio].
         */
        public data object Production : Environment {
            override val serverUrl: String = UID2_API_URL_PRODUCTION
        }

        /**
         * An Environment with its own API endpoint, such as for integration testing.
         */
        public data class Custom(
            override val serverUrl: String,
        ) : Environment
    }

    public companion object {

        private const val TAG = "UID2Manager"

        // The default API server.
        internal const val UID2_API_URL_PRODUCTION = "https://prod.uidapi.com"

        internal const val APPLICATION_ID_DEFAULT = "unknown"

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

        private var serverUrl: String = UID2_API_URL_PRODUCTION
        private var applicationId: String = APPLICATION_ID_DEFAULT
        private var networkSession: NetworkSession = DefaultNetworkSession()
        private var storageManager: StorageManager? = null
        private var isLoggingEnabled: Boolean = false

        private var instance: UID2Manager? = null

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
        @JvmOverloads
        @JvmName("initWithEnvironment")
        @Throws(InitializationException::class)
        @Deprecated(
            message = "Initialize with a custom Environment rather than a serverUrl String",
            replaceWith = ReplaceWith("initWithEnvironment(context, environment, networkSession, isLoggingEnabled)"),
            level = DeprecationLevel.WARNING,
        )
        public fun init(
            context: Context,
            serverUrl: String = UID2_API_URL_PRODUCTION,
            networkSession: NetworkSession = DefaultNetworkSession(),
            isLoggingEnabled: Boolean = false,
        ) {
            init(context, Environment.Custom(serverUrl), networkSession, isLoggingEnabled)
        }

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
        @JvmOverloads
        @Throws(InitializationException::class)
        public fun init(
            context: Context,
            environment: Environment = Environment.Production,
            networkSession: NetworkSession = DefaultNetworkSession(),
            isLoggingEnabled: Boolean = false,
        ) {
            if (instance != null) {
                throw InitializationException()
            }

            this.serverUrl = environment.serverUrl
            this.applicationId = context.packageName
            this.networkSession = networkSession
            this.storageManager = FileStorageManager(context.applicationContext, UID2)
            this.isLoggingEnabled = isLoggingEnabled
        }

        /**
         * Returns True if the manager is already initialised, otherwise False.
         */
        @JvmStatic
        public fun isInitialized(): Boolean = instance != null

        /**
         * Gets the current singleton instance of the manager.
         *
         * @throws InitializationException Thrown if the manager has not yet been initialised.
         */
        @JvmStatic
        public fun getInstance(): UID2Manager {
            val storage = storageManager ?: throw InitializationException()
            val logger = Logger(isLoggingEnabled)

            return instance ?: UID2Manager(
                UID2Client(
                    apiUrl = serverUrl,
                    session = networkSession,
                    applicationId = applicationId,
                    logger = logger,
                ),
                storage,
                TimeUtils,
                InputUtils(),
                Dispatchers.Default,
                true,
                logger,
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
