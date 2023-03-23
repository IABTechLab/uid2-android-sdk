package com.uid2.securesignals.ima

import com.uid2.UID2
import org.junit.Assert.assertEquals
import org.junit.Test

class UID2SecureSignalsAdapterTest {
    @Test
    fun `test SDK version`() {
        val adapter = UID2SecureSignalsAdapter()
        val version = adapter.sdkVersion
        val expectedVersion = UID2.getVersionInfo()

        assertEquals(expectedVersion.major, version.majorVersion)
        assertEquals(expectedVersion.minor, version.minorVersion)
        assertEquals(expectedVersion.patch, version.microVersion)
    }

    @Test
    fun `test plugin version`() {
        val adapter = UID2SecureSignalsAdapter()
        val version = adapter.version
        val expectedVersion = UID2SecureSignals.getVersionInfo()

        assertEquals(expectedVersion.major, version.majorVersion)
        assertEquals(expectedVersion.minor, version.minorVersion)
        assertEquals(expectedVersion.patch, version.microVersion)
    }
}
