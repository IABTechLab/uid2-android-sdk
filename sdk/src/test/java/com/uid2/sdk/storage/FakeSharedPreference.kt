package com.uid2.sdk.storage

import android.content.SharedPreferences
import android.content.SharedPreferences.OnSharedPreferenceChangeListener
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking

/**
 * Fake SharedPreference backed by a Map for testing purposes
 */
internal class FakeSharedPreference : SharedPreferences {

    private val preferenceMap: MutableMap<String, Any?>

    private val preferenceEditor: FakeSharedPreferenceEditor

    init {
        preferenceMap = HashMap()
        preferenceEditor = FakeSharedPreferenceEditor(preferenceMap)
    }

    override fun getAll(): Map<String, *> {
        return preferenceMap
    }

    override fun getString(key: String, defaultValue: String?): String? {
        return when (preferenceMap.containsKey(key)) {
            true -> preferenceMap[key] as String
            false -> defaultValue
        }
    }

    @Suppress("UNCHECKED_CAST")
    override fun getStringSet(key: String, defaultValue: Set<String>?): Set<String>? {
        return when (preferenceMap.containsKey(key)) {
            true -> preferenceMap[key] as Set<String>
            false -> defaultValue
        }
    }

    override fun getInt(key: String, defaultValue: Int): Int {
        return when (preferenceMap.containsKey(key)) {
            true -> preferenceMap[key] as Int
            false -> defaultValue
        }
    }

    override fun getLong(key: String, defaultValue: Long): Long {
        return when (preferenceMap.containsKey(key)) {
            true -> preferenceMap[key] as Long
            false -> defaultValue
        }
    }

    override fun getFloat(key: String, defaultValue: Float): Float {
        return when (preferenceMap.containsKey(key)) {
            true -> preferenceMap[key] as Float
            false -> defaultValue
        }
    }

    override fun getBoolean(key: String, defaultValue: Boolean): Boolean {
        return when (preferenceMap.containsKey(key)) {
            true -> preferenceMap[key] as Boolean
            false -> defaultValue
        }
    }

    override fun contains(key: String): Boolean {
        return preferenceMap.containsKey(key)
    }

    override fun edit(): SharedPreferences.Editor {
        return preferenceEditor
    }

    override fun registerOnSharedPreferenceChangeListener(
        onSharedPreferenceChangeListener: OnSharedPreferenceChangeListener
    ) = Unit

    override fun unregisterOnSharedPreferenceChangeListener(
        onSharedPreferenceChangeListener: OnSharedPreferenceChangeListener
    ) = Unit

    inner class FakeSharedPreferenceEditor(private val preferenceMap: MutableMap<String, Any?>) :
        SharedPreferences.Editor {

        override fun putBoolean(key: String, value: Boolean): SharedPreferences.Editor {
            runBlocking { delay(WRITE_DELAY_MILLI_SEC) }
            preferenceMap[key] = value
            return this
        }

        override fun putFloat(key: String, value: Float): SharedPreferences.Editor {
            runBlocking { delay(WRITE_DELAY_MILLI_SEC) }
            preferenceMap[key] = value
            return this
        }

        override fun putInt(key: String, value: Int): SharedPreferences.Editor {
            runBlocking { delay(WRITE_DELAY_MILLI_SEC) }
            preferenceMap[key] = value
            return this
        }

        override fun putLong(key: String, value: Long): SharedPreferences.Editor {
            runBlocking { delay(WRITE_DELAY_MILLI_SEC) }
            preferenceMap[key] = value
            return this
        }

        override fun putString(key: String, value: String?): SharedPreferences.Editor {
            runBlocking { delay(WRITE_DELAY_MILLI_SEC) }
            preferenceMap[key] = value
            return this
        }

        override fun putStringSet(key: String, value: Set<String>?): SharedPreferences.Editor {
            runBlocking { delay(WRITE_DELAY_MILLI_SEC) }
            preferenceMap[key] = value
            return this
        }

        override fun apply() { /** NO-OP **/ }

        override fun clear(): SharedPreferences.Editor {
            runBlocking { delay(WRITE_DELAY_MILLI_SEC) }
            preferenceMap.clear()
            return this
        }

        override fun commit(): Boolean {
            runBlocking { delay(WRITE_DELAY_MILLI_SEC) }
            return true
        }

        override fun remove(key: String): SharedPreferences.Editor {
            runBlocking { delay(WRITE_DELAY_MILLI_SEC) }
            preferenceMap.remove(key)
            return this
        }
    }

    companion object {
        const val WRITE_DELAY_MILLI_SEC = 100L
    }
}
