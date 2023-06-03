package com.uid2.securesignals.ima

import com.uid2.BuildConfig
import com.uid2.VersionParser

/**
 * An object exposing the version information associated with the UID2 IMA Plugin.
 */
internal object PluginVersion {
    private const val VERSION_STRING = BuildConfig.VERSION

    /**
     * Gets the version of the included UID2/IMA plugin, in its individual major, minor and patch components.
     */
    fun getVersionInfo() = VersionParser.parseVersion(VERSION_STRING)
}
