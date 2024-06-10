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
import io.mockk.every
import io.mockk.junit4.MockKRule
import io.mockk.mockk
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
import org.junit.Rule
import org.junit.Test
import java.security.KeyPair
import java.security.PublicKey
import javax.crypto.SecretKey

class UID2ClientTest {
    @get:Rule
    val mockkRule = MockKRule(this)

    private val testDispatcher: TestDispatcher = StandardTestDispatcher()

    private val networkSession = mockk<NetworkSession>()
    private val packageName = "com.uid2.devapp"
    private val dataEnvelope = mockk<DataEnvelope>(relaxed = true)
    private val keyUtils = mockk<KeyUtils>()
    private val timeUtils = mockk<TimeUtils>()
    private val logger = mockk<Logger>(relaxed = true)

    private val url = "https://test.dev"
    private val refreshToken = "RefreshToken"
    private val refreshKey = "RefreshKey"

    private val keyPair = mockk<KeyPair>()
    private val keyPairPublic = mockk<PublicKey>()
    private val keyPairPublicEncoded = ByteArray(12)

    private val SUBSCRIPTION_ID = "subscription_id"
    private val PUBLIC_KEY = "public_key"

    @Before
    fun before() {
        // By default, don't encrypt the data. Just convert it directly to a ByteArray.
        every { dataEnvelope.encrypt(any(), any(), any(), any()) }.answers {
            (secondArg() as String).toByteArray()
        }

        every { dataEnvelope.decrypt(any<String>(), any<String>(), any<Boolean>()) }.returns(null)

        every { keyUtils.generateServerPublicKey(any()) }.returns(mockk<PublicKey>())
        every { keyUtils.generateKeyPair() }.returns(keyPair)
        every { keyUtils.generateSharedSecret(any(), any()) }.returns(mockk<SecretKey>(relaxed = true))
        every { keyUtils.generateIv(any()) }.answers { ByteArray(firstArg() as Int) }
        every { keyUtils.generateAad(any(), any()) }.returns("")

        every { keyPairPublic.encoded }.returns(keyPairPublicEncoded)
        every { keyPair.public }.returns(keyPairPublic)

        every { timeUtils.now() }.returns(0)
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
        every { keyUtils.generateServerPublicKey(any()) }.returns(null)

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
        every { keyUtils.generateKeyPair() }.returns(null)

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
        every { keyUtils.generateSharedSecret(any(), any()) }.returns(null)

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
        every { dataEnvelope.encrypt(any(), any(), any(), any()) }.returns(null)

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
        every { networkSession.loadData(any(), any()) }.returns(NetworkResponse(200, "somedata"))
        every { dataEnvelope.decrypt(any<ByteArray>(), any<String>(), any<Boolean>()) }.returns(null)

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
        every { dataEnvelope.decrypt(any<ByteArray>(), any<String>(), any<Boolean>()) }.returns(
            unencrypted.toString().toByteArray(),
        )
        every { networkSession.loadData(any(), any()) }.returns(NetworkResponse(200, "some data"))

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

        every { dataEnvelope.decrypt(any<String>(), any(), any()) }.returns(null)
        every { networkSession.loadData(any(), any()) }.returns(NetworkResponse(200, "some data"))

        // Verify that when an unexpected response is returned, the appropriate exception is thrown.
        assertThrows(PayloadDecryptException::class.java) {
            runBlocking { client.refreshIdentity(refreshToken, "This is not a key") }
        }
    }

    @Test
    fun `test successful refresh`() = runTest(testDispatcher) {
        val client = withClient()

        val unencrypted = JSONObject(TestData.REFRESH_TOKEN_SUCCESS_DECRYPTED)
        every { dataEnvelope.decrypt(any<String>(), any(), any()) }.returns(unencrypted.toString().toByteArray())
        every { networkSession.loadData(any(), any()) }.returns(NetworkResponse(200, "some data"))

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
        every { dataEnvelope.decrypt(any<String>(), any(), any()) }.returns(unencrypted.toString().toByteArray())
        every { networkSession.loadData(any(), any()) }.returns(NetworkResponse(200, "some data"))

        // Verify that the payload was successfully decrypted, and parsed.
        val identity = client.refreshIdentity(refreshToken, TestData.REFRESH_TOKEN_ENCRYPTED_OPT_OUT_KEY)
        assertEquals(IdentityStatus.OPT_OUT, identity.status)
        assertNull(identity.identity)
    }

    @Test
    fun `test version info - generate identity`() {
        testVersionInfo { client ->
            val unencrypted = JSONObject(TestData.REFRESH_TOKEN_SUCCESS_DECRYPTED)
            every { dataEnvelope.decrypt(any<ByteArray>(), any(), any()) }.returns(
                unencrypted.toString().toByteArray(),
            )

            // Ask the Client to generate a new Identity, so it can make the appropriate request.
            client.generateIdentity(
                IdentityRequest.Email("test@test.com"),
                SUBSCRIPTION_ID,
                PUBLIC_KEY,
            )
        }
    }

    @Test
    fun `test version info - refresh identity`() {
        every { dataEnvelope.decrypt(any<String>(), any(), any()) }.returns(
            TestData.REFRESH_TOKEN_SUCCESS_DECRYPTED.toByteArray(),
        )

        testVersionInfo { client ->
            // Ask the Client to refresh the Identity, so it can make the appropriate request.
            client.refreshIdentity(refreshToken, TestData.REFRESH_TOKEN_ENCRYPTED_SUCCESS_KEY)
        }
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
        every { networkSession.loadData(any(), any()) }.returns(NetworkResponse(400))

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
        every { networkSession.loadData(any(), any()) }.returns(
            NetworkResponse(200, "This is not encrypted"),
        )

        // Verify that when an unexpected response is received, the appropriate exception is thrown.
        assertThrows(PayloadDecryptException::class.java) {
            runTest(testDispatcher) {
                callback(client)
            }
        }
    }

    /**
     * Helper function to test whether the expected version headers are included in a specific request, with the
     * request being invoked via a given callback.
     */
    private fun testVersionInfo(callback: suspend (client: UID2Client) -> Unit) = runTest(testDispatcher) {
        val client = withClient()

        // Configure the network session to return a valid (encrypted) payload and allows us to capture the given
        // NetworkRequest.
        var networkRequest: NetworkRequest? = null
        every { networkSession.loadData(any(), any()) }.answers {
            networkRequest = secondArg() as NetworkRequest?
            NetworkResponse(200, "some data")
        }

        callback(client)

        // Verify that the Client included the expected Version header, and that it ended with our SDK version.
        val reportedVersion = networkRequest?.headers?.get("X-UID2-Client-Version")
        assertNotNull(reportedVersion)
        assertTrue(reportedVersion?.endsWith(UID2.getVersion()) == true)
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
