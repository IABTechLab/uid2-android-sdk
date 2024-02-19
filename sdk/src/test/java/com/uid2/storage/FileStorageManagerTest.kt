package com.uid2.storage

import com.uid2.data.IdentityStatus.ESTABLISHED
import com.uid2.data.IdentityStatus.NO_IDENTITY
import com.uid2.data.UID2Identity
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.junit.MockitoJUnitRunner
import java.io.File

@ExperimentalCoroutinesApi
@RunWith(MockitoJUnitRunner::class)
class FileStorageManagerTest {
    private lateinit var testDispatcher: TestDispatcher

    private val identityFile: File = File("test_identity.json")

    @Before
    fun before() {
        testDispatcher = StandardTestDispatcher()
    }

    @After
    fun after() {
        identityFile.delete()
    }

    @Test
    fun `test storage stores and loads identity`() = runTest(testDispatcher) {
        val identity = UID2Identity(
            "ad token",
            "refresh token",
            12345L,
            3333L,
            4321L,
            "refresh response key",
        )

        val storageManager = FileStorageManager({ identityFile }, testDispatcher)
        storageManager.saveIdentity(identity, ESTABLISHED)
        testDispatcher.scheduler.advanceUntilIdle()

        val loaded = storageManager.loadIdentity()
        assertEquals(identity, loaded.first)
        assertEquals(ESTABLISHED, loaded.second)
    }

    @Test
    fun `test clears identity`() = runTest(testDispatcher) {
        val identity = UID2Identity(
            "ad token",
            "refresh token",
            12345L,
            3333L,
            4321L,
            "refresh response key",
        )

        val storageManager = FileStorageManager({ identityFile }, testDispatcher)
        storageManager.saveIdentity(identity, ESTABLISHED)
        testDispatcher.scheduler.advanceUntilIdle()

        // After saving the identity to the StorageManager, clear them.
        storageManager.clear()
        testDispatcher.scheduler.advanceUntilIdle()

        // Verify that we're no longer able to load the old identity, and that the Shared Preferences have been cleared.
        val loaded = storageManager.loadIdentity()
        assertNull(loaded.first)
        assertEquals(NO_IDENTITY, loaded.second)
        assertFalse(identityFile.exists())
    }
}
