package com.uid2.storage

import com.uid2.data.IdentityStatus
import com.uid2.data.UID2Identity

/**
 * An interface controlling access to local storage, used for the persistence of UID2Identity instances.
 */
internal interface StorageManager {
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
}
