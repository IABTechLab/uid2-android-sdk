package com.uid2.dev.utils

import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Build

fun PackageManager.getApplicationInfoCompat(packageName: String, flags: Int = 0): ApplicationInfo =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        @Suppress("WrongConstant")
        getApplicationInfo(packageName, PackageManager.ApplicationInfoFlags.of(flags.toLong()))
    } else {
        @Suppress("DEPRECATION")
        getApplicationInfo(packageName, flags)
    }
