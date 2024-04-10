package com.uid2.data

import com.uid2.data.IdentityRequest.Email
import com.uid2.data.IdentityRequest.EmailHash
import com.uid2.data.IdentityRequest.Phone
import com.uid2.data.IdentityRequest.PhoneHash
import com.uid2.extensions.toSha256
import org.json.JSONObject

/**
 * This class represents the different types of identity requests that are supported client side.
 */
public sealed class IdentityRequest(internal val data: String) {

    /**
     * A raw email address.
     */
    public data class Email(private var email: String) : IdentityRequest(email)

    /**
     * A SHA-256 hashed email address.
     */
    public data class EmailHash(private var hash: String) : IdentityRequest(hash)

    /**
     * A raw telephone number.
     */
    public data class Phone(private val phone: String) : IdentityRequest(phone)

    /**
     * A SHA-256 hashed telephone number.
     */
    public data class PhoneHash(private var hash: String) : IdentityRequest(hash)
}

/**
 * Extension method to convert the associated [IdentityRequest] into the expected payload.
 *
 * The payload should only contained a SHA-256 hashed representation of the data. If a raw email or telephone number has
 * been provided, then we will has it ourselves when building the payload.
 */
internal fun IdentityRequest.toPayload(packageName: String): String {
    val payloadKey = when (this) {
        is Email, is EmailHash -> PARAM_EMAIL_HASH
        is Phone, is PhoneHash -> PARAM_PHONE_HASH
    }

    val payloadValue = when (this) {
        is Email -> data.toSha256()
        is Phone -> data.toSha256()
        else -> data
    }

    return JSONObject().apply {
        put(payloadKey, payloadValue)

        // If the identity has opted out, we will get an opt-out response.
        put(PARAM_OPT_OUT_CHECK, 1)
        put("app_name", packageName)
    }.toString()
}

private const val PARAM_EMAIL_HASH = "email_hash"
private const val PARAM_PHONE_HASH = "phone_hash"
private const val PARAM_OPT_OUT_CHECK = "optout_check"
