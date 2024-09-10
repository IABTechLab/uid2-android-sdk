package com.uid2

import android.content.Context
import com.uid2.UID2Manager.Companion.APPLICATION_ID_DEFAULT
import com.uid2.network.DefaultNetworkSession
import com.uid2.network.NetworkSession
import com.uid2.storage.FileStorageManager
import com.uid2.storage.FileStorageManager.Store.EUID
import com.uid2.storage.StorageManager
import com.uid2.utils.InputUtils
import com.uid2.utils.Logger
import com.uid2.utils.TimeUtils
import kotlinx.coroutines.Dispatchers

public object EUIDManager {

    public sealed interface Environment {
        public val serverUrl: String

        /**
         * AWS EU West 2 (London).
         */
        public data object London : Environment {
            override val serverUrl: String = EUID_API_URL_PRODUCTION
        }

        /**
         * The default Environment, equivalent to [London].
         */
        public data object Production : Environment {
            override val serverUrl: String = EUID_API_URL_PRODUCTION
        }

        /**
         * An Environment with its own API endpoint, such as for integration testing.
         */
        public data class Custom(
            override val serverUrl: String,
        ) : Environment
    }

    // The default API server.
    internal const val EUID_API_URL_PRODUCTION = "https://prod.euid.eu/v2"

    private var serverUrl: String = EUID_API_URL_PRODUCTION
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
     * @param environment The API Environment to use.
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
        this.storageManager = FileStorageManager(context.applicationContext, EUID)
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
        if (storageManager == null) {
            throw InitializationException()
        }
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
}
