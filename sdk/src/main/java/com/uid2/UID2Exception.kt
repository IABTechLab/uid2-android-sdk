package com.uid2

/**
 * Base class for all custom exceptions reported by the UID2 SDK.
 */
public open class UID2Exception(message: String? = null, cause: Throwable? = null) : Exception(message, cause)

/**
 * The SDK has been initialized *after* it's been created.
 */
internal class InitializationException(message: String? = null) : UID2Exception(message)

/**
 * The configured API URL is invalid.
 */
internal class InvalidApiUrlException : UID2Exception()

/**
 * An attempt to generate one of the required keys (for token generation) failed.
 */
internal class CryptoException : UID2Exception()

/**
 * The given input appears to be invalid.
 */
public class InputValidationException(description: String?) : UID2Exception(description)

/**
 * An attempt was made to the API that resulted in a failure.
 */
internal class RequestFailureException(val statusCode: Int, message: String? = null) : UID2Exception(message)

/**
 * The encrypted payload could not be decrypted successfully.
 */
internal class PayloadDecryptException : UID2Exception()

/**
 * The decrypted payload appears invalid.
 */
internal class InvalidPayloadException : UID2Exception()
