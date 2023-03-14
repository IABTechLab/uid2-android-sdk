package com.uid2.sdk.network

import com.uid2.sdk.extensions.decodeJsonToMap
import com.uid2.sdk.network.NetworkRequestType.GET
import com.uid2.sdk.network.NetworkRequestType.POST
import java.io.ByteArrayInputStream
import java.io.OutputStream
import java.net.HttpURLConnection
import java.net.URL
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.junit.MockitoJUnitRunner
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@RunWith(MockitoJUnitRunner::class)
class DefaultNetworkSessionTest {
    private val url = URL("https://test.com/path")
    private val connection: HttpURLConnection = mock()
    private val outputStream: OutputStream = mock()

    @Before
    fun before() {
        whenever(connection.outputStream).thenReturn(outputStream)
    }

    @Test
    fun `test request methods`() {
        val session = buildNetworkSession()

        session.loadData(url, NetworkRequest(GET))
        verify(connection).requestMethod = "GET"

        session.loadData(url, NetworkRequest(POST))
        verify(connection).requestMethod = "POST"
    }

    @Test
    fun `test request headers`() {
        val session = buildNetworkSession()

        val headers = mapOf(
            "Key1" to "Value1",
            "Key2" to "Value2",
            "Key3" to "Value3",
        )

        session.loadData(url, NetworkRequest(GET, headers))

        // Verify that for each of the request headers we provided, they were correctly added to the
        // connection.
        headers.forEach {
            verify(connection).addRequestProperty(it.key, it.value)
        }
    }

    @Test
    fun `test post data`() {
        val session = buildNetworkSession()

        val data = "Key1=Value1&Key2=2"
        session.loadData(url, NetworkRequest(POST, mapOf(), data))

        // We'll build the expected String representation of the map ourselves, but check that the
        // expected bytes have been written to the output stream. We also want to make sure that
        // the output stream was correctly closed afterwards.
        val expectedData = data.toByteArray(Charsets.UTF_8)
        verify(outputStream).write(expectedData)
        verify(outputStream).close()
    }

    @Test
    fun `test request failure`() {
        whenever(connection.responseCode).thenReturn(HttpURLConnection.HTTP_UNAUTHORIZED)

        val session = buildNetworkSession()
        val response = session.loadData(url, NetworkRequest(GET))

        // Verify that an empty map has been returned.
        assertEquals(HttpURLConnection.HTTP_UNAUTHORIZED, response.code)
        assertEquals("", response.data)
    }

    @Test
    fun `test request success`() {
        whenever(connection.responseCode).thenReturn(HttpURLConnection.HTTP_OK)

        val map = mapOf(
            "Key1" to "Value1",
            "Key2" to "Value2",
            "Key3" to "Value3",
        )

        val jsonString = JSONObject(map).toString(0)

        // Build a fake input stream that will return our pre-created JSON string.
        val inputStream = ByteArrayInputStream(jsonString.toByteArray(Charsets.UTF_8))
        whenever(connection.inputStream).thenReturn(inputStream)

        val session = buildNetworkSession()
        val response = session.loadData(url, NetworkRequest(GET))

        // Verify that the parsed response matches that returned by our fake input stream.
        assertEquals(HttpURLConnection.HTTP_OK, response.code)
        assertEquals(map, response.data.decodeJsonToMap())
    }

    /**
     * Creates a test instance of the DefaultNetworkSession. This instance will leverage our mocked
     * HttpURLConnection, to allow us more control over its interaction with the instance being
     * tested.
     */
    private fun buildNetworkSession() = object : DefaultNetworkSession() {
        override fun openConnection(url: URL) = connection
    }
}
