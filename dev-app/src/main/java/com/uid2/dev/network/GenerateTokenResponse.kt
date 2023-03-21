package com.uid2.dev.network

import com.uid2.data.UID2Identity
import org.json.JSONObject

data class GenerateTokenResponse(
    val body: UID2Identity?,
    val status: String,
    val message: String?
) {
    companion object {
        private const val STATUS = "status"
        private const val BODY = "body"
        private const val MESSAGE = "message"

        /**
         * Helper function to parse a given JSON object into the expected GenerateTokenResponse instance. If the JSON
         * instance doesn't contain all required parameters, then null is returned.
         */
        fun fromJson(json: JSONObject): GenerateTokenResponse? {
            // We always expect a valid status.
            val status = json.opt(STATUS)?.toString() ?: return null

            val body = json.optJSONObject(BODY)?.let { UID2Identity.fromJson(it) }
            val message = json.opt(MESSAGE)?.toString()

            return GenerateTokenResponse(
                body,
                status,
                message
            )
        }
    }
}
