package com.uid2.network

import com.uid2.data.IdentityStatus
import com.uid2.data.UID2Identity

/**
 * The data available after attempting to generate or refresh the Identity.
 */
internal data class ResponsePackage(
    val identity: UID2Identity?,
    val status: IdentityStatus,
    val message: String,
)
