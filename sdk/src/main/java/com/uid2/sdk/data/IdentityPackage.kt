package com.uid2.sdk.data

/**
 * The lifecycle of the Identity will cause the UID2Identity to change, along with potentially becoming invalid in some
 * circumstances. This class represents combines the different possible states.
 */
data class IdentityPackage(
    val valid: Boolean,
    val errorMessage: String?,
    val identity: UID2Identity?,
    val status: IdentityStatus
)
