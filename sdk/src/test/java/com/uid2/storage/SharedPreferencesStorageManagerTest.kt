package com.uid2.storage

import android.content.Context
import android.content.SharedPreferences
import android.content.SharedPreferences.Editor
import com.uid2.data.UID2Identity
import org.junit.Assert
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.junit.MockitoJUnitRunner
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@RunWith(MockitoJUnitRunner::class)
class SharedPreferencesStorageManagerTest {
    private val context: Context = mock()
    private val preferences: SharedPreferences = mock()
    private val editor: Editor = mock()

    private var editorCache = mutableMapOf<String, Any>()

    @Before
    fun before() {
        whenever(context.getSharedPreferences(any(), any())).thenReturn(preferences)
        whenever(preferences.edit()).thenReturn(editor)

        // Configure the Editor so that any values persisted are actually stored in a local cache.
        // This can then be used later when reading back these same values.
        whenever(editor.putString(any(), any())).thenAnswer {
            editorCache[it.arguments[0].toString()] = it.arguments[1]
            return@thenAnswer editor
        }

        // Configure the getX methods to return the cached value.
        whenever(preferences.getString(any(), anyOrNull())).thenAnswer {
            return@thenAnswer editorCache[it.arguments[0].toString()]
        }

        // Configure the Editor so that any requests to clear will actually clear the backed editor cache.
        whenever(editor.clear()).thenAnswer {
            editorCache.clear()
            return@thenAnswer editor
        }
    }

    @Test
    fun `test storage stores and loads identity`() {
        val identity = UID2Identity(
            "ad token",
            "refresh token",
            12345L,
            3333L,
            4321L,
            "refresh response key"
        )

        val storageManager = SharedPreferencesStorageManager(context)
        storageManager.saveIdentity(identity)

        val loaded = storageManager.loadIdentity()
        assertEquals(identity, loaded)
    }

    @Test
    fun `test clears identity`() {
        val identity = UID2Identity(
            "ad token",
            "refresh token",
            12345L,
            3333L,
            4321L,
            "refresh response key"
        )

        val storageManager = SharedPreferencesStorageManager(context)
        storageManager.saveIdentity(identity)

        // After saving the identity to the StorageManager, clear them.
        storageManager.clear()

        // Verify that we're no longer able to load the old identity, and that the Shared Preferences have been cleared.
        val loaded = storageManager.loadIdentity()
        Assert.assertNull(loaded)
        verify(editor).clear()
    }
}
