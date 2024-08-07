package com.uid2

import com.uid2.UID2Manager.GenerateIdentityResult
import com.uid2.data.IdentityRequest
import com.uid2.data.IdentityStatus
import com.uid2.data.IdentityStatus.ESTABLISHED
import com.uid2.data.IdentityStatus.EXPIRED
import com.uid2.data.IdentityStatus.NO_IDENTITY
import com.uid2.data.IdentityStatus.OPT_OUT
import com.uid2.data.IdentityStatus.REFRESHED
import com.uid2.data.IdentityStatus.REFRESH_EXPIRED
import com.uid2.data.UID2Identity
import com.uid2.network.ResponsePackage
import com.uid2.storage.StorageManager
import com.uid2.utils.InputUtils
import com.uid2.utils.Logger
import com.uid2.utils.TimeUtils
import io.mockk.Called
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.junit4.MockKRule
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.io.IOException
import java.util.concurrent.TimeUnit
import kotlin.random.Random

@ExperimentalCoroutinesApi
class UID2ManagerTest {
    @get:Rule
    val mockkRule = MockKRule(this)

    private val testDispatcher: TestDispatcher = StandardTestDispatcher()

    private val expirationInterval = 10 * 1000L // 10 seconds
    private val charPool: List<Char> = ('a'..'z') + ('A'..'Z') + ('0'..'9')

    private val client = mockk<UID2Client>()
    private val storageManager = mockk<StorageManager>()
    private val timeUtils = mockk<TimeUtils>()
    private val inputUtils = mockk<InputUtils>()
    private val logger = mockk<Logger>(relaxed = true)

    private lateinit var manager: UID2Manager
    private val initialIdentity = withRandomIdentity()
    private val initialStatus = ESTABLISHED
    private val listener = mockk<UID2ManagerIdentityChangedListener>(relaxed = true)

    @Before
    fun before() {
        // By default, we won't expire tokens.
        every { timeUtils.hasExpired(any()) }.returns(false)

        every { inputUtils.normalize(any<IdentityRequest.Email>()) }.answers { firstArg() as IdentityRequest.Email }
        every { inputUtils.normalize(any<IdentityRequest.Phone>()) }.answers { firstArg() as IdentityRequest.Phone }

        coEvery { storageManager.clear() }.returns(true)
        coEvery { storageManager.saveIdentity(any(), any()) }.returns(true)
        coEvery { storageManager.loadIdentity() }.returns(Pair(initialIdentity, initialStatus))

        manager = withManager(client, storageManager, timeUtils, inputUtils, testDispatcher, false, listener)
    }

    @Test
    fun `reports when initialization is complete`() = runTest(testDispatcher) {
        val listenerCount = 5
        val listenerResults = ArrayList<Boolean>().apply {
            for (i in 0..listenerCount) {
                add(false)
            }
        }

        val manager = UID2Manager(client, storageManager, timeUtils, inputUtils, testDispatcher, false, logger).apply {
            this.checkExpiration = false

            // Add the required listeners.
            for (i in 0..listenerCount) {
                addOnInitializedListener { listenerResults[i] = true }
            }
        }

        // Verify that the manager invokes our callback after it's been able to load the identity from storage.
        assertTrue(listenerResults.all { !it })
        testDispatcher.scheduler.advanceUntilIdle()
        assertTrue(listenerResults.all { it })

        // Create a new listener that we add after initialization is complete. Verify that it's called immediately.
        var isInitialized = false
        manager.addOnInitializedListener { isInitialized = true }
        assertTrue(isInitialized)
    }

    @Test
    fun `restores identity from storage`() = runTest(testDispatcher) {
        // Verify that the initial state of the manager reflects the restored Identity.
        assertNotNull(manager.currentIdentity)
        assertEquals(initialIdentity, manager.currentIdentity)
        assertEquals(initialStatus, manager.currentIdentityStatus)
    }

    @Test
    fun `set identity immediately available`() = runTest(testDispatcher) {
        val identity = withRandomIdentity()

        // By default, the Manager will have restored the previously persisted Identity. Let's reset our state so this
        // will be a new Identity.
        manager.resetIdentity()

        // Verify that immediately after setting an identity, it's immediately available via currentIdentity.
        manager.setIdentity(identity)
        assertEquals(identity, manager.currentIdentity)
    }

    @Test
    fun `updates identity when set`() = runTest(testDispatcher) {
        val identity = withRandomIdentity()

        // By default, the Manager will have restored the previously persisted Identity. Let's reset our state so this
        // will be a new Identity.
        manager.resetIdentity()

        // Verify that setting the Identity on the Manager, results in it being persisted via the StorageManager.
        manager.setIdentity(identity)
        testDispatcher.scheduler.advanceUntilIdle()
        coVerify { storageManager.saveIdentity(identity, ESTABLISHED) }

        // Verify that the Manager updated with the new identity and reported the state changes appropriately.
        assertManagerState(manager, identity, ESTABLISHED)
    }

    @Test
    fun `generates identity for different requests`() = runTest(testDispatcher) {
        val subscriptionId = "sub"
        val publicKey = "pub"

        listOf(
            IdentityRequest.Email("test@test.com"),
            IdentityRequest.EmailHash("a-hash"),
            IdentityRequest.Phone("+00000000000"),
            IdentityRequest.PhoneHash("another-hash"),
        ).forEach { request ->
            val generated = withRandomIdentity()
            coEvery { client.generateIdentity(request, subscriptionId, publicKey) }.returns(
                ResponsePackage(generated, ESTABLISHED, ""),
            )

            // Request a new identity should be generated.
            var result: GenerateIdentityResult? = null
            manager.generateIdentity(request, subscriptionId, publicKey) { result = it }
            testDispatcher.scheduler.advanceUntilIdle()

            // Verify that the identity is updated from the one provided via the Client.
            assertEquals(manager.currentIdentity, generated)

            // Verify that the result callback was invoked.
            assertEquals(GenerateIdentityResult.Success, result)
        }
    }

    @Test
    fun `existing identity untouched if generation fails`() = runTest(testDispatcher) {
        val subscriptionId = "sub"
        val publicKey = "pub"

        val request = IdentityRequest.Email("test@test.com")
        coEvery { client.generateIdentity(request, subscriptionId, publicKey) }.throws(PayloadDecryptException())

        // Verify that the manager has a known (existing) identity.
        assertEquals(manager.currentIdentity, initialIdentity)

        // Request a new identity is generated, knowing that this will fail.
        var result: GenerateIdentityResult? = null
        manager.generateIdentity(request, subscriptionId, publicKey) { result = it }
        testDispatcher.scheduler.advanceUntilIdle()

        // Verify that after the failure, the existing identity is still present.
        assertEquals(manager.currentIdentity, initialIdentity)

        // Verify that the result callback was invoked.
        assertTrue(result is GenerateIdentityResult.Error)
    }

    @Test
    fun `generates for opt-out identity`() = runTest(testDispatcher) {
        val subscriptionId = "sub"
        val publicKey = "pub"

        val request = IdentityRequest.Email("test@test.com")
        coEvery { client.generateIdentity(request, subscriptionId, publicKey) }.returns(
            ResponsePackage(null, OPT_OUT, ""),
        )

        // Request a new identity should be generated.
        var result: GenerateIdentityResult? = null
        manager.generateIdentity(request, subscriptionId, publicKey) { result = it }
        testDispatcher.scheduler.advanceUntilIdle()

        // Verify that after generating the identity, there is no current identity but the expected OPT_OUT status.
        assertNull(manager.currentIdentity)
        assertEquals(OPT_OUT, manager.currentIdentityStatus)

        // Verify that the result callback was invoked.
        assertTrue(result is GenerateIdentityResult.Success)
    }

    @Test
    fun `resets identity`() = runTest(testDispatcher) {
        // Verify that the initial state of the manager reflects the restored Identity.
        assertNotNull(manager.currentIdentity)
        assertEquals(initialIdentity, manager.currentIdentity)
        assertEquals(ESTABLISHED, manager.currentIdentityStatus)

        // Reset the Manager's identity
        manager.resetIdentity()
        testDispatcher.scheduler.advanceUntilIdle()

        // Verify that the Manager updated with the reset identity and reported the state changes appropriately.
        assertManagerState(manager, null, NO_IDENTITY)
    }

    @Test
    fun `resets identity immediately after initialisation`() = runTest(testDispatcher) {
        // Create a new instance of the manager but *don't* allow it to finish initialising (loading previous identity)
        val manager = UID2Manager(client, storageManager, timeUtils, inputUtils, testDispatcher, false, logger).apply {
            onIdentityChangedListener = listener
            checkExpiration = false
        }

        // Reset the Manager's identity
        manager.resetIdentity()
        testDispatcher.scheduler.advanceUntilIdle()

        // Verify that the Manager updated with the reset identity and reported the state changes appropriately.
        assertManagerState(manager, null, NO_IDENTITY)
    }

    @Test
    fun `reports when has identity`() = runTest(testDispatcher) {
        // When the default identity is restored, verify that hasIdentity reflects this.
        assertNotNull(manager.currentIdentity)
        assertTrue(manager.hasIdentity())
    }

    @Test
    fun `reports when no identity available`() = runTest(testDispatcher) {
        // Reset the Manager's identity
        manager.resetIdentity()
        testDispatcher.scheduler.advanceUntilIdle()

        // Verify that no identity is reported.
        assertNull(manager.currentIdentity)
        assertFalse(manager.hasIdentity())
    }

    @Test
    fun `reports when no identity available after opt-out`() = runTest(testDispatcher) {
        // Configure the client so that when asked to refresh, it actually reports that the user has now opted out.
        coEvery { client.refreshIdentity(initialIdentity.refreshToken, initialIdentity.refreshResponseKey) }.returns(
            ResponsePackage(
                null,
                OPT_OUT,
                "User opt-ed out",
            ),
        )

        // Ask the manager to refresh, allowing the current TestDispatcher to process any jobs.
        manager.refreshIdentity()
        testDispatcher.scheduler.advanceUntilIdle()

        // Verify that no identity is reported.
        assertNull(manager.currentIdentity)
        assertFalse(manager.hasIdentity())
    }

    @Test
    fun `refresh no-op when no identity`() = runTest(testDispatcher) {
        // Create a mock StorageManager that doesn't have any previously saved Identity.
        val storageManager = mockk<StorageManager>()
        coEvery { storageManager.loadIdentity() }.returns(Pair(null, NO_IDENTITY))

        val manager = withManager(client, storageManager, timeUtils, inputUtils, testDispatcher, false, null)
        assertNull(manager.currentIdentity)

        // Verify that if we attempt to refresh the Identity when one is not set, nothing happens.
        manager.refreshIdentity()
        verify { client wasNot Called }
    }

    @Test
    fun `refresh notifies when identity updates`() = runTest(testDispatcher) {
        // Configure the client so that when asked to refresh, it returns a new Identity.
        val newIdentity = withRandomIdentity()
        coEvery { client.refreshIdentity(initialIdentity.refreshToken, initialIdentity.refreshResponseKey) }.returns(
            ResponsePackage(
                newIdentity,
                REFRESHED,
                "Refreshed",
            ),
        )

        // Ask the manager to refresh, allowing the current TestDispatcher to process any jobs.
        manager.refreshIdentity()
        testDispatcher.scheduler.advanceUntilIdle()

        // Verify that the Client was asked to refresh.
        coVerify { client.refreshIdentity(initialIdentity.refreshToken, initialIdentity.refreshResponseKey) }

        // Verify that the Manager updated with the new identity and reported the state changes appropriately.
        assertManagerState(manager, newIdentity, REFRESHED)
    }

    @Test
    fun `refresh identities opt out`() = runTest(testDispatcher) {
        // Configure the client so that when asked to refresh, it actually reports that the user has now opted out.
        coEvery { client.refreshIdentity(initialIdentity.refreshToken, initialIdentity.refreshResponseKey) }.returns(
            ResponsePackage(
                null,
                OPT_OUT,
                "User opt-ed out",
            ),
        )

        // Ask the manager to refresh, allowing the current TestDispatcher to process any jobs.
        manager.refreshIdentity()
        testDispatcher.scheduler.advanceUntilIdle()

        // Verify that the Client was asked to refresh.
        coVerify { client.refreshIdentity(initialIdentity.refreshToken, initialIdentity.refreshResponseKey) }

        // Verify that the Manager updated with the new identity and reported the state changes appropriately.
        assertManagerState(manager, null, OPT_OUT)
    }

    @Test
    fun `refresh retries`() = runTest(testDispatcher) {
        // Configure the Client to initial report an error (exception), but if asked again, it will return a valid
        // refreshed Identity.
        var hasErrored = false
        val newIdentity = withRandomIdentity()
        coEvery { client.refreshIdentity(initialIdentity.refreshToken, initialIdentity.refreshResponseKey) }.answers {
            if (hasErrored) {
                ResponsePackage(
                    newIdentity,
                    REFRESHED,
                    "Refreshed",
                )
            } else {
                hasErrored = true
                throw IOException()
            }
        }

        // Start with a clean state.
        clearMocks(listener)

        // Ask the manager to refresh, allowing the current TestDispatcher to process any jobs.
        manager.refreshIdentity()
        testDispatcher.scheduler.advanceTimeBy(TimeUnit.SECONDS.toMillis(1))

        // Verify that nothing changed after the identity failed to refresh.
        verify { listener wasNot Called }

        // Advance the timer enough so that the Manager should have had an opportunity to retry refreshing the identity.
        testDispatcher.scheduler.advanceTimeBy(TimeUnit.SECONDS.toMillis(6))
        assertManagerState(manager, newIdentity, REFRESHED)
    }

    @Test
    fun `refresh retries until expired`() = runTest(testDispatcher) {
        // Configure the client to always report an error, e.g. the network isn't accessible.
        var refreshCount = 0
        coEvery { client.refreshIdentity(initialIdentity.refreshToken, initialIdentity.refreshResponseKey) }.answers {
            refreshCount++
            throw IOException()
        }

        // Ask the manager to refresh, allowing the current TestDispatcher to process some jobs.
        manager.refreshIdentity()
        testDispatcher.scheduler.advanceTimeBy(TimeUnit.SECONDS.toMillis(10))

        // Since we should retry every 5 seconds (initially), we expect two attempts to refresh.
        assertEquals(2, refreshCount)

        // Now report that the identity has expired, and therefore not able to refresh any more. Allow the current
        // TestDispatcher to process all/any remaining jobs.
        every { timeUtils.hasExpired(any()) }.returns(true)
        testDispatcher.scheduler.advanceUntilIdle()

        // Verify no more refresh attempts occurred.
        assertEquals(2, refreshCount)
    }

    @Test
    fun `refresh retries slow down`() = runTest(testDispatcher) {
        try {
            // Configure the client to always report an error, e.g. the network isn't accessible.
            var refreshCount = 0
            coEvery {
                client.refreshIdentity(initialIdentity.refreshToken, initialIdentity.refreshResponseKey)
            }.answers {
                refreshCount++
                throw IOException()
            }

            // Ask the manager to refresh, allowing the current TestDispatcher to process some jobs.
            manager.refreshIdentity()
            testDispatcher.scheduler.advanceTimeBy(TimeUnit.SECONDS.toMillis(30))

            // Since we should retry every 5 seconds (initially), we expect two attempts to refresh.
            assertEquals(6, refreshCount)

            testDispatcher.scheduler.advanceTimeBy(TimeUnit.SECONDS.toMillis(30))
            assertEquals(6, refreshCount)

            testDispatcher.scheduler.advanceTimeBy(TimeUnit.SECONDS.toMillis(30))
            assertEquals(7, refreshCount)

            testDispatcher.scheduler.advanceTimeBy(TimeUnit.SECONDS.toMillis(60))
            assertEquals(8, refreshCount)
        } finally {
            // Make sure we expire the identity, so that any automatic refresh stops.
            every { timeUtils.hasExpired(any()) }.returns(true)
        }
    }

    @Test
    fun `automatically refreshes when enabled`() = runTest(testDispatcher) {
        // Configure the storage to not have access to a previously persisted Identity.
        coEvery { storageManager.loadIdentity() }.returns(Pair(null, NO_IDENTITY))

        // Configure a Refresh to be required in +5 seconds when asked. This should result in the Manager scheduling
        // that via the TestDispatcher.
        val newIdentity = withRandomIdentity()
        var refreshed = false
        every { timeUtils.diffToNow(any()) }.returns(TimeUnit.SECONDS.toMillis(5))
        coEvery { client.refreshIdentity(any(), any()) }.answers {
            refreshed = true
            ResponsePackage(
                newIdentity,
                REFRESHED,
                "User refreshed",
            )
        }

        // Build the Manager.
        val manager = withManager(client, storageManager, timeUtils, inputUtils, testDispatcher, true, listener)
        testScheduler.advanceTimeBy(10)
        assertNull(manager.currentIdentity)

        // Set the initial identity. We will allow this to be processed before we clear the state of the listener.
        manager.setIdentity(initialIdentity)
        testScheduler.advanceTimeBy(10)
        clearMocks(listener)

        // Allow the Test Scheduler to advance by a second, and verify that no refresh has yet occurred.
        testScheduler.advanceTimeBy(TimeUnit.SECONDS.toMillis(1))
        verify { listener wasNot Called }
        assertFalse(refreshed)

        // Now advance the Test Scheduler past the time we expect a refresh to occur.
        testScheduler.advanceTimeBy(TimeUnit.SECONDS.toMillis(5))

        // Verify that a refresh has occurred and the new identity is reflected by the Manager. We also need to disable
        // automatic refreshing *before* to avoid a loop where this new identity will refresh every 5 seconds, resulting
        // in a never-ending test!
        manager.automaticRefreshEnabled = false
        assertTrue(refreshed)
        assertManagerState(manager, newIdentity, REFRESHED)
    }

    @Test
    fun `updates after identity expiration`() = runTest(testDispatcher) {
        // Configure the storage to not have access to a previously persisted Identity.
        coEvery { storageManager.loadIdentity() }.returns(Pair(null, NO_IDENTITY))

        // Bind the TimeUtil's implementation to the clock of the TestDispatcher.
        every { timeUtils.diffToNow(any()) }.answers {
            (firstArg() as Long) - testDispatcher.scheduler.currentTime
        }
        every { timeUtils.hasExpired(any()) }.answers {
            (firstArg() as Long) <= testDispatcher.scheduler.currentTime
        }

        // Build the Manager, allowing it to attempt to load from storage.
        val manager = withManager(client, storageManager, timeUtils, inputUtils, testDispatcher, false, listener, true)
        testScheduler.advanceTimeBy(10)
        assertNull(manager.currentIdentity)

        // Set the initial identity, ignoring the first notifications.
        manager.setIdentity(initialIdentity)
        testScheduler.advanceTimeBy(10)
        clearMocks(listener)

        // Advance the clock to just past the time where the identity should expire. Verify that we were notified.
        testScheduler.advanceTimeBy(initialIdentity.identityExpires + 1000)
        verify { listener.onIdentityStatusChanged(initialIdentity, EXPIRED) }

        // Advance the clock to just past the time where the identity could no longer be refreshed. Verify that we were
        // notified.
        testScheduler.advanceTimeBy((initialIdentity.refreshExpires - initialIdentity.identityExpires) + 1000)
        assertManagerState(manager, null, REFRESH_EXPIRED)
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
        verify { listener.onIdentityStatusChanged(identity, status) }

        // We expect the last state to be replayed, so we can collect the flow and store the states.
        val states = mutableListOf<UID2ManagerState>()
        val job = launch(testDispatcher) { manager.state.collect { states.add(it) } }
        testDispatcher.scheduler.advanceUntilIdle()

        // Verify that we received the expected event.
        val expectedStates = listOf(UID2Manager.getManagerState(identity, status))
        assertEquals(expectedStates, states)

        job.cancel()
    }

    private fun withManager(
        client: UID2Client,
        storageManager: StorageManager,
        timeUtils: TimeUtils,
        inputUtils: InputUtils,
        dispatcher: CoroutineDispatcher,
        initialAutomaticRefreshEnabled: Boolean,
        listener: UID2ManagerIdentityChangedListener?,
        initialCheckExpiration: Boolean = false,
    ): UID2Manager {
        return UID2Manager(
            client,
            storageManager,
            timeUtils,
            inputUtils,
            dispatcher,
            initialAutomaticRefreshEnabled,
            logger,
        ).apply {
            onIdentityChangedListener = listener
            checkExpiration = initialCheckExpiration

            if (!initialAutomaticRefreshEnabled && !initialCheckExpiration) {
                testDispatcher.scheduler.advanceUntilIdle()
            }
        }
    }

    /**
     * Helper function to create a new (Random) identity.
     */
    private fun withRandomIdentity(): UID2Identity {
        // We will generate valid expiration times:
        //  - Refresh From (-10s)
        //  - Identity Expires (0)
        //  - Refresh Expires (+10s)
        val identityExpires = randomLong(expirationInterval)
        val refreshFrom = identityExpires - expirationInterval
        val refreshExpires = identityExpires + expirationInterval

        return UID2Identity(
            randomString(12),
            randomString(12),
            identityExpires,
            refreshFrom,
            refreshExpires,
            randomString(12),
        )
    }

    private fun randomString(length: Int) = List(length) { charPool.random() }.joinToString("")
    private fun randomLong(min: Long) = Random.nextLong(min, Long.MAX_VALUE)
}
