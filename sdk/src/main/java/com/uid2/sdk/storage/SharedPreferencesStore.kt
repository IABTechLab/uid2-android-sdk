package com.uid2.sdk.storage

import android.content.Context
import android.content.SharedPreferences
import androidx.annotation.VisibleForTesting
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.util.concurrent.ConcurrentHashMap

/**
 * Caching Shared preferences store.
 *  - creates a in-memory copy of ALL of [SharedPreferences] to speed up later access.
 *  - backed by [ConcurrentHashMap]
 *  - writes are async unless a commit is specified
 *
 * The intention is that the `initializeSharedPrefs()` method is called during the app startup
 * phase to ensure the backing [SharedPreferences] is loaded from disk and the results cached.
 * This can be done via WorkManager.
 */
@Suppress("TooManyFunctions")
abstract class SharedPreferencesStore constructor(
    private val applicationContext: Context,
    dispatcher: CoroutineDispatcher
) : Store {

    private var memoryCache: ConcurrentHashMap<String, Any?> = ConcurrentHashMap<String, Any?>()
    private var sharedPreferences: SharedPreferences? = null

    private val scope = CoroutineScope(dispatcher + SupervisorJob())

    abstract fun getSharedPreferencesName(): String

    /**
     * Initialize the Store on Startup, by reading ALL of the [SharedPreferences]
     * file into a cache.  Call this asynchronously off the main thread.
     */
    override suspend fun initializeSharedPrefs() {
        synchronized(this) {
            // fast exit in case of additional synchronized blocked calls due to
            // multiple reads on startup before initialization is done.
            if (memoryCache.isEmpty()) return

            sharedPreferences = applicationContext.getSharedPreferences(
                getSharedPreferencesName(),
                Context.MODE_PRIVATE
            ).also { prefs ->
                memoryCache = ConcurrentHashMap<String, Any?>().also { map ->
                    map.putAll(prefs.all)
                }
            }
        }
    }

    override fun getBoolean(key: String): Boolean {
        return getBoolean(key, Store.BOOLEAN_NOT_FOUND_DEFAULT)
    }

    override fun getBoolean(key: String, defaultValue: Boolean): Boolean {
        return runBlocking { get(key, defaultValue) }
    }

    override fun putBoolean(key: String, value: Boolean) {
        putInCache(key, value)
        scope.launch { getEditor().putBoolean(key, value).apply() }
    }

    override suspend fun putBooleanCommit(key: String, value: Boolean) {
        putInCache(key, value)
        scope.launch { getEditor().putBoolean(key, value).commit() }
    }

    override fun getFloat(key: String): Float {
        return getFloat(key, Store.FLOAT_NOT_FOUND_DEFAULT)
    }

    override fun getFloat(key: String, defaultValue: Float): Float {
        return runBlocking { get(key, defaultValue) }
    }

    override fun putFloat(key: String, value: Float) {
        putInCache(key, value)
        scope.launch { getEditor().putFloat(key, value).apply() }
    }

    override suspend fun putFloatCommit(key: String, value: Float) {
        putInCache(key, value)
        scope.launch { getEditor().putFloat(key, value).commit() }
    }

    override fun getInt(key: String): Int {
        return getInt(key, Store.INT_NOT_FOUND_DEFAULT)
    }

    override fun getInt(key: String, defaultValue: Int): Int {
        return runBlocking { get(key, defaultValue) }
    }

    override fun putInt(key: String, value: Int) {
        putInCache(key, value)
        scope.launch { getEditor().putInt(key, value).apply() }
    }

    override suspend fun putIntCommit(key: String, value: Int) {
        putInCache(key, value)
        scope.launch { getEditor().putInt(key, value).commit() }
    }

    override fun getLong(key: String): Long {
        return getLong(key, Store.LONG_NOT_FOUND_DEFAULT)
    }

    override fun getLong(key: String, defaultValue: Long): Long {
        return runBlocking { get(key, defaultValue) }
    }

    override fun putLong(key: String, value: Long) {
        putInCache(key, value)
        scope.launch { getEditor().putLong(key, value).apply() }
    }

    override suspend fun putLongCommit(key: String, value: Long) {
        putInCache(key, value)
        scope.launch { getEditor().putLong(key, value).commit() }
    }

    override fun getString(key: String): String? {
        return getString(key, null)
    }

    override fun getString(key: String, defaultValue: String?): String? {
        return runBlocking { get(key, defaultValue) }
    }

    override fun putString(key: String, value: String?) {
        value?.run { putInCache(key, value) } ?: memoryCache.remove(key)
        scope.launch { getEditor().putString(key, value).apply() }
    }

    override suspend fun putStringCommit(key: String, value: String?) {
        value?.run { putInCache(key, value) } ?: memoryCache.remove(key)
        scope.launch { getEditor().putString(key, value).commit() }
    }

    override fun getStringSet(key: String): Set<String>? {
        return getStringSet(key, null)
    }

    override fun getStringSet(key: String, defaultValue: Set<String>?): Set<String>? {
        return runBlocking { get(key, defaultValue) }
    }

    override fun putStringSet(key: String, value: Set<String>?) {
        value?.run { putInCache(key, value) } ?: memoryCache.remove(key)
        scope.launch { getEditor().putStringSet(key, value).apply() }
    }

    override suspend fun putStringSetCommit(key: String, value: Set<String>) {
        putInCache(key, value)
        scope.launch { getEditor().putStringSet(key, value).commit() }
    }

    override fun contains(key: String): Boolean = memoryCache.containsKey(key)

    override fun clear() {
        memoryCache.clear()
        scope.launch { getEditor().clear().commit() }
    }

    override fun remove(key: String) {
        memoryCache.remove(key)
        scope.launch { getEditor().remove(key).commit() }
    }

    private suspend fun getSharedPreferences(): SharedPreferences {
        return sharedPreferences ?: initializeSharedPrefs().run { sharedPreferences!! }
    }

    private suspend fun getEditor(): SharedPreferences.Editor = getSharedPreferences().edit()

    @Suppress("UNCHECKED_CAST")
    private suspend fun <T> get(key: String, defaultValue: T): T {
        val data = getFromCache(key)
        if (data != null) return data as T

        val value: T = when (defaultValue) {
            is String? -> getSharedPreferences().getString(key, defaultValue as? String) as T
            is Int -> getSharedPreferences().getInt(key, defaultValue as Int) as T
            is Boolean -> getSharedPreferences().getBoolean(key, defaultValue as Boolean) as T
            is Float -> getSharedPreferences().getFloat(key, defaultValue as Float) as T
            is Long -> getSharedPreferences().getLong(key, defaultValue as Long) as T
            is Set<*>? -> getSharedPreferences().getStringSet(key, defaultValue as? Set<String>) as T
            else -> throw UnsupportedOperationException(MSG_NOT_YET_IMPLEMENTED)
        }
        value?.run {
            putInCache(key, value)
        }
        return value
    }

    private fun <T> putInCache(key: String, value: T) {
        memoryCache[key] = value
    }

    private fun getFromCache(key: String): Any? = memoryCache[key]

    @VisibleForTesting
    fun setSharedPreferences(sp: SharedPreferences) {
        synchronized(this) {
            sharedPreferences = sp.also { prefs ->
                memoryCache.clear()
                memoryCache.putAll(prefs.all)
            }
        }
    }

    @VisibleForTesting
    fun getCache() = memoryCache as Map<String, Any?>

    private companion object {
        const val MSG_NOT_YET_IMPLEMENTED = "Not yet implemented"
    }
}
