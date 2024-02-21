package com.uid2.extensions

import android.util.Base64

/**
 * Extension method to encode a ByteArray to a String which is URL Safe (https://tools.ietf.org/html/rfc4648#page-7).
 * This uses the android.util version of Base64 to keep our minimum SDK low.
 */
internal fun ByteArray.encodeBase64URLSafe(): String = Base64.encodeToString(
    this,
    Base64.URL_SAFE or Base64.NO_PADDING or Base64.NO_WRAP,
)

internal fun ByteArray.encodeBase64NonURLSafe(): String = Base64.encodeToString(
    this,
    Base64.NO_WRAP,
)
