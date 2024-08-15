package com.uid2.data

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class IdentityRequestTest {

    @Test
    fun `test email payload`() {
        val payload = IdentityRequest.Email("test.test@test.com").toPayload()
        val jsonPayload = JSONObject(payload)

        // Verify that we receive the expected JSON payload
        assertNotNull(jsonPayload)
        assertTrue(jsonPayload.has(FIELD_EMAIL_HASH))
        assertEquals("dvECjPKZHya0/SIhSGwP0m8SgTv1vzLxPULUOsm880M=", jsonPayload[FIELD_EMAIL_HASH])
    }

    @Test
    fun `test email hash payload`() {
        val hash = "this-is-a-hash"
        val payload = IdentityRequest.EmailHash(hash).toPayload()
        val jsonPayload = JSONObject(payload)

        // Verify that we receive the expected JSON payload
        assertNotNull(jsonPayload)
        assertTrue(jsonPayload.has(FIELD_EMAIL_HASH))
        assertEquals(hash, jsonPayload[FIELD_EMAIL_HASH])
    }

    @Test
    fun `test phone payload`() {
        val payload = IdentityRequest.Phone("+1234567890").toPayload()
        val jsonPayload = JSONObject(payload)

        // Verify that we receive the expected JSON payload
        assertNotNull(jsonPayload)
        assertTrue(jsonPayload.has(FIELD_PHONE_HASH))
        assertEquals("QizoLG/BckrIeAQvfQVWU6temD0YbmFoJqctQ4S2ivg=", jsonPayload[FIELD_PHONE_HASH])
    }

    @Test
    fun `test phone hash payload`() {
        val hash = "this-is-a-hash"
        val payload = IdentityRequest.PhoneHash(hash).toPayload()
        val jsonPayload = JSONObject(payload)

        // Verify that we receive the expected JSON payload
        assertNotNull(jsonPayload)
        assertTrue(jsonPayload.has(FIELD_PHONE_HASH))
        assertEquals(hash, jsonPayload[FIELD_PHONE_HASH])
    }

    private companion object {
        const val FIELD_EMAIL_HASH = "email_hash"
        const val FIELD_PHONE_HASH = "phone_hash"
    }
}
