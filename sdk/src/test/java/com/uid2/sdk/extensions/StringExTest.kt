package com.uid2.sdk.extensions

import org.json.JSONObject
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.junit.MockitoJUnitRunner

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
        Assert.assertEquals(map, parsedMap)
    }

    @Test
    fun `test parsing invalid json`() {
        Assert.assertNull("not json".decodeJsonToMap())
        Assert.assertNull("{\"missing\": \"end bracket \"".decodeJsonToMap())
    }
}
