package com.uid2.storage

import android.content.Context
import com.uid2.data.UID2Identity

/**
 * An interface controlling access to local storage, used for the persistence of UID2Identity instances.
 */
interface StorageManager {
    /**
     * Saves the given UID2Identity locally, allowing to be loaded later.
     */
    fun saveIdentity(identity: UID2Identity): Boolean

    /**
     * Loads any previously persisted UID2Identity locally. If no save data is found, this will just return null.
     */
    fun loadIdentity(): UID2Identity?

    /**
     * Clears any previously stored data.
     */
    fun clear(): Boolean

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
