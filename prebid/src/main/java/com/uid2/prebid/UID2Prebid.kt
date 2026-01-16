package com.uid2.prebid

import com.uid2.UID2Manager
import com.uid2.UID2ManagerState
import com.uid2.UID2ManagerState.Established
import com.uid2.UID2ManagerState.Refreshed
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import org.prebid.mobile.ExternalUserId
import org.prebid.mobile.PrebidMobile
import org.prebid.mobile.TargetingParams

/**
 * Interface to wrap access to [PrebidMobile]. This is used to improve testability, rather than having the [UID2Prebid]
 * access it via static methods.
 */
internal fun interface PrebidExternalUserIdInteractor {
    operator fun invoke(ids: List<ExternalUserId>)
}

/**
 * Prebid integration that will observe a given [UID2Manager] instance and update Prebid when a new [ExternalUserId] is
 * available. After creating the instance, a consumer must explicitly call [initialize] for this instance to start
 * observing changes.
 */
public class UID2Prebid internal constructor(
    private val manager: UID2Manager,
    private val externalUserIdFactory: () -> List<ExternalUserId>,
    private val prebidInteractor: PrebidExternalUserIdInteractor,
    dispatcher: CoroutineDispatcher,
) {

    // We redirect to the logger owned by the UID2Manager, as it's been configured correctly.
    private val logger = manager.logger

    /**
     * Constructor.
     *
     * @param manager The [UID2Manager] instance to be observed.
     * @param externalUserIdFactory A factory that will allow the consumer to add any other [ExternalUserId]s that should
     *                              also be included, rather than just a single list containing only UID2's instance.
     */
    @JvmOverloads
    public constructor(
        manager: UID2Manager = UID2Manager.getInstance(),
        externalUserIdFactory: () -> List<ExternalUserId> = { emptyList() },
    ) : this(
        manager,
        externalUserIdFactory,
        PrebidExternalUserIdInteractor { ids -> TargetingParams.setExternalUserIds(ids) },
        Dispatchers.Default,
    )

    private val scope = CoroutineScope(dispatcher + SupervisorJob())

    /**
     * Initializes the integration which will start observing the associated [UID2Manager] instance for changes in the
     * availability of the advertising token. As the token is refreshed, this will automatically update Prebid's list
     * of ExternalUserIds.
     */
    public fun initialize() {
        // Once the UID2Manager instance has been initialized, we will start observing it for changes.
        manager.addOnInitializedListener {
            updateExternalUserId(manager.getAdvertisingToken(), "Initialized")
            observeIdentityChanges()
        }
    }

    /**
     * Releases this instance, to not be used again.
     */
    public fun release() {
        scope.cancel()
    }

    /**
     * Returns the list of UID2 scoped [ExternalUserId]s.
     */
    public fun getExternalUserIdList(): List<ExternalUserId> {
        return getExternalUserIdList(manager.getAdvertisingToken())
    }

    /**
     * Observes changes in the [UID2ManagerState] of the [UID2Manager] to update Prebid's [ExternalUserId]s.
     */
    private fun observeIdentityChanges() {
        scope.launch {
            manager.state.collect { state ->
                when (state) {
                    is Established -> updateExternalUserId(state.identity.advertisingToken, "Identity Established")
                    is Refreshed -> updateExternalUserId(state.identity.advertisingToken, "Identity Refreshed")
                    else -> updateExternalUserId(null, "Identity Changed: $state")
                }
            }
        }
    }

    /**
     * Updates Prebid's [ExternalUserId]s.
     */
    private fun updateExternalUserId(advertisingToken: String?, reason: String) {
        // We should set the external user ids to contain both our own UID2 specific one, along with any provided
        // externally.
        logger.i(TAG) { "Updating Prebid: $reason" }
        val userIds = getExternalUserIdList(advertisingToken)
        prebidInteractor(externalUserIdFactory() + userIds)
    }

    /**
     * Converts the given token to the associated list of [ExternalUserId]s.
     */
    private fun getExternalUserIdList(advertisingToken: String?): List<ExternalUserId> {
        return advertisingToken?.toExternalUserIdList() ?: emptyList()
    }

    /**
     * Extension function to build a list containing the single [ExternalUserId] that is associated with UID2.
     */
    private fun String.toExternalUserIdList(): List<ExternalUserId> {
        val source = if (manager.isEuid) {
            USER_ID_SOURCE_EUID
        } else {
            USER_ID_SOURCE_UID2
        }
        return listOf(
            ExternalUserId(source, listOf(ExternalUserId.UniqueId(this, AGENT_TYPE_PERSON_ID))),
        )
    }

    private companion object {
        const val TAG = "UID2Prebid"
        const val USER_ID_SOURCE_UID2 = "uidapi.com"
        const val USER_ID_SOURCE_EUID = "euid.eu"

        /**
         * "A person-based ID, i.e., that is the same across devices."
         * https://github.com/InteractiveAdvertisingBureau/AdCOM/blob/main/AdCOM%20v1.0%20FINAL.md#list_agenttypes
         */
        const val AGENT_TYPE_PERSON_ID = 3
    }
}
