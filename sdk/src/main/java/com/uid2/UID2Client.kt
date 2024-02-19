package com.uid2

import com.uid2.data.IdentityRequest
import com.uid2.data.toPayload
import com.uid2.extensions.encodeBase64
import com.uid2.network.DataEnvelope
import com.uid2.network.NetworkRequest
import com.uid2.network.NetworkRequestType
import com.uid2.network.NetworkSession
import com.uid2.network.RefreshResponse
import com.uid2.network.ResponsePackage
import com.uid2.utils.KeyUtils
import com.uid2.utils.Logger
import com.uid2.utils.TimeUtils
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URI
import java.net.URL

/**
 * This class is responsible for refreshing the identity, using a provided refresh token. The payload response will be
 * encrypted, so also is provided a key to allow decryption.
 */
internal class UID2Client(
    private val apiUrl: String,
    private val session: NetworkSession,
    private val applicationId: String,
    private val dataEnvelope: DataEnvelope = DataEnvelope,
    private val timeUtils: TimeUtils = TimeUtils,
    private val keyUtils: KeyUtils = KeyUtils,
    private val logger: Logger = Logger(),
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
    private val paramsFactory: (Map<String, String>) -> String = { params ->
        JSONObject(params).toString()
    },
) {
    // The endpoints are built from the given API root, along with our known path appended. If the consumer has
    // incorrectly configured the SDK, it's possible this could be null.
    private val apiGenerateUrl: URL? by lazy { getApiUrl(API_GENERATE_PATH) }
    private val apiRefreshUrl: URL? by lazy { getApiUrl(API_REFRESH_PATH) }

    // We expect the Client to report a Version that is in the following format: Android-X.Y.Z
    private val clientVersion: String by lazy { "Android-${UID2.getVersion()}" }

    @Throws(
        InvalidApiUrlException::class,
        CryptoException::class,
        RequestFailureException::class,
        PayloadDecryptException::class,
        InvalidPayloadException::class,
    )
    suspend fun generateIdentity(
        identityRequest: IdentityRequest,
        subscriptionId: String,
        publicKey: String,
    ): ResponsePackage = withContext(ioDispatcher) {
        logger.i(TAG) { "Generating Identity" }

        // Check to make sure we have a valid endpoint to hit.
        val url = apiGenerateUrl ?: run {
            logger.e(TAG) { "Error determining identity generation API" }
            throw InvalidApiUrlException()
        }

        // Generate the required Server and Client keys.
        val serverPublicKey = keyUtils.generateServerPublicKey(publicKey)
        val clientKeyPair = keyUtils.generateKeyPair()
        if (serverPublicKey == null || clientKeyPair == null) {
            logger.e(TAG) { "Error generating server and client keys" }
            throw CryptoException()
        }

        // Generate our temporary shared secret.
        val sharedSecret = keyUtils.generateSharedSecret(serverPublicKey, clientKeyPair) ?: run {
            logger.e(TAG) { "Error generating temporary shared secret" }
            throw CryptoException()
        }

        val iv = keyUtils.generateIv(IV_LENGTH_BYTES)
        val now = timeUtils.now()
        val aad = keyUtils.generateAad(now, applicationId)

        // Build and encrypt the payload containing the identity generation request.
        val payload = identityRequest.toPayload()
        val encryptedPayload = dataEnvelope.encrypt(sharedSecret, payload, iv, aad.toByteArray()) ?: run {
            logger.e(TAG) { "Error encrypting payload" }
            throw CryptoException()
        }

        // Build the request to generate the token.
        val request = NetworkRequest(
            NetworkRequestType.POST,
            mapOf(
                HEADER_CLIENT_VERSION to clientVersion,
            ),
            paramsFactory(
                mapOf(
                    "payload" to encryptedPayload.encodeBase64(),
                    "iv" to iv.encodeBase64(),
                    "public_key" to clientKeyPair.public.encoded.encodeBase64(),
                    "timestamp" to now.toString(),
                    "subscription_id" to subscriptionId,
                    "app_name" to applicationId,
                ),
            ),
        )

        // Attempt to make the request via the provided NetworkSession.
        val response = session.loadData(url, request)
        if (response.code != HttpURLConnection.HTTP_OK) {
            logger.e(TAG) { "Client details failure: ${response.code}" }
            throw RequestFailureException(response.code)
        }

        // The response should be an encrypted payload. Let's attempt to decrypt it using the key we were provided.
        val envelope = dataEnvelope.decrypt(sharedSecret.encoded, response.data, false) ?: run {
            logger.e(TAG) { "Error decrypting response from client details" }
            throw PayloadDecryptException()
        }

        // The decrypted payload should be JSON which we can parse.
        val generateResponse = RefreshResponse.fromJson(JSONObject(String(envelope, Charsets.UTF_8)))
        return@withContext generateResponse?.toResponsePackage(false) ?: run {
            logger.e(TAG) { "Error parsing response from client details" }
            throw InvalidPayloadException()
        }
    }

    @Throws(
        InvalidApiUrlException::class,
        RequestFailureException::class,
        PayloadDecryptException::class,
        InvalidPayloadException::class,
    )
    suspend fun refreshIdentity(refreshToken: String, refreshResponseKey: String): ResponsePackage =
        withContext(ioDispatcher) {
            logger.i(TAG) { "Refreshing identity" }

            // Check to make sure we have a valid endpoint to hit.
            val url = apiRefreshUrl ?: run {
                logger.e(TAG) { "Error determining identity refresh API" }
                throw InvalidApiUrlException()
            }

            // Build the request to refresh the token.
            val request = NetworkRequest(
                NetworkRequestType.POST,
                mapOf(
                    HEADER_CLIENT_VERSION to clientVersion,
                    "Content-Type" to "application/x-www-form-urlencoded",
                ),
                refreshToken,
            )

            // Attempt to make the request via the provided NetworkSession.
            val response = session.loadData(url, request)
            if (response.code != HttpURLConnection.HTTP_OK) {
                logger.e(TAG) { "Client details failure: ${response.code}" }
                throw RequestFailureException(response.code)
            }

            // The response should be an encrypted payload. Let's attempt to decrypt it using the key we were provided.
            val payload = dataEnvelope.decrypt(refreshResponseKey, response.data, false) ?: run {
                logger.e(TAG) { "Error decrypting response from client details" }
                throw PayloadDecryptException()
            }

            // The decrypted payload should be JSON which we can parse.
            val refreshResponse = RefreshResponse.fromJson(JSONObject(String(payload, Charsets.UTF_8)))
            return@withContext refreshResponse?.toResponsePackage(true) ?: run {
                logger.e(TAG) { "Error parsing response from client details" }
                throw InvalidPayloadException()
            }
        }

    /**
     * Builds a [URL] for the configured API server with the given (relative) path.
     */
    private fun getApiUrl(path: String): URL? {
        return runCatching {
            URL(
                URI(apiUrl)
                    .resolve(path)
                    .toString(),
            )
        }.getOrNull()
    }

    private companion object {
        const val TAG = "UID2Client"

        // The relative path of the API's generate endpoint
        const val API_GENERATE_PATH = "/v2/token/client-generate"

        // The relative path of the API's refresh endpoint
        const val API_REFRESH_PATH = "/v2/token/refresh"

        // The header used to provide the client version.
        const val HEADER_CLIENT_VERSION = "X-UID2-Client-Version"

        // The length, in bytes, of the IV used when generating an identity.
        const val IV_LENGTH_BYTES = 12
    }
}
