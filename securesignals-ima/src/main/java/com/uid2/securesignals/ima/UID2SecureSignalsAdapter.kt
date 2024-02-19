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
public class UID2SecureSignalsException(message: String? = null, cause: Throwable? = null) : Exception(message, cause)

/**
 * An implementation of Google's IMA SecureSignalsAdapter that integrates UID2 tokens, accessed via the UID2Manager.
 */
public class UID2SecureSignalsAdapter : SecureSignalsAdapter {

    /**
     * Gets the version of the UID2 SDK.
     */
    public override fun getSDKVersion(): VersionInfo = UID2.getVersionInfo().let {
        VersionInfo(it.major, it.minor, it.patch)
    }

    /**
     * Gets the version of the UID2 Secure Signals plugin.
     */
    public override fun getVersion(): VersionInfo = PluginVersion.getVersionInfo().let {
        VersionInfo(it.major, it.minor, it.patch)
    }

    /**
     * Initialises the UID2 SDK with the given Context.
     */
    public override fun initialize(context: Context, callback: SecureSignalsInitializeCallback) {
        // It's possible that the UID2Manager is already initialised. If so, it's a no-op.
        if (!UID2Manager.isInitialized()) {
            UID2Manager.init(context)
        }

        callback.onSuccess()
    }

    /**
     * Collects the UID2 advertising token, if available.
     */
    public override fun collectSignals(context: Context, callback: SecureSignalsCollectSignalsCallback) {
        UID2Manager.getInstance().let { manager ->
            val token = manager.getAdvertisingToken()
            if (token != null) {
                callback.onSuccess(token)
            } else {
                // We include the IdentityStatus in the "error" to have better visibility on why the Advertising Token
                // was not present. There are a number of valid reasons why we don't have a token, but we are still
                // required to report these as "failures".
                callback.onFailure(
                    UID2SecureSignalsException(
                        "No Advertising Token available (Status: ${manager.currentIdentityStatus.value})",
                    ),
                )
            }
        }
    }
}
