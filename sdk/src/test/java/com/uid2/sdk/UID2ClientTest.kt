package com.uid2.sdk

import com.uid2.sdk.data.IdentityStatus
import com.uid2.sdk.data.TestData
import com.uid2.sdk.data.UID2Identity
import com.uid2.sdk.network.NetworkResponse
import com.uid2.sdk.network.NetworkSession
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.junit.MockitoJUnitRunner
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

@RunWith(MockitoJUnitRunner::class)
class UID2ClientTest {
    private val networkSession: NetworkSession = mock()

    private val url = "https://test.dev"
    private val refreshToken = "RefreshToken"
    private val refreshKey = "RefreshKey"

    @Test
    fun `test invalid api url`() {
        val client = UID2Client(
            "this is not a url",
            networkSession
        )

        // Verify that when we have configured the client with an invalid URL, that it throws the appropriate exception
        // when we try to refresh the Identity.
        assertThrows(InvalidApiUrlException::class.java) {
            runBlocking { client.refreshIdentity(refreshToken, refreshKey) }
        }
    }


    @Test
    fun `test network failure`() {
        val client = UID2Client(
            url,
            networkSession
        )

        // Configure the network session to report a failure.
        whenever(networkSession.loadData(any(), any())).thenReturn(NetworkResponse(400))

        // Verify that when a network failure occurs, the appropriate exception is thrown.
        assertThrows(RefreshTokenException::class.java) {
            runBlocking { client.refreshIdentity(refreshToken, refreshKey) }
        }
    }

    @Test
    fun `test invalid data failure`() {
        val client = UID2Client(
            url,
            networkSession
        )

        whenever(networkSession.loadData(any(), any())).thenReturn(
            NetworkResponse(200, "This is not encrypted")
        )

        // Verify that when an unexpected response is returned, the appropriate exception is thrown.
        assertThrows(PayloadDecryptException::class.java) {
            runBlocking { client.refreshIdentity(refreshToken, refreshKey) }
        }
    }

    @Test
    fun `test invalid data key`() {
        val client = UID2Client(
            url,
            networkSession
        )

        whenever(networkSession.loadData(any(), any())).thenReturn(
            NetworkResponse(200, TestData.REFRESH_TOKEN_SUCCESS_ENCRYPTED)
        )

        // Verify that when an unexpected response is returned, the appropriate exception is thrown.
        assertThrows(PayloadDecryptException::class.java) {
            runBlocking { client.refreshIdentity(refreshToken, "This is not a key") }
        }
    }

    @Test
    fun `test successful refresh`() = runBlocking {
        val client = UID2Client(
            url,
            networkSession
        )

        // Configure the network session to return a valid (encrypted) payload.
        whenever(networkSession.loadData(any(), any())).thenReturn(
            NetworkResponse(200, TestData.REFRESH_TOKEN_SUCCESS_ENCRYPTED)
        )

        // Verify that the payload was successfully decrypted, and parsed.
        val identity = client.refreshIdentity(refreshToken, TestData.REFRESH_TOKEN_ENCRYPTED_SUCCESS_KEY)
        assertEquals(IdentityStatus.REFRESHED, identity.status)

        // Verify that the returned package has an identity that matches what we included in the body of the response.
        val expectedIdentity = JSONObject(TestData.REFRESH_TOKEN_SUCCESS_DECRYPTED).getJSONObject("body").let {
            UID2Identity.fromJson(it)
        }
        assertEquals(expectedIdentity, identity.identity)
    }

    @Test
    fun `test successful optout`() = runBlocking {
        val client = UID2Client(
            url,
            networkSession
        )

        // Configure the network session to return a valid (encrypted) payload.
        whenever(networkSession.loadData(any(), any())).thenReturn(
            NetworkResponse(200, TestData.REFRESH_TOKEN_OPT_OUT_ENCRYPTED)
        )

        // Verify that the payload was successfully decrypted, and parsed.
        val identity = client.refreshIdentity(refreshToken, TestData.REFRESH_TOKEN_ENCRYPTED_OPT_OUT_KEY)
        assertEquals(IdentityStatus.OPT_OUT, identity.status)
        assertNull(identity.identity)
    }
}