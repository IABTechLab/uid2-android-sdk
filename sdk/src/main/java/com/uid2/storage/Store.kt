package com.uid2.storage

/**
 * Store : interface that is a (mostly) drop-in replacement for SharedPreferences.
 */
@Suppress("TooManyFunctions")
interface Store {
    suspend fun initializeSharedPrefs()

    fun getBoolean(key: String): Boolean
    fun getBoolean(key: String, defaultValue: Boolean): Boolean
    fun putBoolean(key: String, value: Boolean)
    suspend fun putBooleanCommit(key: String, value: Boolean)

    fun getFloat(key: String): Float
    fun getFloat(key: String, defaultValue: Float): Float
    fun putFloat(key: String, value: Float)
    suspend fun putFloatCommit(key: String, value: Float)

    fun getInt(key: String): Int
    fun getInt(key: String, defaultValue: Int): Int
    fun putInt(key: String, value: Int)
    suspend fun putIntCommit(key: String, value: Int)

    fun getLong(key: String): Long
    fun getLong(key: String, defaultValue: Long): Long
    fun putLong(key: String, value: Long)
    suspend fun putLongCommit(key: String, value: Long)

    fun getString(key: String): String?
    fun getString(key: String, defaultValue: String?): String?
    fun putString(key: String, value: String?)
    suspend fun putStringCommit(key: String, value: String?)

    fun getStringSet(key: String): Set<String>?
    fun getStringSet(key: String, defaultValue: Set<String>?): Set<String>?
    fun putStringSet(key: String, value: Set<String>?)
    suspend fun putStringSetCommit(key: String, value: Set<String>)

    fun contains(key: String): Boolean
    fun clear()
    fun remove(key: String)

    companion object {
        const val BOOLEAN_NOT_FOUND_DEFAULT = false
        const val FLOAT_NOT_FOUND_DEFAULT = Float.MIN_VALUE
        const val INT_NOT_FOUND_DEFAULT = Integer.MIN_VALUE
        const val LONG_NOT_FOUND_DEFAULT = Long.MIN_VALUE
        val STRING_NOT_FOUND_DEFAULT = null
        val STRING_SET_NOT_FOUND_DEFAULT = null
    }
}
