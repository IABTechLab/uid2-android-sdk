package com.uid2.securesignals.gma

import android.content.Context
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.mediation.InitializationCompleteCallback
import com.google.android.gms.ads.mediation.MediationConfiguration
import com.google.android.gms.ads.mediation.rtb.RtbAdapter
import com.google.android.gms.ads.mediation.rtb.RtbSignalData
import com.google.android.gms.ads.mediation.rtb.SignalCallbacks
import com.uid2.EUIDManager
import com.uid2.UID2
import com.google.android.gms.ads.mediation.VersionInfo as GmaVersionInfo

/**
 * An implementation of Google's GMS RtbAdapter that integrates UID2 tokens, accessed via the UID2Manager.
 */
public class EUIDMediationAdapter : RtbAdapter() {

    /**
     * Gets the version of the UID2 SDK.
     */
    @Suppress("DEPRECATION")
    public override fun getSDKVersionInfo(): GmaVersionInfo = UID2.getVersionInfo().let {
        GmaVersionInfo(it.major, it.minor, it.patch)
    }

    /**
     * Gets the version of the UID2 Secure Signals plugin.
     */
    @Suppress("DEPRECATION")
    public override fun getVersionInfo(): GmaVersionInfo = PluginVersion.getVersionInfo().let {
        GmaVersionInfo(it.major, it.minor, it.patch)
    }

    /**
     * Initialises the UID2 SDK with the given Context.
     */
    override fun initialize(
        context: Context,
        initializationCompleteCallback: InitializationCompleteCallback,
        mediationConfigurations: MutableList<MediationConfiguration>,
    ) {
        // It's possible that the UID2Manager is already initialised. If so, it's a no-op.
        if (!EUIDManager.isInitialized()) {
            EUIDManager.init(context)
        }

        // After we've asked to initialize the manager, we should wait until it's complete before reporting success.
        // This will potentially allow any previously persisted identity to be fully restored before we allow any
        // signals to be collected.
        EUIDManager.getInstance().addOnInitializedListener(initializationCompleteCallback::onInitializationSucceeded)
    }

    /**
     * Collects the UID2 advertising token, if available.
     */
    override fun collectSignals(rtbSignalData: RtbSignalData, signalCallbacks: SignalCallbacks) {
        EUIDManager.getInstance().let { manager ->
            val token = manager.getAdvertisingToken()
            if (token != null) {
                signalCallbacks.onSuccess(token)
            } else {
                // We include the IdentityStatus in the "error" to have better visibility on why the Advertising Token
                // was not present. There are a number of valid reasons why we don't have a token, but we are still
                // required to report these as "failures".
                signalCallbacks.onFailure(
                    AdError(
                        manager.currentIdentityStatus.value,
                        "No Advertising Token",
                        "UID2",
                    ),
                )
            }
        }
    }
}
