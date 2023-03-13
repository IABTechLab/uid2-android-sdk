package com.uid2.sdk.storage

import android.content.Context
import android.content.SharedPreferences
import com.uid2.sdk.data.UID2Identity
import org.json.JSONObject

/**
 * An implementation of the StorageManager that persists UID2Identity instances in clear-text via Shared Preferences.
 */
class SharedPreferencesStorageManager(private val context: Context) : StorageManager {

    private val preferences: SharedPreferences by lazy {
        context.getSharedPreferences(IDENTITY_KEY_FILE, Context.MODE_PRIVATE)
    }

    override fun saveIdentity(identity: UID2Identity) =
        preferences.edit()
            .putString(IDENTITY_PREF_KEY, identity.toJson().toString(0))
            .commit()

    override fun loadIdentity() = preferences.getString(IDENTITY_PREF_KEY, "")?.let {
        runCatching { UID2Identity.fromJson(JSONObject(it)) }.getOrNull()
    }

    override fun clear() =
        preferences.edit()
            .clear()
            .commit()

    private companion object {
        private const val IDENTITY_KEY_FILE = "uid2_identity"
        private const val IDENTITY_PREF_KEY = "identity"
    }
}
