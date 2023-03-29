package com.uid2.securesignals.ima

import android.content.Context
import com.google.ads.interactivemedia.v3.api.VersionInfo
import com.google.ads.interactivemedia.v3.api.signals.SecureSignalsAdapter
import com.google.ads.interactivemedia.v3.api.signals.SecureSignalsCollectSignalsCallback
import com.google.ads.interactivemedia.v3.api.signals.SecureSignalsInitializeCallback
import com.uid2.UID2
import com.uid2.UID2Manager

/**
 * A custom exception type that is used to report failures from the UID2SecureSignalsAdapter when an error has occurred.
 */
class UID2SecureSignalsException(message: String? = null, cause: Throwable? = null): Exception(message, cause)

/**
 * An implementation of Google's IMA SecureSignalsAdapter that integrates UID2 tokens, accessed via the UID2Manager.
 */
class UID2SecureSignalsAdapter: SecureSignalsAdapter {

    /**
     * Gets the version of the UID2 SDK.
     */
    override fun getSDKVersion(): VersionInfo = UID2.getVersionInfo().let {
        VersionInfo(it.major, it.minor, it.patch)
    }

    /**
     * Gets the version of the UID2 Secure Signals plugin.
     */
    override fun getVersion(): VersionInfo = PluginVersion.getVersionInfo().let {
        VersionInfo(it.major, it.minor, it.patch)
    }

    /**
     * Initialises the UID2 SDK with the given Context.
     */
    override fun initialize(context: Context?, callback: SecureSignalsInitializeCallback?) {
        if (context != null) {
            UID2Manager.init(context)
            callback?.onSuccess()
        } else if (UID2Manager.isInitialized()) {
            callback?.onSuccess()
        } else {
            callback?.onFailure(UID2SecureSignalsException("No Context provided to initialise UID2Manager"))
        }
    }

    /**
     * Collects the UID2 advertising token, if available.
     */
    override fun collectSignals(context: Context?, callback: SecureSignalsCollectSignalsCallback?) {
        val token = UID2Manager.getInstance().getAdvertisingToken()
        if (token != null) {
            callback?.onSuccess(token)
        } else {
            callback?.onFailure(UID2SecureSignalsException("No Advertising Token available"))
        }
    }
}
