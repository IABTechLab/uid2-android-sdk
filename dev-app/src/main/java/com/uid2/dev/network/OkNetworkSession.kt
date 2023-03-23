package com.uid2.dev.network

import android.util.Log
import com.uid2.network.NetworkRequest
import com.uid2.network.NetworkResponse
import com.uid2.network.NetworkSession
import java.net.URL
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody

/**
 * An example of a custom NetworkSession, which internally is using OkHttp.
 */
class OkNetworkSession : NetworkSession {
    private val client = OkHttpClient()

    override fun loadData(url: URL, request: NetworkRequest): NetworkResponse {
        Log.i(TAG, "Requested URL: $url")

        val builder = Request.Builder()
            .url(url)

        // If we've been given any request headers, add them to the request.
        request.headers.forEach {
            builder.addHeader(it.key, it.value)
        }

        // If data was provided, this will require us to write it to the output stream.
        request.data?.let { data -> builder.post(data.toRequestBody()) }

        // A successful response code should be in the [200-299] range.
        val response = client.newCall(builder.build()).execute()
        if (!response.isSuccessful) {
            Log.e(TAG, "Error Received: ${response.code}")
            return NetworkResponse(response.code)
        }

        // We expect the response to be a String.
        Log.i(TAG, "Request Successful: ${response.code}")
        return NetworkResponse(response.code, response.body?.string() ?: "")
    }

    private companion object {
        const val TAG = "OkNetworkSession"
    }
}
