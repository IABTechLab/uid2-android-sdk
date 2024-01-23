package com.uid2

import com.uid2.network.DataEnvelope
import com.uid2.network.NetworkRequest
import com.uid2.network.NetworkRequestType
import com.uid2.network.NetworkSession
import com.uid2.network.RefreshPackage
import com.uid2.network.RefreshResponse
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
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) {
    // The refresh endpoint is built from the given API root, along with our known refresh path appended. If the
    // consumer has incorrectly configured the SDK, it's possible this could be null.
    private val apiRefreshUrl: URL? by lazy {
        runCatching {
            URL(
                URI(apiUrl)
                    .resolve(API_REFRESH_PATH)
                    .toString(),
            )
        }.getOrNull()
    }

    private val apiCstgUrl: URL? by lazy {
        runCatching {
            URL(
                URI("http://localhost:8080/")
                    .resolve(API_CSTG_PATH)
                    .toString(),
            )
        }.getOrNull()
    }


    // We expect the Client to report a Version that is in the following format: Android-X.Y.Z
    private val clientVersion: String by lazy { "Android-${UID2.getVersion()}" }

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

    suspend fun cstg(
        requestBody: String,
    ) = withContext(ioDispatcher) {
        // Check to make sure we have a valid endpoint to hit.
        val url = apiCstgUrl ?: throw InvalidApiUrlException()

        val request = NetworkRequest(
            NetworkRequestType.POST,
            mapOf(
                "Origin" to "DemoApp",
//                "Content-Type" to "application/x-www-form-urlencoded",
            ),
            requestBody
        )

        // Attempt to make the request via the provided NetworkSession.
        val response = session.loadData(url, request)
        if (response.code != HttpURLConnection.HTTP_OK) {
            throw RefreshTokenException(response.code)
        }

        // The response should be an encrypted payload. Let's attempt to decrypt it using the key we were provided.
//        val payload = DataEnvelope.decrypt(refreshResponseKey, response.data, true)
//            ?: throw PayloadDecryptException()
//
//        // The decrypted payload should be JSON which we can parse.
//        val refreshResponse = RefreshResponse.fromJson(JSONObject(String(payload, Charsets.UTF_8)))
//        return@withContext refreshResponse?.toRefreshPackage() ?: throw InvalidPayloadException()
        return@withContext "Hello"
    }

    private companion object {
        // The relative path of the API's refresh endpoint
        const val API_REFRESH_PATH = "/v2/token/refresh"
        const val API_CSTG_PATH = "/v2/token/client-generate"
    }
}
