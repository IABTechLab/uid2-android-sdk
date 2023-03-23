package com.uid2.network

import java.net.HttpURLConnection
import java.net.URL

/**
 * A default implementation of NetworkSession that leverages HttpUrlConnection to make the necessary
 * GET and POST requests.
 */
open class DefaultNetworkSession : NetworkSession {

    override fun loadData(url: URL, request: NetworkRequest): NetworkResponse {
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
                    outputStream.write(data.toByteArray(Charsets.UTF_8))
                }
            }
        }

        // A successful response code should be in the [200-299] range.
        val responseCode = connection.responseCode
        if (responseCode !in SUCCESS_CODE_RANGE_MIN..SUCCESS_CODE_RANGE_MAX) {
            return NetworkResponse(responseCode)
        }

        // We expect the response to be a String.
        val responseText = connection.inputStream.bufferedReader().use { it.readText() }
        return NetworkResponse(responseCode, responseText)
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

    private companion object {
        const val SUCCESS_CODE_RANGE_MIN = HttpURLConnection.HTTP_OK
        const val SUCCESS_CODE_RANGE_MAX = 299
    }
}
