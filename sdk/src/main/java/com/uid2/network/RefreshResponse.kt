package com.uid2.network

import com.uid2.data.IdentityStatus.ESTABLISHED
import com.uid2.data.IdentityStatus.OPT_OUT
import com.uid2.data.IdentityStatus.REFRESHED
import com.uid2.data.IdentityStatus.REFRESH_EXPIRED
import com.uid2.data.UID2Identity
import com.uid2.network.RefreshResponse.Status.EXPIRED_TOKEN
import com.uid2.network.RefreshResponse.Status.SUCCESS
import org.json.JSONObject

/**
 * This class defines the expected response from the Identity API when refreshing. The results could include a new
 * (refreshed) Identity, or represent a failure/error.
 *
 * https://unifiedid.com/docs/endpoints/post-token-refresh
 */
internal data class RefreshResponse(
    val body: UID2Identity?,
    val status: Status,
    val message: String?,
) {
    /**
     * The different possible status values when refreshing.
     */
    enum class Status(private val text: String) {
        SUCCESS("success"),
        OPT_OUT("optout"),
        EXPIRED_TOKEN("expired_token"),
        CLIENT_ERROR("client_error"),
        INVALID_TOKEN("invalid_token"),
        UNAUTHORIZED("unauthorized"),
        ;

        companion object {
            fun forStatus(status: String) = entries.first { it.text == status }
        }
    }

    /**
     * Converts the response into the equivalent [ResponsePackage].
     */
    fun toResponsePackage(isRefresh: Boolean): ResponsePackage? = when (status) {
        SUCCESS -> {
            if (isRefresh) {
                ResponsePackage(body, REFRESHED, "Identity refreshed")
            } else {
                ResponsePackage(body, ESTABLISHED, "Identity established")
            }
        }
        Status.OPT_OUT -> ResponsePackage(null, OPT_OUT, "User opt out")
        EXPIRED_TOKEN -> ResponsePackage(null, REFRESH_EXPIRED, "Refresh token expired")
        else -> null
    }

    companion object {
        /**
         * Helper function to parse a given JSON object into the expected RefreshResponse instance. If the JSON instance
         * doesn't contain all required parameters, then null is returned.
         */
        fun fromJson(json: JSONObject): RefreshResponse? {
            // We always expect a valid status.
            val statusText = json.opt("status")?.toString() ?: return null

            val status = Status.forStatus(statusText)
            val body = json.optJSONObject("body")?.let { UID2Identity.fromJson(it) }
            val message = json.opt("message")?.toString()

            // Check that if we've successfully refreshed, that we have a valid UID2Identity.
            if (status == SUCCESS && body == null) {
                return null
            }

            return RefreshResponse(
                body,
                status,
                message,
            )
        }
    }
}
