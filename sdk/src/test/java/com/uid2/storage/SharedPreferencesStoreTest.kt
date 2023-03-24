package com.uid2.storage

import android.app.Application
import android.content.Context
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.junit.MockitoJUnitRunner
import org.mockito.kotlin.mock

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(MockitoJUnitRunner::class)
internal class SharedPreferencesStoreTest {

    private lateinit var sharedPrefsStore: SharedPrefTestStore
    private lateinit var fakeSharedPrefs: FakeSharedPreference
    private lateinit var testDispatcher: TestDispatcher

    private val context: Application = mock()


    @Before
    fun setupTest() {
        testDispatcher = StandardTestDispatcher()
        fakeSharedPrefs = FakeSharedPreference()
        sharedPrefsStore = SharedPrefTestStore(context, testDispatcher)
        sharedPrefsStore.setSharedPreferences(fakeSharedPrefs)
    }

    @After
    fun teardownTest() {
        sharedPrefsStore.clear()
        testDispatcher.scheduler.advanceUntilIdle()
    }

    /** ==== Boolean ================================================================================================ */
    @Test
    fun `boolean get, no default, no value stored, returns default`() {
        assertEquals(Store.BOOLEAN_NOT_FOUND_DEFAULT, sharedPrefsStore.getBoolean(key1))
    }

    @Test
    fun `boolean get, no default, no value stored, default also writes default value to cache`() {
        sharedPrefsStore.getBoolean(key1)
        assertTrue(sharedPrefsStore.getCache().contains(key1))
    }

    @Test
    fun `boolean get, no default, no value stored, default not written to backing data`() {
        sharedPrefsStore.getBoolean(key1)
        testDispatcher.scheduler.advanceUntilIdle()
        assertFalse(fakeSharedPrefs.contains(key1))
    }

    @Test
    fun `boolean get with default, no value stored, returns default`() {
        val defaultValue = true
        assertEquals(defaultValue, sharedPrefsStore.getBoolean(key1, defaultValue))
    }

    @Test
    fun `boolean get with default, no value stored, also writes default value to cache`() {
        sharedPrefsStore.getBoolean(key1, true)
        assertTrue(sharedPrefsStore.getCache().contains(key1))
    }

    @Test
    fun `boolean get with default, no value stored, default not written to backing data`() {
        sharedPrefsStore.getBoolean(key1, true)
        testDispatcher.scheduler.advanceUntilIdle()
        assertFalse(fakeSharedPrefs.contains(key1))
    }

    @Test
    fun `boolean put saves value to cache`() {
        sharedPrefsStore.putBoolean(key1, true)
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(fakeSharedPrefs.contains(key1))
    }

    @Test
    fun `boolean put writes to backing data asynchronously`() {
        val value = true
        sharedPrefsStore.putBoolean(key1, value)

        assertFalse(fakeSharedPrefs.contains(key1)) // not there yet
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(value, fakeSharedPrefs.getBoolean(key1, !value))
    }

    @Test
    fun `boolean put with commit writes to backing data asynchronously`() {
        val value = true
        sharedPrefsStore.putBoolean(key1, value)

        assertFalse(fakeSharedPrefs.contains(key1)) // not there yet
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(value, fakeSharedPrefs.getBoolean(key1, !value))
    }

    /** ==== Float ================================================================================================== */
    @Test
    fun `float get, no default, no value stored, returns default`() {
        assertEquals(Store.FLOAT_NOT_FOUND_DEFAULT, sharedPrefsStore.getFloat(key1))
    }

    @Test
    fun `float get, no default, no value stored, default also writes default value to cache`() {
        sharedPrefsStore.getFloat(key1)
        assertTrue(sharedPrefsStore.getCache().contains(key1))
    }

    @Test
    fun `float get, no default, no value stored, default not written to backing data`() {
        sharedPrefsStore.getFloat(key1)
        testDispatcher.scheduler.advanceUntilIdle()
        assertFalse(fakeSharedPrefs.contains(key1))
    }

    @Test
    fun `float get with default, no value stored, returns default`() {
        assertEquals(PI, sharedPrefsStore.getFloat(key1, PI))
    }

    @Test
    fun `float get with default, no value stored, also writes default value to cache`() {
        sharedPrefsStore.getFloat(key1, PI)
        assertTrue(sharedPrefsStore.getCache().contains(key1))
    }

    @Test
    fun `float get with default, no value stored, default not written to backing data`() {
        sharedPrefsStore.getFloat(key1, PI)
        testDispatcher.scheduler.advanceUntilIdle()
        assertFalse(fakeSharedPrefs.contains(key1))
    }

    @Test
    fun `float put saves value to cache`() {
        sharedPrefsStore.putFloat(key1, PI)
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(fakeSharedPrefs.contains(key1))
    }

    @Test
    fun `float put writes to backing data asynchronously`() {
        sharedPrefsStore.putFloat(key1, PI)

        assertFalse(fakeSharedPrefs.contains(key1)) // not there yet
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(PI, fakeSharedPrefs.getFloat(key1, PI + 1.0F))
    }

    @Test
    fun `float put with commit writes to backing data asynchronously`() {
        sharedPrefsStore.putFloat(key1, PI)

        assertFalse(fakeSharedPrefs.contains(key1)) // not there yet
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(PI, fakeSharedPrefs.getFloat(key1, PI + 1.0F))
    }

    /** ==== Int ==================================================================================================== */
    @Test
    fun `int get, no default, no value stored, returns default`() {
        assertEquals(Store.INT_NOT_FOUND_DEFAULT, sharedPrefsStore.getInt(key1))
    }

    @Test
    fun `int get, no default, no value stored, default also writes default value to cache`() {
        sharedPrefsStore.getInt(key1)
        assertTrue(sharedPrefsStore.getCache().contains(key1))
    }

    @Test
    fun `int get, no default, no value stored, default not written to backing data`() {
        sharedPrefsStore.getInt(key1)
        testDispatcher.scheduler.advanceUntilIdle()
        assertFalse(fakeSharedPrefs.contains(key1))
    }

    @Test
    fun `int get with default, no value stored, returns default`() {
        val default = 1
        assertEquals(default, sharedPrefsStore.getInt(key1, default))
    }

    @Test
    fun `int get with default, no value stored, also writes default value to cache`() {
        sharedPrefsStore.getInt(key1, 1)
        assertTrue(sharedPrefsStore.getCache().contains(key1))
    }

    @Test
    fun `int get with default, no value stored, default not written to backing data`() {
        sharedPrefsStore.getInt(key1, 1)
        testDispatcher.scheduler.advanceUntilIdle()
        assertFalse(fakeSharedPrefs.contains(key1))
    }

    @Test
    fun `int put saves value to cache`() {
        sharedPrefsStore.putInt(key1, 1)
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(fakeSharedPrefs.contains(key1))
    }

    @Test
    fun `int put writes to backing data asynchronously`() {
        val value = 1
        sharedPrefsStore.putInt(key1, value)

        assertFalse(fakeSharedPrefs.contains(key1)) // not there yet
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(value, fakeSharedPrefs.getInt(key1, value + 1))
    }

    @Test
    fun `int put with commit writes to backing data asynchronously`() {
        val value = 1
        sharedPrefsStore.putInt(key1, value)

        assertFalse(fakeSharedPrefs.contains(key1)) // not there yet
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(value, fakeSharedPrefs.getInt(key1, value + 1))
    }

    /** ==== String ================================================================================================= */
    @Test
    fun `string get, no default, no value stored, returns default`() {
        assertEquals(Store.STRING_NOT_FOUND_DEFAULT, sharedPrefsStore.getString(key1))
    }

    @Test
    fun `string get, no default, no value stored, since default is null, it does NOT write default value to cache`() {
        sharedPrefsStore.getString(key1)
        assertFalse(sharedPrefsStore.getCache().contains(key1))
    }

    @Test
    fun `string get, no default, no value stored, default not written to backing data`() {
        sharedPrefsStore.getString(key1)
        testDispatcher.scheduler.advanceUntilIdle()
        assertFalse(fakeSharedPrefs.contains(key1))
    }

    @Test
    fun `string get with default, no value stored, returns default`() {
        assertEquals(STR, sharedPrefsStore.getString(key1, STR))
    }

    @Test
    fun `string get with default, no value stored, also writes default value to cache`() {
        sharedPrefsStore.getString(key1, STR)
        assertTrue(sharedPrefsStore.getCache().contains(key1))
    }

    @Test
    fun `string get with default, no value stored, default not written to backing data`() {
        sharedPrefsStore.getString(key1, STR)
        testDispatcher.scheduler.advanceUntilIdle()
        assertFalse(fakeSharedPrefs.contains(key1))
    }

    @Test
    fun `string put saves value to cache`() {
        sharedPrefsStore.putString(key1, STR)
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(fakeSharedPrefs.contains(key1))
    }

    @Test
    fun `string put writes to backing data asynchronously`() {
        sharedPrefsStore.putString(key1, STR)

        assertFalse(fakeSharedPrefs.contains(key1)) // not there yet
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(STR, fakeSharedPrefs.getString(key1, "$STR $STR"))
    }

    @Test
    fun `string put with commit writes to backing data asynchronously`() {
        sharedPrefsStore.putString(key1, STR)

        assertFalse(fakeSharedPrefs.contains(key1)) // not there yet
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(STR, fakeSharedPrefs.getString(key1, "$STR $STR"))
    }

    /** ==== StringSet ============================================================================================== */
    @Test
    fun `stringSet get, no default, no value stored, returns default`() {
        assertEquals(Store.STRING_SET_NOT_FOUND_DEFAULT, sharedPrefsStore.getStringSet(key1))
    }

    @Test
    fun `stringSet, no default, no value stored, since default is null, it does NOT write default value to cache`() {
        sharedPrefsStore.getStringSet(key1)
        assertFalse(sharedPrefsStore.getCache().contains(key1))
    }

    @Test
    fun `stringSet get, no default, no value stored, default not written to backing data`() {
        sharedPrefsStore.getStringSet(key1)
        testDispatcher.scheduler.advanceUntilIdle()
        assertFalse(fakeSharedPrefs.contains(key1))
    }

    @Test
    fun `stringSet get with default, no value stored, returns default`() {
        assertEquals(STR_SET, sharedPrefsStore.getStringSet(key1, STR_SET))
    }

    @Test
    fun `stringSet get with default, no value stored, also writes default value to cache`() {
        sharedPrefsStore.getStringSet(key1, STR_SET)
        assertTrue(sharedPrefsStore.getCache().contains(key1))
    }

    @Test
    fun `stringSet get with default, no value stored, default not written to backing data`() {
        sharedPrefsStore.getStringSet(key1, STR_SET)
        testDispatcher.scheduler.advanceUntilIdle()
        assertFalse(fakeSharedPrefs.contains(key1))
    }

    @Test
    fun `stringSet put saves value to cache`() {
        sharedPrefsStore.putStringSet(key1, STR_SET)
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(fakeSharedPrefs.contains(key1))
    }

    @Test
    fun `stringSet put writes to backing data asynchronously`() {
        sharedPrefsStore.putStringSet(key1, STR_SET)
        val badDefault = setOf("1")

        assertFalse(fakeSharedPrefs.contains(key1)) // not there yet
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(STR_SET, fakeSharedPrefs.getStringSet(key1, badDefault))
    }

    @Test
    fun `stringSet put with commit writes to backing data asynchronously`() {
        sharedPrefsStore.putStringSet(key1, STR_SET)
        val badDefault = setOf("1")

        assertFalse(fakeSharedPrefs.contains(key1)) // not there yet
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(STR_SET, fakeSharedPrefs.getStringSet(key1, badDefault))
    }

    /** ==== Other functions ======================================================================================== */
    @Test
    fun contains() {
        sharedPrefsStore.putBoolean(key1, false)
        assertTrue(sharedPrefsStore.contains(key1))
    }

    @Test
    fun clear() {
        val testValue1 = 6

        // Test store and retrieve
        sharedPrefsStore.putInt(key1, testValue1)
        assertEquals(testValue1, sharedPrefsStore.getInt(key1))

        sharedPrefsStore.clear()

        // Will return min_value
        assertNotEquals(testValue1, sharedPrefsStore.getInt(key1))
    }

    @Test
    fun remove() {
        val testValue1 = 6

        // Test store and retrieve
        sharedPrefsStore.putInt(key1, testValue1)
        assertEquals(testValue1, sharedPrefsStore.getInt(key1))

        sharedPrefsStore.remove(key1)

        // No longer returns value after remove
        assertNotEquals(testValue1, sharedPrefsStore.getInt(key1))
    }

    inner class SharedPrefTestStore(
        context: Context,
        dispatcher: CoroutineDispatcher
    ) : SharedPreferencesStore(context, dispatcher) {
        override fun getSharedPreferencesName(): String = "TestStore"
    }

    private companion object {
        const val key1 = "key1"
        const val key2 = "key2"
        const val PI = 3.141F
        const val STR = "Hello"
        val STR_SET = mutableSetOf("1", "2", "3")
    }
}
