package com.uid2.sdk.network

import com.uid2.sdk.data.IdentityStatus.OPT_OUT
import com.uid2.sdk.data.IdentityStatus.REFRESHED
import com.uid2.sdk.data.IdentityStatus.REFRESH_EXPIRED
import com.uid2.sdk.data.TestData
import com.uid2.sdk.data.UID2Identity
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class RefreshResponseTest {
    @Test
    fun `test invalid json`() {
        listOf(
            JSONObject(),
            JSONObject(mapOf("key" to "value")),
            JSONObject(TestData.INVALID_REFRESH)
        ).forEach {
            val refresh = RefreshResponse.fromJson(it)
            assertNull(refresh)
        }
    }

    @Test
    fun `test valid json`() {
        val refresh = RefreshResponse.fromJson(JSONObject(TestData.VALID_REFRESH))

        // Verify that the parsed RefreshResponse matches what we expect for the given input test data.
        assertNotNull(refresh)
        assertNull(refresh?.message)
        assertEquals(RefreshResponse.Status.forStatus(TestData.VALID_REFRESH_STATUS), refresh?.status)

        // Verify that the identity was parsed correctly.
        val identity = UID2Identity.fromJson(JSONObject(TestData.VALID_IDENTITY))
        assertEquals(identity, refresh?.body)

        // Verify that when converted to a RefreshPackage, the identity still matches what we expect.
        val refreshPackage = refresh?.toRefreshPackage()
        assertNotNull(refreshPackage)
        assertEquals(identity, refreshPackage?.identity)
        assertEquals(REFRESHED, refreshPackage?.status)
    }

    @Test
    fun `test refresh package`() {
        mapOf(
            JSONObject(TestData.VALID_REFRESH_OPT_OUT) to OPT_OUT,
            JSONObject(TestData.VALID_REFRESH_EXPIRED_TOKEN) to REFRESH_EXPIRED
        ).forEach {
            val refresh = RefreshResponse.fromJson(it.key)?.toRefreshPackage()

            // Verify that the converted RefreshPackage includes the expected Status.
            assertNotNull(refresh)
            assertEquals(it.value, refresh?.status)
            assertNotNull(refresh?.message)
        }
    }
}
