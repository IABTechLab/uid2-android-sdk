package com.uid2

import com.uid2.data.IdentityRequest
import com.uid2.data.IdentityStatus
import com.uid2.data.TestData
import com.uid2.data.UID2Identity
import com.uid2.network.DataEnvelope
import com.uid2.network.NetworkRequest
import com.uid2.network.NetworkResponse
import com.uid2.network.NetworkSession
import com.uid2.utils.KeyUtils
import com.uid2.utils.Logger
import com.uid2.utils.TimeUtils
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.runTest
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.junit.MockitoJUnitRunner
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import java.security.KeyPair
import java.security.PublicKey

@RunWith(MockitoJUnitRunner::class)
class UID2ClientTest {
    private val testDispatcher: TestDispatcher = StandardTestDispatcher()

    private val networkSession: NetworkSession = mock()
    private val packageName = "com.uid2.devapp"
    private val dataEnvelope: DataEnvelope = mock()
    private val keyUtils: KeyUtils = mock()
    private val timeUtils: TimeUtils = mock()
    private val logger: Logger = mock()

    private val url = "https://test.dev"
    private val refreshToken = "RefreshToken"
    private val refreshKey = "RefreshKey"

    private val keyPair: KeyPair = mock()
    private val keyPairPublic: PublicKey = mock()
    private val keyPairPublicEncoded = ByteArray(12)

    private val SUBSCRIPTION_ID = "subscription_id"
    private val PUBLIC_KEY = "public_key"

    @Before
    fun before() {
        // By default, don't encrypt the data. Just convert it directly to a ByteArray.
        whenever(dataEnvelope.encrypt(any(), any(), any(), any())).thenAnswer {
            return@thenAnswer (it.arguments[1] as String).toByteArray()
        }

        whenever(keyUtils.generateServerPublicKey(any())).thenReturn(mock())
        whenever(keyUtils.generateKeyPair()).thenReturn(keyPair)
        whenever(keyUtils.generateSharedSecret(any(), any())).thenReturn(mock())
        whenever(keyUtils.generateIv(any())).thenAnswer { ByteArray(it.arguments[0] as Int) }
        whenever(keyUtils.generateAad(any())).thenReturn("")

        whenever(keyPairPublic.encoded).thenReturn(keyPairPublicEncoded)
        whenever(keyPair.public).thenReturn(keyPairPublic)

        whenever(timeUtils.now()).thenReturn(0)
    }

    //region generateIdentity

    @Test
    fun `test generate with invalid api url`() = runTest(testDispatcher) {
        testInvalidClientApi { client ->
            client.generateIdentity(
                IdentityRequest.Email("test@test.com"),
                SUBSCRIPTION_ID,
                PUBLIC_KEY,
            )
        }
    }

    @Test
    fun `test generate with public key failure`() = runTest(testDispatcher) {
        val client = withClient()

        // Mock the KeyUtils to fail to generate the required PublicKey.
        whenever(keyUtils.generateServerPublicKey(any())).thenReturn(null)

        // Verify the expected CryptoException is thrown.
        assertThrows(CryptoException::class.java) {
            runTest(testDispatcher) {
                client.generateIdentity(
                    IdentityRequest.Email("test@test.com"),
                    SUBSCRIPTION_ID,
                    PUBLIC_KEY,
                )
            }
        }
    }

    @Test
    fun `test generate with key pair failure`() = runTest(testDispatcher) {
        val client = withClient()

        // Mock the KeyUtils to fail to generate the required KeyPair.
        whenever(keyUtils.generateKeyPair()).thenReturn(null)

        // Verify the expected CryptoException is thrown.
        assertThrows(CryptoException::class.java) {
            runTest(testDispatcher) {
                client.generateIdentity(
                    IdentityRequest.Email("test@test.com"),
                    SUBSCRIPTION_ID,
                    PUBLIC_KEY,
                )
            }
        }
    }

    @Test
    fun `test generate with shared secret failure`() = runTest(testDispatcher) {
        val client = withClient()

        // Mock the KeyUtils to fail to generate the required SharedSecret.
        whenever(keyUtils.generateSharedSecret(any(), any())).thenReturn(null)

        // Verify the expected CryptoException is thrown.
        assertThrows(CryptoException::class.java) {
            runTest(testDispatcher) {
                client.generateIdentity(
                    IdentityRequest.Email("test@test.com"),
                    SUBSCRIPTION_ID,
                    PUBLIC_KEY,
                )
            }
        }
    }

    @Test
    fun `test generate with encryption failure`() = runTest(testDispatcher) {
        val client = withClient()

        // Mock the DataEnvelope to fail to encrypt the given payload.
        whenever(dataEnvelope.encrypt(any(), any(), any(), any())).thenReturn(null)

        // Verify the expected CryptoException is thrown.
        assertThrows(CryptoException::class.java) {
            runTest(testDispatcher) {
                client.generateIdentity(
                    IdentityRequest.Email("test@test.com"),
                    SUBSCRIPTION_ID,
                    PUBLIC_KEY,
                )
            }
        }
    }

    @Test
    fun `test generate with network failure`() = runTest(testDispatcher) {
        testNetworkFailure { client ->
            client.generateIdentity(
                IdentityRequest.Email("test@test.com"),
                SUBSCRIPTION_ID,
                PUBLIC_KEY,
            )
        }
    }

    @Test
    fun `test generate with decryption failure`() = runTest(testDispatcher) {
        val client = withClient()

        // Mock the DataEnvelope to fail to decrypt the given payload.
        whenever(networkSession.loadData(any(), any())).thenReturn(NetworkResponse(200, "somedata"))
        whenever(dataEnvelope.decrypt(anyOrNull<ByteArray>(), any(), any())).thenReturn(null)

        // Verify the expected CryptoException is thrown.
        assertThrows(PayloadDecryptException::class.java) {
            runTest(testDispatcher) {
                client.generateIdentity(
                    IdentityRequest.Email("test@test.com"),
                    SUBSCRIPTION_ID,
                    PUBLIC_KEY,
                )
            }
        }
    }

    @Test
    fun `test generate success`() = runTest(testDispatcher) {
        val client = withClient()

        val unencrypted = JSONObject(TestData.REFRESH_TOKEN_SUCCESS_DECRYPTED)
        whenever(dataEnvelope.decrypt(anyOrNull<ByteArray>(), any(), any())).thenReturn(
            unencrypted.toString().toByteArray(),
        )
        whenever(networkSession.loadData(any(), any())).thenReturn(NetworkResponse(200, "some data"))

        val response = client.generateIdentity(
            IdentityRequest.Email("test@test.com"),
            SUBSCRIPTION_ID,
            PUBLIC_KEY,
        )
        assertNotNull(response)

        // Verify that the returned package has an identity that matches what we included in the body of the response.
        val expectedIdentity = unencrypted.getJSONObject("body").let {
            UID2Identity.fromJson(it)
        }
        assertEquals(expectedIdentity, response.identity)
    }

    //endregion

    //region refreshIdentity

    @Test
    fun `test refresh with invalid api url`() = runTest(testDispatcher) {
        testInvalidClientApi { client ->
            client.refreshIdentity(refreshToken, refreshKey)
        }
    }

    @Test
    fun `test refresh with network failure`() = runTest(testDispatcher) {
        testNetworkFailure { client ->
            client.refreshIdentity(refreshToken, refreshKey)
        }
    }

    @Test
    fun `test refresh with invalid data failure`() = runTest(testDispatcher) {
        testInvalidNetworkResponse { client ->
            client.refreshIdentity(refreshToken, refreshKey)
        }
    }

    @Test
    fun `test refresh with invalid data key`() = runTest(testDispatcher) {
        val client = withClient()

        whenever(dataEnvelope.decrypt(any<String>(), any(), any())).thenReturn(null)
        whenever(networkSession.loadData(any(), any())).thenReturn(NetworkResponse(200, "some data"))

        // Verify that when an unexpected response is returned, the appropriate exception is thrown.
        assertThrows(PayloadDecryptException::class.java) {
            runBlocking { client.refreshIdentity(refreshToken, "This is not a key") }
        }
    }

    @Test
    fun `test successful refresh`() = runTest(testDispatcher) {
        val client = withClient()

        val unencrypted = JSONObject(TestData.REFRESH_TOKEN_SUCCESS_DECRYPTED)
        whenever(dataEnvelope.decrypt(any<String>(), any(), any())).thenReturn(unencrypted.toString().toByteArray())
        whenever(networkSession.loadData(any(), any())).thenReturn(NetworkResponse(200, "some data"))

        // Verify that the payload was successfully decrypted, and parsed.
        val identity = client.refreshIdentity(refreshToken, TestData.REFRESH_TOKEN_ENCRYPTED_SUCCESS_KEY)
        assertEquals(IdentityStatus.REFRESHED, identity.status)

        // Verify that the returned package has an identity that matches what we included in the body of the response.
        val expectedIdentity = unencrypted.getJSONObject("body").let {
            UID2Identity.fromJson(it)
        }
        assertEquals(expectedIdentity, identity.identity)
    }

    //endregion

    @Test
    fun `test successful opt-out`() = runBlocking {
        val client = withClient()

        // Configure the network session to return a valid payload.
        val unencrypted = JSONObject(TestData.REFRESH_TOKEN_OPT_OUT_DECRYPTED)
        whenever(dataEnvelope.decrypt(any<String>(), any(), any())).thenReturn(unencrypted.toString().toByteArray())
        whenever(networkSession.loadData(any(), any())).thenReturn(NetworkResponse(200, "some data"))

        // Verify that the payload was successfully decrypted, and parsed.
        val identity = client.refreshIdentity(refreshToken, TestData.REFRESH_TOKEN_ENCRYPTED_OPT_OUT_KEY)
        assertEquals(IdentityStatus.OPT_OUT, identity.status)
        assertNull(identity.identity)
    }

    @Test
    fun `test version info`() = runBlocking {
        val client = withClient()

        // Configure the network session to return a valid (encrypted) payload and allows us to capture the given
        // NetworkRequest.
        var networkRequest: NetworkRequest? = null
        val unencrypted = JSONObject(TestData.REFRESH_TOKEN_SUCCESS_DECRYPTED)
        whenever(dataEnvelope.decrypt(any<String>(), any(), any())).thenReturn(unencrypted.toString().toByteArray())
        whenever(networkSession.loadData(any(), any())).thenAnswer {
            networkRequest = it.arguments[1] as NetworkRequest?
            return@thenAnswer NetworkResponse(200, "some data")
        }

        // Ask the Client to refresh the Identity, so it can make the appropriate request.
        client.refreshIdentity(refreshToken, TestData.REFRESH_TOKEN_ENCRYPTED_SUCCESS_KEY)

        // Verify that the Client included the expected Version header, and that it ended with our SDK version.
        val reportedVersion = networkRequest?.headers?.get("X-UID2-Client-Version")
        assertNotNull(reportedVersion)
        assertTrue(reportedVersion?.endsWith(UID2.getVersion()) == true)
    }

    /**
     * Helper function to test that the given callback will result in the correct exception when the [UID2Client] is
     * configured with an invalid API URL.
     */
    private fun testInvalidClientApi(callback: suspend (client: UID2Client) -> Unit) {
        val client = UID2Client(
            "this is not a url",
            networkSession,
            packageName,
            dataEnvelope,
            timeUtils,
            keyUtils,
            logger,
        )

        // Verify that when we have configured the client with an invalid URL, that it throws the appropriate exception
        // when we try to fetch the client details.
        assertThrows(InvalidApiUrlException::class.java) {
            runTest(testDispatcher) {
                callback(client)
            }
        }
    }

    /**
     * Helper function to test that a given callback will result in the correct exception when the [UID2Client]
     * experiences a network failure.
     */
    private fun testNetworkFailure(callback: suspend (client: UID2Client) -> Unit) {
        val client = withClient()

        // Configure the network session to report a failure.
        whenever(networkSession.loadData(any(), any())).thenReturn(NetworkResponse(400))

        // Verify that when a network failure occurs, the appropriate exception is thrown.
        assertThrows(RequestFailureException::class.java) {
            runTest(testDispatcher) {
                callback(client)
            }
        }
    }

    /**
     * Helper function to test that a given callback will result in the correct exception when the [UID2Client]
     * receives an unexpected response.
     */
    private fun testInvalidNetworkResponse(callback: suspend (client: UID2Client) -> Unit) {
        val client = withClient()

        // Configure the network session to return an invalid response.
        whenever(networkSession.loadData(any(), any())).thenReturn(
            NetworkResponse(200, "This is not encrypted"),
        )

        // Verify that when an unexpected response is received, the appropriate exception is thrown.
        assertThrows(PayloadDecryptException::class.java) {
            runTest(testDispatcher) {
                callback(client)
            }
        }
    }

    private fun withClient() = UID2Client(
        url,
        networkSession,
        packageName,
        dataEnvelope,
        timeUtils,
        keyUtils,
        logger,
    )
}
