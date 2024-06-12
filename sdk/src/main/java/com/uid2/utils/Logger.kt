package com.uid2.utils

import android.util.Log
import com.uid2.InternalUID2Api

/**
 * Simple logger class that wraps Android's [Log].
 *
 * @suppress
 */
@InternalUID2Api
public class Logger(private val isEnabled: Boolean = false) {
    public fun v(tag: String, throwable: Throwable? = null, message: () -> String = { "" }) {
        if (isEnabled) {
            Log.v(tag, message(), throwable)
        }
    }

    public fun d(tag: String, throwable: Throwable? = null, message: () -> String = { "" }) {
        if (isEnabled) {
            Log.d(tag, message(), throwable)
        }
    }

    public fun i(tag: String, throwable: Throwable? = null, message: () -> String = { "" }) {
        if (isEnabled) {
            Log.i(tag, message(), throwable)
        }
    }

    public fun e(tag: String, throwable: Throwable? = null, message: () -> String = { "" }) {
        Log.e(tag, message(), throwable)
    }
}
