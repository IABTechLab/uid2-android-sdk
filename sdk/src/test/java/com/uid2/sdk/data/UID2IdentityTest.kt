package com.uid2.sdk.data

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class UID2IdentityTest {
    @Test
    fun `test invalid json`() {
        // Verify that completely invalid json is handled correctly.
        listOf(
            JSONObject(),
            JSONObject(mapOf("key" to "value"))
        ).forEach {
            val identity = UID2Identity.fromJson(it)
            assertNull(identity)
        }

        // Verify that if any of the mandatory parameters are missing, it's handled correctly.
        listOf(
            "advertising_token",
            "refresh_token",
            "identity_expires",
            "refresh_expires",
            "refresh_from",
            "refresh_response_key"
        ).forEach {
            val json = JSONObject(TestData.VALID_IDENTITY)
            json.remove(it)

            val identity = UID2Identity.fromJson(json)
            assertNull(identity)
        }
    }

    @Test
    fun `test valid json`() {
        val identity = UID2Identity.fromJson(JSONObject(TestData.VALID_IDENTITY))

        // Verify that the parsed UID2Identity matches what we expect for the given input test data.
        assertNotNull(identity)
        assertEquals(TestData.VALID_IDENTITY_AD_TOKEN, identity?.advertisingToken)
        assertEquals(TestData.VALID_IDENTITY_REFRESH_TOKEN, identity?.refreshToken)
        assertEquals(TestData.VALID_IDENTITY_EXPIRES, identity?.identityExpires)
        assertEquals(TestData.VALID_IDENTITY_REFRESH_FROM, identity?.refreshFrom)
        assertEquals(TestData.VALID_IDENTITY_REFRESH_EXPIRES, identity?.refreshExpires)
        assertEquals(TestData.VALID_IDENTITY_REFRESH_RESPONSE_KEY, identity?.refreshResponseKey)
    }
}
