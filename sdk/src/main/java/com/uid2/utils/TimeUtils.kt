package com.uid2.utils

/**
 * A class containing utility methods around the current time.
 */
internal interface TimeUtils {

    /**
     * Returns the current epoch time (in milliseconds).
     */
    fun now(): Long

    /**
     * Returns whether or not the given (epoch) time in milliseconds, is in the past.
     */
    fun hasExpired(expiryMs: Long): Boolean

    /**
     * Returns the number of milliseconds difference between the given time and "now".
     */
    fun diffToNow(fromMs: Long): Long

    companion object Default : TimeUtils {
        override fun now() = System.currentTimeMillis()

        override fun hasExpired(expiryMs: Long) = expiryMs <= System.currentTimeMillis()

        override fun diffToNow(fromMs: Long) = fromMs - System.currentTimeMillis()
    }
}
