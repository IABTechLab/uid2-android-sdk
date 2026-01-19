package com.uid2.prebid

import com.uid2.UID2Manager
import com.uid2.UID2ManagerState
import com.uid2.data.UID2Identity
import com.uid2.utils.Logger
import io.mockk.every
import io.mockk.junit4.MockKRule
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.prebid.mobile.ExternalUserId

@OptIn(ExperimentalCoroutinesApi::class)
class UID2PrebidTest {
    @get:Rule
    val mockkRule = MockKRule(this)

    private val testDispatcher: TestDispatcher = StandardTestDispatcher()

    private val manager = mockk<UID2Manager>()
    private var currentAdvertisingToken = "secret token"
    private val state = MutableStateFlow<UID2ManagerState>(UID2ManagerState.Loading)
    private val logger = mockk<Logger>(relaxed = true)

    private val prebidExternalUserIdInteractor = FakePrebidExternalUserIdInteractor()

    @Before
    fun before() {
        every { manager.addOnInitializedListener(any()) }.answers {
            // Invoke the listener immediately.
            val listener = firstArg() as (() -> Unit)
            listener()

            return@answers manager
        }

        every { manager.getAdvertisingToken() }.returns(currentAdvertisingToken)
        every { manager.state }.returns(state)
        every { manager.logger }.returns(logger)
        every { manager.isEuid }.returns(false)
    }

    @Test
    fun `initialises user id on creation`() {
        val prebid = withPrebid().apply {
            initialize()
        }

        // Verify that immediately after being initialized, the available advertising token is set on Prebid.
        assertEquals(1, prebidExternalUserIdInteractor.lastIds.size)
        assertEquals(currentAdvertisingToken, prebidExternalUserIdInteractor.lastIds[0].uniqueIds[0].id)
    }

    @Test
    fun `observes changes to advertising token`() = runTest(testDispatcher) {
        val prebid = withPrebid().apply {
            initialize()
        }

        // Start with an established token.
        val newToken1 = "established-token"
        state.emit(withEstablished(newToken1))
        advanceUntilIdle()

        // Verify it was set on Prebid.
        prebidExternalUserIdInteractor.assertLastToken(newToken1, "uidapi.com")

        // Refresh the token.
        val newToken2 = "refreshed-token-1"
        state.emit(withRefreshed(newToken2))
        advanceUntilIdle()

        // Verify it was set on Prebid.
        prebidExternalUserIdInteractor.assertLastToken(newToken2, "uidapi.com")

        // Refresh the token again.
        val newToken3 = "refreshed-token-2"
        state.emit(withRefreshed(newToken3))
        advanceUntilIdle()

        // Verify it was set on Prebid.
        prebidExternalUserIdInteractor.assertLastToken(newToken3, "uidapi.com")
    }

    @Test
    fun `sets the source for EUID`() = runTest(testDispatcher) {
        every { manager.isEuid }.returns(true)

        val prebid = withPrebid().apply {
            initialize()
        }

        // Start with an established token.
        val newToken1 = "established-token"
        state.emit(withEstablished(newToken1))
        advanceUntilIdle()

        // Verify it was set on Prebid.
        prebidExternalUserIdInteractor.assertLastToken(newToken1, "euid.eu")

        // Refresh the token.
        val newToken2 = "refreshed-token-1"
        state.emit(withRefreshed(newToken2))
        advanceUntilIdle()

        // Verify it was set on Prebid.
        prebidExternalUserIdInteractor.assertLastToken(newToken2, "euid.eu")

        // Refresh the token again.
        val newToken3 = "refreshed-token-2"
        state.emit(withRefreshed(newToken3))
        advanceUntilIdle()

        // Verify it was set on Prebid.
        prebidExternalUserIdInteractor.assertLastToken(newToken3, "euid.eu")
    }

    @Test
    fun `removes id when token invalid`() = runTest(testDispatcher) {
        val prebid = withPrebid().apply {
            initialize()
        }

        listOf(
            UID2ManagerState.Expired(withIdentity("")),
            UID2ManagerState.Invalid,
            UID2ManagerState.RefreshExpired,
            UID2ManagerState.OptOut,
        ).forEach { managerState ->

            // Start from an established state.
            val token = "established"
            state.emit(withEstablished(token))
            advanceUntilIdle()

            // Verify that it's been set on Prebid.
            prebidExternalUserIdInteractor.assertLastToken(token, "uidapi.com")

            // Emit the new state.
            state.emit(managerState)
            advanceUntilIdle()

            // Verify that previous IDs have been removed.
            prebidExternalUserIdInteractor.assertNoIds()
        }
    }

    private fun withPrebid(): UID2Prebid {
        return UID2Prebid(
            manager = manager,
            externalUserIdFactory = { emptyList() },
            prebidInteractor = prebidExternalUserIdInteractor,
            dispatcher = testDispatcher,
        )
    }

    private fun withEstablished(advertisingToken: String) = UID2ManagerState.Established(
        withIdentity(advertisingToken),
    )

    private fun withRefreshed(advertisingToken: String) = UID2ManagerState.Refreshed(
        withIdentity(advertisingToken),
    )

    private fun withIdentity(advertisingToken: String) = UID2Identity(
        advertisingToken = advertisingToken,
        refreshToken = "",
        identityExpires = 0,
        refreshFrom = 0,
        refreshExpires = 0,
        refreshResponseKey = "",
    )

    private fun FakePrebidExternalUserIdInteractor.assertLastToken(advertisingToken: String, source: String) {
        assertTrue(lastIds.isNotEmpty())
        lastIds.last().let {
            assertEquals(source, it.source)
            assertEquals(1, it.uniqueIds.size)
            val id = it.uniqueIds[0]
            assertEquals(advertisingToken, id.id)
            assertEquals(3, id.atype)
            assertNull(it.ext)
        }
    }

    private fun FakePrebidExternalUserIdInteractor.assertNoIds() {
        assertTrue(lastIds.isEmpty())
    }
}

private class FakePrebidExternalUserIdInteractor : PrebidExternalUserIdInteractor {
    var lastIds: List<ExternalUserId> = emptyList()

    override fun invoke(ids: List<ExternalUserId>) {
        lastIds = ids
    }
}
