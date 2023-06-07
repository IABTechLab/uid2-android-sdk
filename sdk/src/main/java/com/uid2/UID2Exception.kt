package com.uid2

/**
 * Base class for all custom exceptions reported by the UID2 SDK.
 */
internal open class UID2Exception(message: String? = null, cause: Throwable? = null) : Exception(message, cause)

/**
 * The SDK has been initialized *after* it's been created.
 */
internal class InitializationException(message: String? = null) : UID2Exception(message)

/**
 * The configured API URL is invalid.
 */
internal class InvalidApiUrlException : UID2Exception()

/**
 * The attempt to refresh the token/identity via the API failed.
 */
internal class RefreshTokenException(val statusCode: Int) : UID2Exception()

/**
 * The encrypted payload could not be decrypted successfully.
 */
internal class PayloadDecryptException : UID2Exception()

/**
 * The decrypted payload appears invalid.
 */
internal class InvalidPayloadException : UID2Exception()
