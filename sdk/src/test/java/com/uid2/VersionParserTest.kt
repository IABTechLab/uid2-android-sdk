package com.uid2

import org.junit.Assert.assertEquals
import org.junit.Test

class VersionParserTest {

    @Test
    fun `test invalid versions`() {
        listOf(
            "1.0",
            "a.b.c",
            "1.0.1.1",
        ).forEach {
            val version = VersionParser.parseVersion(it)
            assertEquals(VersionParser.INVALID_VERSION, version)
        }
    }

    @Test
    fun `test valid version`() {
        val major = 1
        val minor = 2
        val patch = 3

        val version = VersionParser.parseVersion("$major.$minor.$patch")
        assertEquals(major, version.major)
        assertEquals(minor, version.minor)
        assertEquals(patch, version.patch)
    }

    @Test
    fun `test snapshot builds`() {
        val major = 1
        val minor = 2
        val patch = 3

        val version = VersionParser.parseVersion("$major.$minor.$patch-SNAPSHOT")
        assertEquals(major, version.major)
        assertEquals(minor, version.minor)
        assertEquals(patch, version.patch)
    }
}
