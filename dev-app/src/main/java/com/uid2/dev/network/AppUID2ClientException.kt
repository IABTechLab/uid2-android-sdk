package com.uid2.dev.network

import com.uid2.UID2Exception

/**
 * The exception thrown when an error occurred in the Development Application's UID2 Client.
 */
class AppUID2ClientException(message: String? = null, cause: Throwable? = null) : UID2Exception(message, cause)
