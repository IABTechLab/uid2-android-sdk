package com.uid2.sdk.network

import com.uid2.sdk.data.IdentityStatus
import com.uid2.sdk.data.UID2Identity

/**
 * The data available after attempting to refresh the Identity.
 */
data class RefreshPackage(
    val identity: UID2Identity?,
    val status: IdentityStatus,
    val message: String
)
