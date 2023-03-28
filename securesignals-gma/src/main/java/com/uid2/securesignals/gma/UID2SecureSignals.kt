package com.uid2.securesignals.gma

import com.uid2.BuildConfig
import com.uid2.Version

/**
 * An object exposing the version information associated with the UID2 GMA Plugin.
 */
object UID2SecureSignals {
    private const val VERSION_STRING = BuildConfig.VERSION

    private const val VERSION_COMPONENTS = 3
    private val INVALID_VERSION = Version(0, 0, 0)

    /**
     * Gets the version of the included UID2/IMA plugin, in string format.
     */
    fun getVersion() = VERSION_STRING

    /**
     * Gets the version of the included UID2/IMA plugin, in its individual major, minor and patch components.
     */
    fun getVersionInfo(): Version {
        val components = VERSION_STRING.split(".")
        if (components.size != VERSION_COMPONENTS) {
            return INVALID_VERSION
        }

        return Version(components[0].toInt(), components[0].toInt(), components[0].toInt())
    }
}
