package com.uid2.dev

import android.app.Application
import android.util.Log
import com.uid2.UID2Manager

class GMADevApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        // Initialise the UID2Manager class. We will use it's DefaultNetworkSession rather than providing our own
        // custom implementation. This can be done to allow wrapping something like OkHttp.
        try {
            UID2Manager.init(
                context = this,
                isLoggingEnabled = true,
            )
        } catch (ex: Exception) {
            Log.e("IMADevApplication", "Error initialising UID2Manager", ex)
        }
    }
}
