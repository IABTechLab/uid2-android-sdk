package com.uid2

import com.uid2.data.IdentityStatus
import com.uid2.data.TestData
import com.uid2.data.UID2Identity
import com.uid2.network.NetworkRequest
import com.uid2.network.NetworkResponse
import com.uid2.network.NetworkSession
import com.uid2.utils.Logger
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.junit.MockitoJUnitRunner
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

@RunWith(MockitoJUnitRunner::class)
class UID2ClientTest {
    private val networkSession: NetworkSession = mock()
    private val logger: Logger = mock()

    private val url = "https://test.dev"
    private val refreshToken = "RefreshToken"
    private val refreshKey = "RefreshKey"

    @Test
    fun `test invalid api url`() {
        val client = UID2Client(
            "this is not a url",
            networkSession,
            logger,
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
            networkSession,
            logger,
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
            networkSession,
            logger,
        )

        whenever(networkSession.loadData(any(), any())).thenReturn(
            NetworkResponse(200, "This is not encrypted"),
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
            networkSession,
            logger,
        )

        whenever(networkSession.loadData(any(), any())).thenReturn(
            NetworkResponse(200, TestData.REFRESH_TOKEN_SUCCESS_ENCRYPTED),
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
            networkSession,
            logger,
        )

        // Configure the network session to return a valid (encrypted) payload.
        whenever(networkSession.loadData(any(), any())).thenReturn(
            NetworkResponse(200, TestData.REFRESH_TOKEN_SUCCESS_ENCRYPTED),
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
            networkSession,
            logger,
        )

        // Configure the network session to return a valid (encrypted) payload.
        whenever(networkSession.loadData(any(), any())).thenReturn(
            NetworkResponse(200, TestData.REFRESH_TOKEN_OPT_OUT_ENCRYPTED),
        )

        // Verify that the payload was successfully decrypted, and parsed.
        val identity = client.refreshIdentity(refreshToken, TestData.REFRESH_TOKEN_ENCRYPTED_OPT_OUT_KEY)
        assertEquals(IdentityStatus.OPT_OUT, identity.status)
        assertNull(identity.identity)
    }

    @Test
    fun `test version info`() = runBlocking {
        val client = UID2Client(
            url,
            networkSession,
            logger,
        )

        // Configure the network session to return a valid (encrypted) payload and allows us to capture the given
        // NetworkRequest.
        var networkRequest: NetworkRequest? = null
        whenever(networkSession.loadData(any(), any())).thenAnswer {
            networkRequest = it.arguments[1] as NetworkRequest?
            return@thenAnswer NetworkResponse(200, TestData.REFRESH_TOKEN_SUCCESS_ENCRYPTED)
        }

        // Ask the Client to refresh the Identity, so it can make the appropriate request.
        client.refreshIdentity(refreshToken, TestData.REFRESH_TOKEN_ENCRYPTED_SUCCESS_KEY)

        // Verify that the Client included the expected Version header, and that it ended with our SDK version.
        val reportedVersion = networkRequest?.headers?.get("X-UID2-Client-Version")
        assertNotNull(reportedVersion)
        assertTrue(reportedVersion?.endsWith(UID2.getVersion()) == true)
    }
}
