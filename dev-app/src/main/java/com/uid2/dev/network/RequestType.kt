package com.uid2.dev.network

/**
 * The types of supported requests when generating the initial Identity.
 */
enum class RequestType(val parameter: String) {
    EMAIL("email"),
    EMAIL_HASH("email_hash"),
    PHONE("phone"),
    PHONE_HASH("phone_hash")
}
