package com.uid2.dev.utils

import android.content.Context
import android.content.pm.PackageManager
import android.os.Bundle

fun Context.getMetadata(): Bundle = packageManager.getApplicationInfoCompat(
    packageName,
    PackageManager.GET_META_DATA,
).metaData
