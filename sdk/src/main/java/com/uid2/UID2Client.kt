package com.uid2

import com.uid2.data.UID2Identity
import com.uid2.extensions.encodeBase64NonURLSafe
import com.uid2.extensions.encodeBase64URLSafe
import com.uid2.network.DataEnvelope
import com.uid2.network.NetworkRequest
import com.uid2.network.NetworkRequestType
import com.uid2.network.NetworkSession
import com.uid2.network.RefreshPackage
import com.uid2.network.RefreshResponse
import com.uid2.utils.KeyUtils
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
    private val apiPublicKey: String?,
    private val apiSubscriptionId: String?,
    private val session: NetworkSession,
    private val packageName: String,
    private val timeUtils: TimeUtils = TimeUtils(),
    private val keyUtils: KeyUtils = KeyUtils(),
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
    private val paramsFactory: (Map<String, String>) -> String = { params ->
        JSONObject().apply {
            params.forEach { (key, value) -> put(key, value) }
        }.toString()
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
        GenerateTokenException::class,
        PayloadDecryptException::class,
        InvalidPayloadException::class,
    )
    suspend fun generateIdentity(emailHash: String): UID2Identity? = withContext(ioDispatcher) {
        // If the SDK hasn't been configured with the required client side token generation parameters, then it's not
        // possible for us to generate the requested (new) identity.
        if (apiPublicKey == null || apiSubscriptionId == null) {
            return@withContext null
        }

        // Check to make sure we have a valid endpoint to hit.
        val url = apiGenerateUrl ?: throw InvalidApiUrlException()

        // Generate the required Server and Client keys.
        val serverPublicKey = keyUtils.generateServerPublicKey(apiPublicKey)
        val clientKeyPair = keyUtils.generateKeyPair()
        if (serverPublicKey == null || clientKeyPair == null) {
            throw CryptoException()
        }

        // Generate our temporary shared secret.
        val sharedSecret = keyUtils.generateSharedSecret(serverPublicKey, clientKeyPair) ?: throw CryptoException()

        val iv = keyUtils.generateIv(IV_LENGTH_BYTES)
        val now = timeUtils.now()
        val aad = keyUtils.generateAad(now)

        // For now, we're going to assume a single type of input.
        val payload = paramsFactory(mapOf("email_hash" to emailHash))
        val encryptedPayload = DataEnvelope.encrypt(sharedSecret, payload, iv, aad.toByteArray())
            ?: throw CryptoException()

        // Build the request to generate the token.
        val request = NetworkRequest(
            NetworkRequestType.POST,
            mapOf(),
            paramsFactory(
                mapOf(
                    "payload" to encryptedPayload.encodeBase64NonURLSafe(),
                    "iv" to iv.encodeBase64NonURLSafe(),
                    "public_key" to clientKeyPair.public.encoded.encodeBase64NonURLSafe(),
                    "timestamp" to now.toString(),
                    "subscription_id" to apiSubscriptionId,
                    "app_name" to packageName,
                ),
            ),
        )

        // Attempt to make the request via the provided NetworkSession.
        val response = session.loadData(url, request)
        if (response.code != HttpURLConnection.HTTP_OK) {
            throw GenerateTokenException(response.code)
        }

        // The response should be an encrypted payload. Let's attempt to decrypt it using the key we were provided.
        val envelope = DataEnvelope.decrypt(sharedSecret.encoded, response.data, false)
            ?: throw PayloadDecryptException()

        // The decrypted payload should be JSON which we can parse.
        val generateResponse = RefreshResponse.fromJson(JSONObject(String(envelope, Charsets.UTF_8)))
        return@withContext generateResponse?.toGeneratedIdentity() ?: throw InvalidPayloadException()
    }

    @Throws(
        InvalidApiUrlException::class,
        RefreshTokenException::class,
        PayloadDecryptException::class,
        InvalidPayloadException::class,
    )
    suspend fun refreshIdentity(
        refreshToken: String,
        refreshResponseKey: String,
    ): RefreshPackage = withContext(ioDispatcher) {
        // Check to make sure we have a valid endpoint to hit.
        val url = apiRefreshUrl ?: throw InvalidApiUrlException()

        // Build the request to refresh the token.
        val request = NetworkRequest(
            NetworkRequestType.POST,
            mapOf(
                "X-UID2-Client-Version" to clientVersion,
                "Content-Type" to "application/x-www-form-urlencoded",
            ),
            refreshToken,
        )

        // Attempt to make the request via the provided NetworkSession.
        val response = session.loadData(url, request)
        if (response.code != HttpURLConnection.HTTP_OK) {
            throw RefreshTokenException(response.code)
        }

        // The response should be an encrypted payload. Let's attempt to decrypt it using the key we were provided.
        val payload = DataEnvelope.decrypt(refreshResponseKey, response.data, true)
            ?: throw PayloadDecryptException()

        // The decrypted payload should be JSON which we can parse.
        val refreshResponse = RefreshResponse.fromJson(JSONObject(String(payload, Charsets.UTF_8)))
        return@withContext refreshResponse?.toRefreshPackage() ?: throw InvalidPayloadException()
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
        // The relative path of the API's generate endpoint
        const val API_GENERATE_PATH = "/v2/token/client-generate"

        // The relative path of the API's refresh endpoint
        const val API_REFRESH_PATH = "/v2/token/refresh"

        // The length, in bytes, of the IV used when generating an identity.
        const val IV_LENGTH_BYTES = 12
    }
}
