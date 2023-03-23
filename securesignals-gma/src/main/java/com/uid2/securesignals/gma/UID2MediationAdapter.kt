package com.uid2.securesignals.gma

import android.content.Context
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.mediation.InitializationCompleteCallback
import com.google.android.gms.ads.mediation.MediationConfiguration
import com.google.android.gms.ads.mediation.rtb.RtbAdapter
import com.google.android.gms.ads.mediation.rtb.RtbSignalData
import com.google.android.gms.ads.mediation.rtb.SignalCallbacks
import com.uid2.UID2
import com.uid2.UID2Manager

/**
 * An implementation of Google's GMS RtbAdapter that integrates UID2 tokens, accessed via the UID2Manager.
 */
class UID2MediationAdapter : RtbAdapter() {

    /**
     * Gets the version of the UID2 SDK.
     */
    @Suppress("DEPRECATION")
    override fun getSDKVersionInfo() = UID2.getVersionInfo().let {
        com.google.android.gms.ads.mediation.VersionInfo(it.major, it.minor, it.patch)
    }

    /**
     * Gets the version of the UID2 Secure Signals plugin.
     */
    @Suppress("DEPRECATION")
    override fun getVersionInfo() = UID2SecureSignals.getVersionInfo().let {
        com.google.android.gms.ads.mediation.VersionInfo(it.major, it.minor, it.patch)
    }

    /**
     * Initialises the UID2 SDK with the given Context.
     */
    override fun initialize(
        context: Context,
        initializationCompleteCallback: InitializationCompleteCallback,
        mediationConfigurations: MutableList<MediationConfiguration>
    ) {
        // It's possible that the UID2Manager is already initialised. If so, it's a no-op.
        if (!UID2Manager.isInitialized()) {
            UID2Manager.init(context)
        }

        initializationCompleteCallback.onInitializationSucceeded()
    }

    /**
     * Collects the UID2 advertising token, if available.
     */
    override fun collectSignals(rtbSignalData: RtbSignalData, signalCallbacks: SignalCallbacks) {
        val token = UID2Manager.getInstance().getAdvertisingToken()
        if (token != null) {
            signalCallbacks.onSuccess(token)
        } else {
            signalCallbacks.onFailure(AdError(0, "No Advertising Token", "UID2"))
        }
    }
}