package com.uid2.storage

import android.content.Context
import android.content.SharedPreferences
import com.uid2.data.UID2Identity
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

/**
 * An implementation of the StorageManager that persists UID2Identity instances in clear-text via Shared Preferences.
 */
class SharedPreferencesStorageManager(
    private val context: Context,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : StorageManager {

    private val preferences: SharedPreferences by lazy {
        context.getSharedPreferences(IDENTITY_KEY_FILE, Context.MODE_PRIVATE)
    }

    override suspend fun saveIdentity(identity: UID2Identity) = withContext(ioDispatcher) {
        preferences.edit()
            .putString(IDENTITY_PREF_KEY, identity.toJson().toString(0))
            .commit()
    }

    override suspend fun loadIdentity() = withContext(ioDispatcher) {
        preferences.getString(IDENTITY_PREF_KEY, "")?.let {
            runCatching { UID2Identity.fromJson(JSONObject(it)) }.getOrNull()
        }
    }

    override suspend fun clear() = withContext(ioDispatcher) {
        preferences.edit()
            .clear()
            .commit()
    }

    private companion object {
        private const val IDENTITY_KEY_FILE = "uid2_identity"
        private const val IDENTITY_PREF_KEY = "identity"
    }
}
