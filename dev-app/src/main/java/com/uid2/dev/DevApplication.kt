package com.uid2.dev

import android.app.Application
import android.os.StrictMode
import android.util.Log
import com.uid2.EUIDManager
import com.uid2.UID2Manager
import com.uid2.dev.utils.getMetadata
import com.uid2.dev.utils.isEnvironmentEUID
import com.uid2.prebid.UID2Prebid
import org.prebid.mobile.PrebidMobile

class DevApplication : Application() {
    private lateinit var prebid: UID2Prebid

    override fun onCreate() {
        super.onCreate()

        val isEnvironmentEUID = getMetadata().isEnvironmentEUID()

        // Initialise the UID2Manager class. We will use it's DefaultNetworkSession rather than providing our own
        // custom implementation. This can be done to allow wrapping something like OkHttp.
        if (isEnvironmentEUID) {
            EUIDManager.init(
                context = this,
                EUIDManager.Environment.Custom(EUID_INTEG_SERVER_URL),
                isLoggingEnabled = true,
            )
        } else {
            UID2Manager.init(
                context = this,
                UID2Manager.Environment.Custom(UID2_INTEG_SERVER_URL),
                isLoggingEnabled = true,
            )
        }

        // Alternatively, we could initialise the UID2Manager with our own custom NetworkSession...
        // UID2Manager.init(this, INTEG_SERVER_URL, OkNetworkSession(), true)

        // Create the Prebid integration and allow it to start observing the UID2Manager instance.
        PrebidMobile.initializeSdk(this) { Log.i(TAG, "Prebid: $it") }
        prebid = UID2Prebid(
            if (isEnvironmentEUID) EUIDManager.getInstance() else UID2Manager.getInstance(),
        ).apply {
            initialize()
        }

        // For the development app, we will enable a strict thread policy to ensure we have suitable visibility of any
        // issues within the SDK.
        enableStrictMode()
    }

    private fun enableStrictMode() {
        StrictMode.setThreadPolicy(
            StrictMode.ThreadPolicy.Builder().apply {
                detectDiskReads()
                detectDiskWrites()
                detectNetwork()
                penaltyLog()
            }.build(),
        )
    }

    private companion object {
        const val TAG = "DevApplication"

        const val UID2_INTEG_SERVER_URL = "https://operator-integ.uidapi.com"
        const val EUID_INTEG_SERVER_URL = "https://integ.euid.eu/v2"
    }
}
