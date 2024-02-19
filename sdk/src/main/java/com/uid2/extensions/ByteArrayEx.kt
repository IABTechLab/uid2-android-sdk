package com.uid2.extensions

import android.util.Base64

/**
 * Extension method to encode a ByteArray to a String. This uses the android.util version of Base64 to keep our minimum
 * SDK low.
 */
internal fun ByteArray.encodeBase64(): String = Base64.encodeToString(this, Base64.NO_WRAP)
