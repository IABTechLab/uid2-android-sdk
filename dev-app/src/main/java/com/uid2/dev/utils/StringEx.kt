package com.uid2.dev.utils

import java.security.MessageDigest

/**
 * Extension method to hash a string (via SHA-256) and return the Base64 representation of it.
 */
fun String.toSha256(): String {
    return MessageDigest
        .getInstance("SHA-256")
        .digest(toByteArray())
        .encodeBase64URLSafe()
}
