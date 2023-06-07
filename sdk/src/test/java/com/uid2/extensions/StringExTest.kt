package com.uid2.extensions

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.junit.MockitoJUnitRunner
import java.util.Base64

@RunWith(MockitoJUnitRunner::class)
class StringExTest {

    @Test
    fun `test parsing json to map`() {
        val map = mapOf(
            "Key1" to "Value1",
            "Key2" to "Value2",
            "Key3" to "Value3",
        )

        // Convert the initial map to a string representation.
        val jsonString = JSONObject(map).toString(0)

        // Attempt to decode the string back as JSON and convert to a map. We can then simply
        // compare it to the map we originally built.
        val parsedMap = jsonString.decodeJsonToMap()
        assertEquals(map, parsedMap)
    }

    @Test
    fun `test parsing invalid json`() {
        assertNull("not json".decodeJsonToMap())
        assertNull("{\"missing\": \"end bracket \"".decodeJsonToMap())
    }

    @Test
    fun `test base64 decoding`() {
        // Encode a known input, using Java's built in Base64 encoder.
        val input = "This is a test string"
        val inputEncoded = Base64.getEncoder().encodeToString(input.toByteArray(Charsets.UTF_8))

        // Verify that the extension method decodes the ByteArray as we expect, so that it matches the original input.
        val inputDecoded = inputEncoded.decodeBase64()
        assertNotNull(inputDecoded)
        assertEquals(input, inputDecoded?.toString(Charsets.UTF_8))
    }
}
