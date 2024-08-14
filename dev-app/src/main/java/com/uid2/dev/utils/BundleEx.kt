package com.uid2.dev.utils

import android.os.Bundle

private const val UID2_ENVIRONMENT_EUID = "uid2_environment_euid"

fun Bundle.isEnvironmentEUID(): Boolean = getBoolean(UID2_ENVIRONMENT_EUID, false)
