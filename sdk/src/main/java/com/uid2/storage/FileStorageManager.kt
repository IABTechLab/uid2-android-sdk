package com.uid2.storage

import android.content.Context
import com.uid2.data.IdentityStatus
import com.uid2.data.IdentityStatus.NO_IDENTITY
import com.uid2.data.UID2Identity
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File

/**
 * An implementation of the StorageManager that persists UID2Identity instances in clear-text via a File.
 */
internal class FileStorageManager(
    val identityFileFactory: () -> File,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) : StorageManager {

    // For storage, we use the parent filesDir which is part of the Application's internal storage. This internal
    // storage is sandboxed to prevent any other app, or even the user, from accessing it directly. We rely on Android
    // keeping this file secure.
    //
    // On Android 10+, this location is also likely encrypted.
    //
    // https://developer.android.com/training/data-storage/app-specific#internal-access-files
    constructor(context: Context) : this({ File(context.filesDir, FILE_IDENTITY) })

    // This lazy value *should* only be requested on the ioDispatcher.
    private val identityFile: File by lazy { identityFileFactory() }

    override suspend fun saveIdentity(identity: UID2Identity, status: IdentityStatus) = withContext(ioDispatcher) {
        runCatching {
            identityFile.bufferedWriter(charset).use { writer ->

                // After converting the UID2Identity to JSON, we will extend it to also include the IdentityStatus.
                val identityJson = identity.toJson()
                identityJson.put(KEY_STATUS, status.value)

                writer.write(identityJson.toString(0))
                return@use true
            }
        }.getOrDefault(false)
    }

    override suspend fun loadIdentity(): Pair<UID2Identity?, IdentityStatus> = withContext(ioDispatcher) {
        runCatching {
            val identityJson = JSONObject(identityFile.readText(charset))
            return@runCatching Pair(
                UID2Identity.fromJson(identityJson),
                IdentityStatus.fromValue(identityJson.getInt(KEY_STATUS)),
            )
        }.getOrDefault(Pair(null, NO_IDENTITY))
    }

    override suspend fun clear() = withContext(ioDispatcher) {
        identityFile.delete()
    }

    private companion object {
        const val FILE_IDENTITY = "uid2_identity.json"
        const val KEY_STATUS = "identity_status"

        // The character set used for both reading and writing to the file.
        val charset = Charsets.UTF_8
    }
}
