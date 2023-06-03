package com.uid2

/**
 * The structure representing a version of the SDK.
 */
data class Version(val major: Int, val minor: Int, val patch: Int)

/**
 * An object exposing the version information associated with the UID2 SDK.
 */
object UID2 {
    private const val VERSION_STRING = BuildConfig.VERSION

    /**
     * Gets the version of the included UID2 SDK library, in string format.
     */
    fun getVersion(): String = VERSION_STRING

    /**
     * Gets the version of the included UID2 SDK library, in its individual major, minor and patch components.
     */
    fun getVersionInfo() = VersionParser.parseVersion(VERSION_STRING)
}

object VersionParser {
    private const val VERSION_COMPONENTS = 3
    internal val INVALID_VERSION = Version(0, 0, 0)

    fun parseVersion(original: String): Version {
        // Remove any -SNAPSHOT postfix. These builds shouldn't be used in production, but are in tests.
        val version = original.split("-").first()

        val components = version.split(".")
        if (components.size != VERSION_COMPONENTS) {
            return INVALID_VERSION
        }

        return runCatching {
            Version(components[0].toInt(), components[1].toInt(), components[2].toInt())
        }.getOrDefault(INVALID_VERSION)
    }
}
