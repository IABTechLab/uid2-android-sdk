package com.uid2.network

import com.uid2.data.TestData
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class DataEnvelopeTest {
    @Test
    fun `test encrypted opt out`() {
        val payload = DataEnvelope.decrypt(
            TestData.REFRESH_TOKEN_ENCRYPTED_OPT_OUT_KEY,
            TestData.REFRESH_TOKEN_OPT_OUT_ENCRYPTED,
            true,
        )

        // Verify that the payload was actually decoded.
        assertNotNull(payload)

        // Compare the decrypted data against what we expect.
        val payloadJson = payload?.let { JSONObject(String(payload, Charsets.UTF_8)) }
        val expectedJson = JSONObject(TestData.REFRESH_TOKEN_OPT_OUT_DECRYPTED)
        assertEquals(expectedJson.toString(0), payloadJson?.toString(0))
    }

    @Test
    fun `test encrypted refresh`() {
        val payload = DataEnvelope.decrypt(
            TestData.REFRESH_TOKEN_ENCRYPTED_SUCCESS_KEY,
            TestData.REFRESH_TOKEN_SUCCESS_ENCRYPTED,
            true,
        )

        // Verify that the payload was actually decoded.
        assertNotNull(payload)

        // Compare the decrypted data against what we expect.
        val payloadJson = payload?.let { JSONObject(String(payload, Charsets.UTF_8)) }
        val expectedJson = JSONObject(TestData.REFRESH_TOKEN_SUCCESS_DECRYPTED)
        assertEquals(expectedJson.toString(0), payloadJson?.toString(0))
    }

    @Test
    fun `test invalid key`() {
        val payload = DataEnvelope.decrypt(
            "This is not a key",
            TestData.REFRESH_TOKEN_SUCCESS_ENCRYPTED,
            true,
        )

        // Verify that when attempting to decrypt valid data with an incorrect key, we are returned the expected null.
        assertNull(payload)
    }

    @Test
    fun `test invalid data`() {
        val payload = DataEnvelope.decrypt(
            TestData.REFRESH_TOKEN_ENCRYPTED_SUCCESS_KEY,
            "This is not valid",
            true,
        )

        // Verify that when attempting to decrypt invalid data with a valid key, we are returned the expected null.
        assertNull(payload)
    }
}
