package com.uid2.dev

import android.app.Application
import com.uid2.UID2Manager
import com.uid2.dev.network.OkNetworkSession

class DevApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        // Initialise the UID2Manager class. We will use it's DefaultNetworkSession rather than providing our own
        // custom implementation. This can be done to allow wrapping something like OkHttp.
        UID2Manager.init(this.applicationContext)

        // Alternatively, we could initialise the UID2Manager with our own custom NetworkSession...
        //UID2Manager.init(this.applicationContext, OkNetworkSession())
    }
}
