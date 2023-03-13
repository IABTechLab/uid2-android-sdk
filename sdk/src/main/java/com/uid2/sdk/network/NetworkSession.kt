package com.uid2.sdk.network

import java.net.URL

/**
 * The type of request that needs to be made. We currently only require either a GET or a POST.
 */
enum class NetworkRequestType {
    GET,
    POST
}

/**
 * A class which represents a network request. This could include a number of headers to be used
 * within the request, along with if the request is a POST, some additional data which needs to be
 * written to the connection.
 */
data class NetworkRequest(
    val type: NetworkRequestType,
    val headers: Map<String, String> = mapOf(),
    val data: Map<String, Any>? = null
)

/**
 * This interface controls all network access. A default implementation will be included within the
 * library, but consumers can also choose to implement it to leverage their own networking code
 * (e.g. OkHttp).
 */
interface NetworkSession {

    /**
     * Requests the given URL with the details provided in the request. Any response data will be
     * returned in a map of key/value pairs.
     */
    fun loadData(url: URL, request: NetworkRequest): Map<String, Any>
}
