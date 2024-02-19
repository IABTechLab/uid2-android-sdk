package com.uid2.utils

/**
 * A class containing utility methods around the current time.
 */
internal class TimeUtils {

    /**
     * Returns the current epoch time (in milliseconds).
     */
    fun now() = System.currentTimeMillis()

    /**
     * Returns whether or not the given (epoch) time in milliseconds, is in the past.
     */
    fun hasExpired(expiryMs: Long) = expiryMs <= System.currentTimeMillis()

    /**
     * Returns the number of milliseconds difference between the given time and "now".
     */
    fun diffToNow(fromMs: Long) = fromMs - System.currentTimeMillis()
}
