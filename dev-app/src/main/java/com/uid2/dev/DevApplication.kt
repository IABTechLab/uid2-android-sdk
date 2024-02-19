package com.uid2.dev

import android.app.Application
import android.os.StrictMode
import com.uid2.UID2Manager

class DevApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        // Initialise the UID2Manager class. We will use it's DefaultNetworkSession rather than providing our own
        // custom implementation. This can be done to allow wrapping something like OkHttp.
        UID2Manager.init(this.applicationContext)

        // Alternatively, we could initialise the UID2Manager with our own custom NetworkSession...
        // UID2Manager.init(this.applicationContext, OkNetworkSession())

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
}
