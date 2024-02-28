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

    @Test
    fun `test SHA-256 hashing`() {
        mapOf(
            "test.test@test.com" to "dvECjPKZHya0/SIhSGwP0m8SgTv1vzLxPULUOsm880M=",
            "testtest@gmail.com" to "LkLfFrut8Tc3h/fIvYDiBKSbaMiau/DtaLBPQYszdMw=",
            "test+test@test.com" to "rQ4yzdOz4uG8N54326QyZD6/JwqrXn4lmy34cVCojB8=",
            "+test@test.com" to "weFizOVVWKlLfyorbBU8oxYDv4HJtTZCPMyZ4THzUQE=",
            "test@gmail.com" to "h5JGBrQTGorO7q6IaFMfu5cSqqB6XTp1aybOD11spnQ=",
            "testtest@test.com" to "d1Lr/s4GLLX3SvQVMoQdIMfbQPMAGZYry+2V+0pZlQg=",
            "testtest@gmail.com" to "LkLfFrut8Tc3h/fIvYDiBKSbaMiau/DtaLBPQYszdMw=",
            "\uD83D\uDE0Atesttest@test.com" to "fAFEUqApQ0V/M9mLj/IO54CgKgtQuARKsOMqtFklD4k=",
            "testtest@\uD83D\uDE0Atest.com" to "tcng5pttf7Y2z4ylZTROvIMw1+IVrMpR4D1KeXSrdiM=",
            "testtest@test.com\uD83D\uDE0A" to "0qI21FPLkuez/8RswfmircHPYz9Dtf7/Nch1rSWEQf0=",
        ).forEach {
            // Hash the given string (with SHA-256) and check that the Base64 encoded output matches what we expect.
            assertEquals(it.key.toSha256(), it.value)
        }
    }
}
