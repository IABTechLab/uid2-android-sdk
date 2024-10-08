package com.uid2.securesignals.ima

import com.uid2.UID2
import org.junit.Assert.assertEquals
import org.junit.Test

class EUIDSecureSignalsAdapterTest {
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
        val expectedVersion = PluginVersion.getVersionInfo()

        assertEquals(expectedVersion.major, version.majorVersion)
        assertEquals(expectedVersion.minor, version.minorVersion)
        assertEquals(expectedVersion.patch, version.microVersion)
    }
}
