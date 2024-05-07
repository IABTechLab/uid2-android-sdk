package com.uid2.network

import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

/**
 * A default implementation of [NetworkSession] that leverages [HttpURLConnection] to make the necessary
 * GET and POST requests.
 *
 * If a consuming application wants to take control over the network requests, they can implement their own custom
 * [NetworkSession] and provide it when initialising the SDK via [com.uid2.UID2Manager.init]
 */
public open class DefaultNetworkSession : NetworkSession {

    /**
     * Loads the given [URL] and [NetworkRequest] using [HttpURLConnection].
     */
    override fun loadData(url: URL, request: NetworkRequest): NetworkResponse {
        try {
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

            // A successful response code should be in the [200-299] range. If we receive something outside that range, then
            // we should be reading from the errorStream rather than the standard inputStream.
            val responseCode = connection.responseCode
            val responseStream = if (NetworkSession.isSuccess(responseCode)) {
                connection.inputStream
            } else {
                connection.errorStream
            }

            // We expect the response to be a String.
            val responseText = runCatching {
                responseStream.bufferedReader().use { it.readText() }
            }.getOrDefault("")
            return NetworkResponse(responseCode, responseText)
        } catch (ex: IOException) {
            // If we're unable to make a request, e.g. due to lack of connection, we will simply report an internal
            // error.
            return NetworkResponse(HttpURLConnection.HTTP_INTERNAL_ERROR)
        }
    }

    /**
     * Opens the given URL to return the HttpURLConnection.
     *
     * This is done via an open method, to allow better testability, given that the URL class is
     * final and therefore hard to mock itself.
     */
    public open fun openConnection(url: URL): HttpURLConnection = (url.openConnection() as HttpURLConnection)

    /**
     * Extension to convert the given NetworkRequestType into the expected request method name.
     */
    private fun NetworkRequestType.toRequestMethod() = when (this) {
        NetworkRequestType.GET -> "GET"
        NetworkRequestType.POST -> "POST"
    }
}
