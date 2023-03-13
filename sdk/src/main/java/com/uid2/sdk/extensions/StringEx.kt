package com.uid2.sdk.extensions

import org.json.JSONObject

/**
 * Extension to parse a given String as JSON and convert to a Map. If parsing fails, e.g. the JSON
 * is not well formed, then null will be returned.
 */
fun String.decodeJsonToMap(): Map<String, Any>? {
    val json = runCatching { JSONObject(this) }.getOrNull() ?: return null

    return json.keys().asSequence().map {
        it to json.get(it)
    }.toMap()
}

