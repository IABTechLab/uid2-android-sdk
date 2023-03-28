package com.uid2.network

import java.net.URL

/**
 * The type of request that needs to be made. We currently only require either a GET or a POST.
 */
enum class NetworkRequestType {

    /**
     * A HTTP GET request.
     */
    GET,

    /**
     * A HTTP POST request (which should then include data within the body).
     */
    POST
}

/**
 * A class which represents a network request. This could include a number of headers to be used within the request,
 * along with if the request is a POST, some additional data which needs to be written to the connection.
 *
 * @param type The type of request required.
 * @param headers The collection of headers to be used in the request (in key/value pairs).
 * @param data The optional body data, used in a [NetworkRequestType.POST].
 */
data class NetworkRequest(
    val type: NetworkRequestType,
    val headers: Map<String, String> = mapOf(),
    val data: String? = null
)

/**
 * A class which represents a network response. This will include the HTTP status code, as well as any response data.
 *
 * @param code The HTTP response code received after attempting to make the request.
 * @param data The body data contained within the response. If none is available, the empty string should be provided.
 */
data class NetworkResponse(
    val code: Int,
    val data: String = ""
)

/**
 * This interface controls all network access. A default implementation will be included within the library, but
 * consumers can also choose to implement it to leverage their own networking code (e.g. OkHttp).
 */
interface NetworkSession {

    /**
     * Requests the given URL with the details provided in the request.
     *
     * @param url The [URL] endpoint associated with the request.
     * @param request The details of the required request.
     * @return The [NetworkResponse] which contains the outcome of the attempted request.
     */
    fun loadData(url: URL, request: NetworkRequest): NetworkResponse
}
