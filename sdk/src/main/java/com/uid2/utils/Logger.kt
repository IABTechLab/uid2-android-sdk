package com.uid2.utils

import android.util.Log

/**
 * Simple logger class that wraps Android's [Log].
 */
internal class Logger(private val isEnabled: Boolean = false) {
    fun v(tag: String, throwable: Throwable? = null, message: () -> String = { "" }) {
        if (isEnabled) {
            Log.v(tag, message(), throwable)
        }
    }

    fun d(tag: String, throwable: Throwable? = null, message: () -> String = { "" }) {
        if (isEnabled) {
            Log.d(tag, message(), throwable)
        }
    }

    fun i(tag: String, throwable: Throwable? = null, message: () -> String = { "" }) {
        if (isEnabled) {
            Log.i(tag, message(), throwable)
        }
    }

    fun e(tag: String, throwable: Throwable? = null, message: () -> String = { "" }) {
        Log.e(tag, message(), throwable)
    }
}
