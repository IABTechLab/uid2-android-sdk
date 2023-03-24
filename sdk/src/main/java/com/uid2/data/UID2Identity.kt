package com.uid2.data

import org.json.JSONObject

/**
 * This class represents the core UID2 identity.
 */
data class UID2Identity(
    val advertisingToken: String,
    val refreshToken: String,
    val identityExpires: Long,
    val refreshFrom: Long,
    val refreshExpires: Long,
    val refreshResponseKey: String
) {

    /**
     * Converts the current UID2Identity instance into a JSON representation.
     */
    fun toJson() = JSONObject(
        mapOf(
            "advertising_token" to advertisingToken,
            "refresh_token" to refreshToken,
            "identity_expires" to identityExpires,
            "refresh_from" to refreshFrom,
            "refresh_expires" to refreshExpires,
            "refresh_response_key" to refreshResponseKey
        )
    )

    companion object {

        /**
         * Helper function to parse a given JSON object into the expected UID2Identity instance. If the JSON instance
         * doesn't contain all required parameters, then null is returned.
         */
        fun fromJson(json: JSONObject): UID2Identity? {
            val advertisingToken = json.opt("advertising_token")?.toString() ?: return null
            val refreshToken = json.opt("refresh_token")?.toString() ?: return null
            val identityExpires = (json.opt("identity_expires")?.toString())?.toLongOrNull() ?: return null
            val refreshFrom = (json.opt("refresh_from")?.toString())?.toLongOrNull() ?: return null
            val refreshExpires = (json.opt("refresh_expires")?.toString())?.toLongOrNull() ?: return null
            val refreshResponseKey = json.opt("refresh_response_key")?.toString() ?: return null

            return UID2Identity(
                advertisingToken,
                refreshToken,
                identityExpires,
                refreshFrom,
                refreshExpires,
                refreshResponseKey
            )
        }
    }
}
