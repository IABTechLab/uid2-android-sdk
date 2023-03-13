package com.uid2.sdk.network

import com.uid2.sdk.extensions.decodeJsonToMap
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

/**
 * A default implementation of NetworkSession that leverages HttpUrlConnection to make the necessary
 * GET and POST requests.
 */
open class DefaultNetworkSession : NetworkSession {

    override fun loadData(url: URL, request: NetworkRequest): Map<String, Any> {
        val connection = openConnection(url).apply {
            requestMethod = request.type.toRequestMethod()

            setRequestProperty("Accept", "application/json")

            // If we've been given any request headers, add them to the request.
            request.headers.forEach {
                addRequestProperty(it.key, it.value)
            }

            // If data was provided, this will require us to write it to the output stream.
            request.data?.let { data ->
                doOutput = true

                outputStream.use { outputStream ->
                    outputStream.write(data.toPostData().toByteArray(Charsets.UTF_8))
                }
            }
        }

        // A successful response code should be in the [200-299] range.
        val responseCode = connection.responseCode
        if (responseCode !in SUCCESS_CODE_RANGE_MIN..SUCCESS_CODE_RANGE_MAX) {
            return mapOf()
        }

        // Read the response and attempt to convert the returned JSON into a map.
        val responseText = connection.inputStream.bufferedReader().use { it.readText() }
        return responseText.decodeJsonToMap() ?: mapOf()
    }

    /**
     * Opens the given URL to return the HttpURLConnection.
     *
     * This is done via an open method, to allow better testability, given that the URL class is
     * final and therefore hard to mock itself.
     */
    open fun openConnection(url: URL) = (url.openConnection() as HttpURLConnection)

    /**
     * Extension to convert the given NetworkRequestType into the expected request method name.
     */
    private fun NetworkRequestType.toRequestMethod() = when (this) {
        NetworkRequestType.GET -> "GET"
        NetworkRequestType.POST -> "POST"
    }

    /**
     * Extension to format any associated POST data into to expected format (key=value pairs, joined
     * with "&").
     */
    private fun Map<String, Any>.toPostData() = keys.joinToString("&") {
        val charset = Charsets.UTF_8.name()
        URLEncoder.encode(it, charset) + "=" + URLEncoder.encode(get(it).toString(), charset)
    }

    private companion object {
        const val SUCCESS_CODE_RANGE_MIN = HttpURLConnection.HTTP_OK
        const val SUCCESS_CODE_RANGE_MAX = 299
    }
}
