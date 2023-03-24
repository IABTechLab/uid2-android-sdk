package com.uid2

/**
 * The structure representing a version of the SDK.
 */
data class Version(val major: Int, val minor: Int, val patch: Int)

object UID2 {
    private const val VERSION_STRING = "0.1.0"

    private const val VERSION_COMPONENTS = 3
    private val INVALID_VERSION = Version(0, 0, 0)

    /**
     * Gets the version of the included UID2 SDK library, in string format.
     */
    fun getVersion() = VERSION_STRING

    /**
     * Gets the version of the included UID2 SDK library, in its individual major, minor and patch components.
     */
    fun getVersionInfo(): Version {
        val components = VERSION_STRING.split(".")
        if (components.size != VERSION_COMPONENTS) {
            return INVALID_VERSION
        }

        return Version(components[0].toInt(), components[0].toInt(), components[0].toInt())
    }
}
