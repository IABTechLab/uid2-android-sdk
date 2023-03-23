package com.uid2.securesignals.gma

import com.uid2.UID2
import org.junit.Assert
import org.junit.Test

class UID2MediationAdapterTest {
    @Test
    fun `test SDK version`() {
        val adapter = UID2MediationAdapter()
        val version = adapter.versionInfo
        val expectedVersion = UID2.getVersionInfo()

        Assert.assertEquals(expectedVersion.major, version.majorVersion)
        Assert.assertEquals(expectedVersion.minor, version.minorVersion)
        Assert.assertEquals(expectedVersion.patch, version.microVersion)
    }

    @Test
    fun `test plugin version`() {
        val adapter = UID2MediationAdapter()
        val version = adapter.sdkVersionInfo
        val expectedVersion = UID2SecureSignals.getVersionInfo()

        Assert.assertEquals(expectedVersion.major, version.majorVersion)
        Assert.assertEquals(expectedVersion.minor, version.minorVersion)
        Assert.assertEquals(expectedVersion.patch, version.microVersion)
    }
}
