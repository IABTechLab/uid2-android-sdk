package com.uid2.extensions

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle

/**
 * Helper method to extract the MetaData Bundle associated with the given Context.
 */
internal fun Context.getMetadata(): Bundle = packageManager.getApplicationInfoCompat(
    packageName,
    PackageManager.GET_META_DATA
).metaData

private fun PackageManager.getApplicationInfoCompat(packageName: String, flags: Int = 0): ApplicationInfo =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        getApplicationInfo(packageName, PackageManager.ApplicationInfoFlags.of(flags.toLong()))
    } else {
        @Suppress("DEPRECATION") getApplicationInfo(packageName, flags)
    }
