package com.uid2.network

import com.uid2.data.IdentityStatus.OPT_OUT
import com.uid2.data.IdentityStatus.REFRESHED
import com.uid2.data.IdentityStatus.REFRESH_EXPIRED
import com.uid2.data.TestData
import com.uid2.data.UID2Identity
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class RefreshResponseTest {
    @Test
    fun `test invalid json`() {
        // Verify that completely invalid json is handled correctly.
        listOf(
            JSONObject(),
            JSONObject(mapOf("key" to "value")),
        ).forEach {
            assertNull(RefreshResponse.fromJson(it))
        }

        // If we take a valid response but remove the "status" parameter, check that it's handled correctly.
        val validRefresh = JSONObject(TestData.REFRESH_TOKEN_SUCCESS_DECRYPTED)
        validRefresh.remove("status")

        val refresh = RefreshResponse.fromJson(validRefresh)
        assertNull(refresh)

        // The body of the response should contain enough attributes to build a UID2Identity instance. We will test that
        // if any of these parameters are missing, that we correctly handle it.
        listOf(
            "advertising_token",
            "refresh_token",
            "identity_expires",
            "refresh_expires",
            "refresh_from",
            "refresh_response_key",
        ).forEach {
            val success = JSONObject(TestData.REFRESH_TOKEN_SUCCESS_DECRYPTED)
            val body = success.getJSONObject("body")

            body.remove(it)

            assertNull(RefreshResponse.fromJson(success))
        }
    }

    @Test
    fun `test valid json`() {
        val refreshResponse = JSONObject(TestData.REFRESH_TOKEN_SUCCESS_DECRYPTED)
        val refresh = RefreshResponse.fromJson(refreshResponse)

        // Verify that the parsed RefreshResponse matches what we expect for the given input test data.
        assertNotNull(refresh)
        assertNull(refresh?.message)
        assertEquals(RefreshResponse.Status.SUCCESS, refresh?.status)

        // Verify that the identity was parsed correctly.
        val identity = UID2Identity.fromJson(refreshResponse.getJSONObject("body"))
        assertEquals(identity, refresh?.body)

        // Verify that when converted to a RefreshPackage, the identity still matches what we expect.
        val refreshPackage = refresh?.toResponsePackage(true)
        assertNotNull(refreshPackage)
        assertEquals(identity, refreshPackage?.identity)
        assertEquals(REFRESHED, refreshPackage?.status)
    }

    @Test
    fun `test refresh package`() {
        mapOf(
            JSONObject(TestData.VALID_REFRESH_OPT_OUT) to OPT_OUT,
            JSONObject(TestData.VALID_REFRESH_EXPIRED_TOKEN) to REFRESH_EXPIRED,
        ).forEach {
            val refresh = RefreshResponse.fromJson(it.key)?.toResponsePackage(true)

            // Verify that the converted RefreshPackage includes the expected Status.
            assertNotNull(refresh)
            assertEquals(it.value, refresh?.status)
        }
    }
}
