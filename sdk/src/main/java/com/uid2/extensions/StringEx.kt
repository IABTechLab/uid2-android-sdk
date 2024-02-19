package com.uid2.extensions

import android.util.Base64
import org.json.JSONObject
import java.security.MessageDigest

/**
 * Extension method to decode a String base Base64.
 */
internal fun String.decodeBase64(): ByteArray? = runCatching { Base64.decode(this, Base64.NO_WRAP) }.getOrNull()

/**
 * Extension to parse a given String as JSON and convert to a Map. If parsing fails, e.g. the JSON
 * is not well formed, then null will be returned.
 */
internal fun String.decodeJsonToMap(): Map<String, Any>? {
    val json = runCatching { JSONObject(this) }.getOrNull() ?: return null

    return json.keys().asSequence().map {
        it to json.get(it)
    }.toMap()
}

/**
 * Extension method to hash a string (via SHA-256) and return the Base64 representation of it.
 */
internal fun String.toSha256(): String {
    return MessageDigest
        .getInstance("SHA-256")
        .digest(toByteArray())
        .encodeBase64()
}
