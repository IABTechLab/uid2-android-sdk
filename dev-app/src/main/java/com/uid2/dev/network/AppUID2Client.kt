package com.uid2.dev.network

import android.content.Context
import android.util.Base64
import com.uid2.data.UID2Identity
import com.uid2.dev.utils.getMetadata
import com.uid2.network.DataEnvelope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.net.URI
import java.net.URL
import java.nio.ByteBuffer
import java.nio.ByteOrder
import javax.crypto.Cipher
import javax.crypto.spec.SecretKeySpec
import kotlin.random.Random

/**
 * The consuming application is responsible for generating the initial Identity, then passing that to the UID2Manager
 * to be periodically refreshed. This class provides the Development Application a simple way to generate that starting
 * identity.
 */
class AppUID2Client(
    private val apiUrl: String,
    private val key: String,
    private val secret: String,
) {
    private val client = OkHttpClient()

    private val apiGenerateUrl: URL? by lazy {
        runCatching {
            URL(
                URI(apiUrl)
                    .resolve(API_GENERATE_PATH)
                    .toString(),
            )
        }.getOrNull()
    }

    /**
     * Attempts to generate a UID2Identity from a given Request type. For example, this could be from a validated email
     * address, or telephone number.
     */
    @Throws(AppUID2ClientException::class)
    suspend fun generateIdentity(requestString: String, type: RequestType): UID2Identity? =
        withContext(Dispatchers.IO) {
            // Check to make sure we have a valid endpoint to hit.
            val url = apiGenerateUrl ?: throw AppUID2ClientException(ERROR_UNKNOWN_API)

            // Check that the key and secret were provided.
            if (key.isEmpty() || secret.isEmpty()) {
                throw AppUID2ClientException(ERROR_NO_SECRET_OR_KEY)
            }

            // The secret should be Base64 encoded. Let's decode it and verify that what we have appears valid.
            val secretBytes = runCatching { Base64.decode(secret, Base64.DEFAULT) }.getOrNull()
                ?: throw AppUID2ClientException(ERROR_UNABLE_TO_DECODE_SECRET)

            // The request will contain an encrypted payload which contains the verified identity of the user.
            val requestBody = encryptRequest(
                secretBytes,
                mapOf(type.parameter to requestString),
            ) ?: throw AppUID2ClientException(ERROR_UNABLE_TO_ENCRYPT_REQUEST)

            val request = Request.Builder()
                .url(url)
                .addHeader(REQUEST_HEADER_AUTHORIZATION, REQUEST_HEADER_BEARER + key)
                .addHeader(REQUEST_HEADER_CONTENT_TYPE, REQUEST_HEADER_TEXT_PLAIN)
                .post(requestBody.toRequestBody())
                .build()

            // Make the request and verify that it was successful.
            val response = client.newCall(request).execute()
            if (!response.isSuccessful) {
                throw AppUID2ClientException(ERROR_REQUEST_FAILED + response.code)
            }

            // If we have a valid response, we can try to decrypt it.
            val responseBody = decryptResponse(
                secret,
                response.body?.string()
                    ?: throw AppUID2ClientException(ERROR_RESPONSE_NO_BODY),
            ) ?: throw AppUID2ClientException(ERROR_UNABLE_TO_DECRYPT_RESPONSE)

            // Now try to parse the decrypted response (as JSON).
            val responseToken = runCatching {
                GenerateTokenResponse.fromJson(JSONObject(responseBody))
            }.getOrNull() ?: throw AppUID2ClientException(ERROR_UNABLE_TO_PARSE_RESPONSE)

            // After all that, we should finally have a valid UID2Identity!
            return@withContext responseToken.body
        }

    private fun encryptRequest(key: ByteArray, params: Map<String, String>): ByteArray? {
        // The body of the payload is expected to be the following:
        //  - Current time since epoch (in ms)
        //  - Nonce (8 bytes)
        //  - The payload, in JSON format.
        val body = ByteArrayOutputStream().let {
            // Start by adding the current time (in ms).
            val nowMs = System.currentTimeMillis()
            val nowBytes = nowMs.toByteArray()
            it.write(nowBytes)

            // Then append a random initialization vector
            val nonce = Random.nextBytes(NONCE_SIZE_BYTES)
            it.write(nonce)

            // Attempt to convert the given map into it's JSON representation.
            val json = runCatching { JSONObject(params).toString() + "\n" }.getOrNull() ?: return null
            it.write(json.toByteArray(Charsets.UTF_8))

            return@let it.toByteArray()
        }

        // If the payload is empty, an error occurred. We are therefore unable to generate the request.
        if (body.isEmpty()) {
            return null
        }

        // Initialise the appropriate AES Cipher.
        val cipher = runCatching {
            Cipher.getInstance(ALGORITHM_TRANSFORMATION)?.apply {
                init(Cipher.ENCRYPT_MODE, SecretKeySpec(key, ALGORITHM_NAME))
            }
        }.getOrNull() ?: return null

        // Encrypt the body and generate the required payload.
        val encryptedBody = cipher.doFinal(body)
        val encryptedEnvelope = byteArrayOf(0x01) + cipher.iv + encryptedBody

        // Return the Base64 representation of the envelope.
        return Base64.encode(encryptedEnvelope, Base64.NO_WRAP)
    }

    private fun decryptResponse(key: String, data: String) =
        DataEnvelope.decrypt(key, data, true)?.toString(Charsets.UTF_8)

    private fun Long.toByteArray() = ByteBuffer.allocate(Long.SIZE_BYTES).apply {
        order(ByteOrder.BIG_ENDIAN)
        putLong(this@toByteArray)
    }.array()

    companion object {
        private const val UID2_API_URL_KEY = "uid2_api_url"
        private const val UID2_API_URL_DEFAULT = "https://operator-integ.uidapi.com"

        private const val UID2_API_KEY_KEY = "uid2_api_key"
        private const val UID2_API_SECRET_KEY = "uid2_api_secret"

        // The relative path of the API's generate endpoint
        private const val API_GENERATE_PATH = "/v2/token/generate"

        private const val ALGORITHM_NAME = "AES"
        private const val ALGORITHM_TRANSFORMATION = "AES/GCM/NoPadding"
        private const val NONCE_SIZE_BYTES = 8

        private const val REQUEST_HEADER_CONTENT_TYPE = "Content-Type"
        private const val REQUEST_HEADER_TEXT_PLAIN = "text/plain"
        private const val REQUEST_HEADER_AUTHORIZATION = "Authorization"
        private const val REQUEST_HEADER_BEARER = "Bearer "

        private const val ERROR_UNKNOWN_API = "Unable to determine API endpoint"
        private const val ERROR_NO_SECRET_OR_KEY = "No Key or Secret provided"
        private const val ERROR_UNABLE_TO_DECODE_SECRET =
            "Unable to decode the Base64 Secret, are you sure it's entered correctly?"
        private const val ERROR_UNABLE_TO_ENCRYPT_REQUEST = "Unable to encrypt request"
        private const val ERROR_REQUEST_FAILED = "Request Failed: "
        private const val ERROR_RESPONSE_NO_BODY = "Response didn't include any body"
        private const val ERROR_UNABLE_TO_DECRYPT_RESPONSE = "Unable to decrypt response"
        private const val ERROR_UNABLE_TO_PARSE_RESPONSE = "Unable to parse decrypted response"

        /**
         * Builds a AppUID2Client from a given context.
         *
         * This is required because the Context may contain additional metadata required to configure how we access the
         * API.
         */
        fun fromContext(context: Context): AppUID2Client = context.getMetadata().let {
            return@let AppUID2Client(
                it.getString(UID2_API_URL_KEY, UID2_API_URL_DEFAULT),
                it.getString(UID2_API_KEY_KEY, ""),
                it.getString(UID2_API_SECRET_KEY, ""),
            )
        }
    }
}
