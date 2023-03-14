package com.uid2.sdk

/**
 * Base class for all custom exceptions reported by the UID2 SDK.
 */
open class UID2Exception(message: String? = null, cause: Throwable? = null): Exception(message, cause)

/**
 * The configured API URL is invalid.
 */
class InvalidApiUrlException: UID2Exception()

/**
 * The attempt to refresh the token/identity via the API failed.
 */
class RefreshTokenException(val statusCode: Int): UID2Exception()

/**
 * The encrypted payload could not be decrypted successfully.
 */
class PayloadDecryptException: UID2Exception()

/**
 * The decrypted payload appears invalid.
 */
class InvalidPayloadException: UID2Exception()
