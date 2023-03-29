package com.uid2.storage

import android.content.Context
import android.content.SharedPreferences
import android.content.SharedPreferences.Editor
import com.uid2.data.IdentityStatus.ESTABLISHED
import com.uid2.data.IdentityStatus.NO_IDENTITY
import com.uid2.data.UID2Identity
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.junit.MockitoJUnitRunner
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.mockito.stubbing.Answer

@ExperimentalCoroutinesApi
@RunWith(MockitoJUnitRunner::class)
class SharedPreferencesStorageManagerTest {
    private lateinit var testDispatcher: TestDispatcher

    private val context: Context = mock()
    private val preferences: SharedPreferences = mock()
    private val editor: Editor = mock()

    private var editorCache = mutableMapOf<String, Any>()

    @Before
    fun before() {
        testDispatcher = StandardTestDispatcher()

        whenever(context.getSharedPreferences(any(), any())).thenReturn(preferences)
        whenever(preferences.edit()).thenReturn(editor)

        // Configure the Editor so that any values persisted are actually stored in a local cache.
        // This can then be used later when reading back these same values.
        val editorPutAnswer = Answer {
            editorCache[it.arguments[0].toString()] = it.arguments[1]
            return@Answer editor
        }
        whenever(editor.putString(any(), any())).thenAnswer(editorPutAnswer)
        whenever(editor.putInt(any(), any())).thenAnswer(editorPutAnswer)

        // Configure the getX methods to return the cached value.
        val editorGetAnswer = Answer {
            val key = it.arguments[0].toString()
            if (!editorCache.containsKey(key)) {
                return@Answer it.arguments[1]
            }

            return@Answer editorCache[key]
        }
        whenever(preferences.getString(any(), anyOrNull())).thenAnswer(editorGetAnswer)
        whenever(preferences.getInt(any(), any())).thenAnswer(editorGetAnswer)

        // Configure the Editor so that any requests to clear will actually clear the backed editor cache.
        whenever(editor.clear()).thenAnswer {
            editorCache.clear()
            return@thenAnswer editor
        }
    }

    @Test
    fun `test storage stores and loads identity`() = runTest(testDispatcher) {
        val identity = UID2Identity(
            "ad token",
            "refresh token",
            12345L,
            3333L,
            4321L,
            "refresh response key"
        )

        val storageManager = SharedPreferencesStorageManager(context)
        storageManager.saveIdentity(identity, ESTABLISHED)

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
            "refresh response key"
        )

        val storageManager = SharedPreferencesStorageManager(context)
        storageManager.saveIdentity(identity, ESTABLISHED)

        // After saving the identity to the StorageManager, clear them.
        storageManager.clear()

        // Verify that we're no longer able to load the old identity, and that the Shared Preferences have been cleared.
        val loaded = storageManager.loadIdentity()
        assertNull(loaded.first)
        assertEquals(NO_IDENTITY, loaded.second)
        verify(editor).clear()
    }
}
