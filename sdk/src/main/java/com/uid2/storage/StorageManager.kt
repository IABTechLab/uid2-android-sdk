package com.uid2.storage

import android.content.Context
import com.uid2.data.IdentityStatus
import com.uid2.data.UID2Identity

/**
 * An interface controlling access to local storage, used for the persistence of UID2Identity instances.
 */
interface StorageManager {
    /**
     * Saves the given UID2Identity and status locally, allowing to be loaded later.
     */
    suspend fun saveIdentity(identity: UID2Identity, status: IdentityStatus): Boolean

    /**
     * Loads any previously persisted UID2Identity and status locally.
     */
    suspend fun loadIdentity(): Pair<UID2Identity?, IdentityStatus>

    /**
     * Clears any previously stored data.
     */
    suspend fun clear(): Boolean

    companion object {
        private var instance: StorageManager? = null

        /**
         * Gets the single instance of the StorageManager.
         */
        fun getInstance(context: Context) = instance ?: SharedPreferencesStorageManager(context).apply {
            instance = this
        }
    }
}
