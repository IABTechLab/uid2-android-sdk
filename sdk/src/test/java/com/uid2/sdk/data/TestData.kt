package com.uid2.sdk.data

import java.io.ByteArrayOutputStream
import java.io.InputStream

object TestData {
    // A valid json representation, but missing the required "refresh_response_key" parameter.
    const val INVALID_IDENTITY = "{\"advertising_token\":\"token\",\"refresh_token\":\"refresh\",\"identity_expires\":123456,\"refresh_from\":321,\"refresh_expires\":654321}"

    // A valid, yet made up, json representation.
    const val VALID_IDENTITY = "{\"advertising_token\":\"token\",\"refresh_token\":\"refresh\",\"identity_expires\":123456,\"refresh_from\":321,\"refresh_expires\":654321,\"refresh_response_key\":\"response key\"}"
    const val VALID_IDENTITY_AD_TOKEN = "token"
    const val VALID_IDENTITY_REFRESH_TOKEN = "refresh"
    const val VALID_IDENTITY_EXPIRES = 123456L
    const val VALID_IDENTITY_REFRESH_FROM = 321L
    const val VALID_IDENTITY_REFRESH_EXPIRES = 654321L
    const val VALID_IDENTITY_REFRESH_RESPONSE_KEY = "response key"

    // A valid json representation, but missing the required "status" parameter.
    const val INVALID_REFRESH = "{\"body\":{\"advertising_token\":\"token\",\"refresh_token\":\"refresh\",\"identity_expires\":123456,\"refresh_from\":321,\"refresh_expires\":654321,\"refresh_response_key\":\"response key\"}}"

    // A valid, yet made up, json representation. The body contains the VALID_IDENTITY example shared above.
    const val VALID_REFRESH = "{\"body\":{\"advertising_token\":\"token\",\"refresh_token\":\"refresh\",\"identity_expires\":123456,\"refresh_from\":321,\"refresh_expires\":654321,\"refresh_response_key\":\"response key\"},\"status\":\"success\"}"
    const val VALID_REFRESH_STATUS = "success"

    const val VALID_REFRESH_OPT_OUT = "{\"status\":\"optout\",\"message\":\"User opted out\"}"
    const val VALID_REFRESH_EXPIRED_TOKEN = "{\"status\":\"expired_token\",\"message\":\"Token expired\"}"

    // A Base64 encrypted refresh response that reports a status of "optout"
    val REFRESH_TOKEN_OPT_OUT_ENCRYPTED = ResourceHelper.loadString("test-data/refresh-token-200-optout-encrypted.txt")
    val REFRESH_TOKEN_OPT_OUT_DECRYPTED = ResourceHelper.loadString("test-data/refresh-token-200-optout-decrypted.json")
    const val REFRESH_TOKEN_ENCRYPTED_OPT_OUT_KEY = "DIMOwK7qL6kNfzlkKNdFk2wfXhml75Rcu4FnTFnODo8="

    val REFRESH_TOKEN_SUCCESS_ENCRYPTED = ResourceHelper.loadString("test-data/refresh-token-200-success-encrypted.txt")
    val REFRESH_TOKEN_SUCCESS_DECRYPTED = ResourceHelper.loadString("test-data/refresh-token-200-success-decrypted.json")
    const val REFRESH_TOKEN_ENCRYPTED_SUCCESS_KEY = "mJ8UtxUs4eUennG/h1v5pcxbtSHLSkzVZ1es4a3gLME="
}

/**
 * A helper class to load test resources with a given name.
 */
private object ResourceHelper {
    fun loadString(name: String) = loadString(ResourceHelper::class.java.classLoader, name)

    fun loadString(loader: ClassLoader?, name: String): String {
        loader ?: return ""

        // We can use the provided ClassLoader to attempt to access the named resource via an InputStream.
        return runCatching {
            loader.getResourceAsStream(name).use {
                loadString(it)
            }
        }.getOrDefault("")

    }

    fun loadString(inputStream: InputStream?): String {
        inputStream ?: return ""

        // Attempt to copy the data from the given InputStream into our own OutputStream.
        val outputStream = ByteArrayOutputStream().apply {
            inputStream.copyTo(this)
        }

        return outputStream.toString(Charsets.UTF_8.name()).trimEnd()
    }
}

