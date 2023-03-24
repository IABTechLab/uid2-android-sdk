package com.uid2

import com.uid2.data.IdentityStatus
import com.uid2.data.IdentityStatus.ESTABLISHED
import com.uid2.data.IdentityStatus.NO_IDENTITY
import com.uid2.data.IdentityStatus.OPT_OUT
import com.uid2.data.IdentityStatus.REFRESHED
import com.uid2.data.UID2Identity
import com.uid2.network.RefreshPackage
import com.uid2.storage.StorageManager
import com.uid2.utils.TimeUtils
import java.io.IOException
import java.util.concurrent.TimeUnit
import kotlin.random.Random
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentMatchers.anyLong
import org.mockito.ArgumentMatchers.anyString
import org.mockito.junit.MockitoJUnitRunner
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever

@ExperimentalCoroutinesApi
@RunWith(MockitoJUnitRunner::class)
class UID2ManagerTest {
    private lateinit var testDispatcher: TestDispatcher

    private val charPool : List<Char> = ('a'..'z') + ('A'..'Z') + ('0'..'9')

    private val client: UID2Client = mock()
    private val storageManager: StorageManager = mock()
    private val timeUtils: TimeUtils = mock()

    private lateinit var manager: UID2Manager
    private val initialIdentity = withRandomIdentity()
    private val listener: UID2ManagerIdentityChangedListener = mock()

    @Before
    fun before() = runBlocking {
        testDispatcher = StandardTestDispatcher()

        // By default, we won't expire tokens.
        whenever(timeUtils.hasExpired(anyLong())).thenReturn(false)

        whenever(storageManager.loadIdentity()).thenReturn(initialIdentity)
        manager = UID2Manager(client, storageManager, timeUtils, testDispatcher, false).apply {
            onIdentityChangedListener = listener
        }
    }

    @Test
    fun `restores identity from storage`() {
        // Verify that the initial state of the manager reflects the restored Identity.
        assertNotNull(manager.currentIdentity)
        assertEquals(initialIdentity, manager.currentIdentity)
        assertEquals(ESTABLISHED, manager.currentIdentityStatus)
    }

    @Test
    fun `updates identity when set`() = runTest(testDispatcher) {
        val identity = withRandomIdentity()

        // By default, the Manager will have restored the previously persisted Identity. Let's reset our state so this
        // will be a new Identity.
        manager.resetIdentity()

        // Verify that setting the Identity on the Manager, results in it being persisted via the StorageManager.
        manager.setIdentity(identity)
        verify(storageManager).saveIdentity(identity)

        // Verify that the Manager updated with the new identity and reported the state changes appropriately.
        assertManagerState(manager, identity, ESTABLISHED)
    }

    @Test
    fun `resets identity`() = runTest(testDispatcher) {
        // Verify that the initial state of the manager reflects the restored Identity.
        assertNotNull(manager.currentIdentity)
        assertEquals(initialIdentity, manager.currentIdentity)
        assertEquals(ESTABLISHED, manager.currentIdentityStatus)

        // Reset the Manager's identity
        manager.resetIdentity()

        // Verify that the Manager updated with the reset identity and reported the state changes appropriately.
        assertManagerState(manager, null, NO_IDENTITY)
    }

    @Test
    fun `refresh no-op when no identity`() {
        val manager = UID2Manager(client, mock(), timeUtils, testDispatcher, false)
        assertNull(manager.currentIdentity)

        // Verify that if we attempt to refresh the Identity when one is not set, nothing happens.
        manager.refreshIdentity()
        verifyNoInteractions(client)
    }

    @Test
    fun `refresh notifies when identity updates`() = runTest(testDispatcher) {
        // Configure the client so that when asked to refresh, it returns a new Identity.
        val newIdentity = withRandomIdentity()
        whenever(client.refreshIdentity(initialIdentity.refreshToken, initialIdentity.refreshResponseKey)).thenReturn(
            RefreshPackage(
                newIdentity,
                REFRESHED,
                "Refreshed"
            )
        )

        // Ask the manager to refresh, allowing the current TestDispatcher to process any jobs.
        manager.refreshIdentity()
        testDispatcher.scheduler.advanceUntilIdle()

        // Verify that the Client was asked to refresh.
        verify(client).refreshIdentity(initialIdentity.refreshToken, initialIdentity.refreshResponseKey)

        // Verify that the Manager updated with the new identity and reported the state changes appropriately.
        assertManagerState(manager, newIdentity, REFRESHED)
    }

    @Test
    fun `refresh identities opt out`() = runTest(testDispatcher) {
        // Configure the client so that when asked to refresh, it actually reports that the user has now opted out.
        whenever(client.refreshIdentity(initialIdentity.refreshToken, initialIdentity.refreshResponseKey)).thenReturn(
            RefreshPackage(
                null,
                OPT_OUT,
                "User opt-ed out"
            )
        )

        // Ask the manager to refresh, allowing the current TestDispatcher to process any jobs.
        manager.refreshIdentity()
        testDispatcher.scheduler.advanceUntilIdle()

        // Verify that the Client was asked to refresh.
        verify(client).refreshIdentity(initialIdentity.refreshToken, initialIdentity.refreshResponseKey)

        // Verify that the Manager updated with the new identity and reported the state changes appropriately.
        assertManagerState(manager, null, OPT_OUT)
    }

    @Test
    fun `refresh retries`() = runTest(testDispatcher) {
        // Configure the Client to initial report an error (exception), but if asked again, it will return a valid
        // refreshed Identity.
        var hasErrored = false
        val newIdentity = withRandomIdentity()
        whenever(client.refreshIdentity(initialIdentity.refreshToken, initialIdentity.refreshResponseKey)).thenAnswer {
            if (hasErrored) {
                return@thenAnswer RefreshPackage(
                    newIdentity,
                    REFRESHED,
                    "Refreshed"
                )
            } else {
                hasErrored = true
                throw IOException()
            }
        }

        // Ask the manager to refresh, allowing the current TestDispatcher to process any jobs.
        manager.refreshIdentity()
        testDispatcher.scheduler.runCurrent()

        // Verify that nothing changed after the identity failed to refresh.
        verifyNoInteractions(listener)

        // Advance the timer enough so that the Manager should have had an opportunity to retry refreshing the identity.
        testScheduler.advanceTimeBy(TimeUnit.SECONDS.toMillis(6))
        assertManagerState(manager, newIdentity, REFRESHED)
    }

    @Test
    fun `automatically refreshes when enabled`() = runTest(testDispatcher) {
        // Configure the storage to not have access to a previously persisted Identity.
        whenever(storageManager.loadIdentity()).thenReturn(null)

        // Configure a Refresh to be required in +5 seconds when asked. This should result in the Manager scheduling
        // that via the TestDispatcher.
        val newIdentity = withRandomIdentity()
        whenever(timeUtils.diffToNow(anyLong())).thenReturn(TimeUnit.SECONDS.toMillis(5))
        whenever(client.refreshIdentity(anyString(), anyString())).thenAnswer {
            RefreshPackage(
                newIdentity,
                REFRESHED,
                "User refreshed"
            )
        }

        // Build the Manager.
        val manager = UID2Manager(client, storageManager, timeUtils, testDispatcher, true)
        assertNull(manager.currentIdentity)

        // Set the initial identity along with the Listener. We do this afterwards as we don't care about that initial
        // notification.
        manager.setIdentity(initialIdentity)
        manager.onIdentityChangedListener = listener

        // Allow the Test Scheduler to advance by a second, and verify that no refresh has yet occurred.
        testScheduler.advanceTimeBy(TimeUnit.SECONDS.toMillis(1))
        verifyNoInteractions(listener)

        // Now advance the Test Scheduler past the time we expect a refresh to occur.
        testScheduler.advanceTimeBy(TimeUnit.SECONDS.toMillis(5))

        // Verify that a refresh has occurred and the new identity is reflected by the Manager. We also need to disable
        // automatic refreshing *before* to avoid a loop where this new identity will refresh every 5 seconds, resulting
        // in a never-ending test!
        manager.automaticRefreshEnabled = false
        assertManagerState(manager, newIdentity, REFRESHED)
    }

    /**
     * Helper function to assert that the Manager is in the expected state, and that state was reported via the
     * supported callback / flow.
     */
    private fun TestScope.assertManagerState(manager: UID2Manager, identity: UID2Identity?, status: IdentityStatus) {
        // Verify the advertising token reported comes from the associated Identity.
        assertEquals(identity?.advertisingToken, manager.getAdvertisingToken())

        // Verify that the expected Identity and Status is being reported by the Manager
        assertEquals(identity, manager.currentIdentity)
        assertEquals(status, manager.currentIdentityStatus)

        // Verify that this same identity was reported via the Listener.
        verify(listener).onIdentityStatusChanged(identity, status)

        // We expect the last state to be replayed, so we can collect the flow and store the states.
        val states = mutableListOf<UID2ManagerState>()
        val job = launch(testDispatcher) { manager.state.collect { states.add(it) } }
        testDispatcher.scheduler.advanceUntilIdle()

        // Verify that we received the expected event.
        val expectedStates = listOf(UID2Manager.getManagerState(identity, status))
        assertEquals(expectedStates, states)

        job.cancel()
    }

    /**
     * Helper function to create a new (Random) identity.
     */
    private fun withRandomIdentity() = UID2Identity(
        randomString(12),
        randomString(12),
        randomLong(),
        randomLong(),
        randomLong(),
        randomString(12)
    )

    private fun randomString(length: Int) = List(length) { charPool.random() }.joinToString("")
    private fun randomLong() = Random.nextLong()
}
