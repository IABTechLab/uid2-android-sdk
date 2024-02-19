package com.uid2.data

/**
 * This enum type represents the different status an Identity can be in.
 *
 * This has been translated from the Web implement, see the following for more information:
 * https://github.com/IABTechLab/uid2-web-integrations/blob/5a8295c47697cdb1fe36997bc2eb2e39ae143f8b/src/Uid2InitCallbacks.ts#L12-L20
 */
public enum class IdentityStatus(public val value: Int) {
    ESTABLISHED(0),
    REFRESHED(1),
    EXPIRED(100),
    NO_IDENTITY(-1),
    INVALID(-2),
    REFRESH_EXPIRED(-3),
    OPT_OUT(-4),
    ;

    public override fun toString(): String = when (this) {
        ESTABLISHED -> "Established"
        REFRESHED -> "Refreshed"
        EXPIRED -> "Expired"
        NO_IDENTITY -> "No Identity"
        INVALID -> "Invalid"
        REFRESH_EXPIRED -> "Refresh Expired"
        OPT_OUT -> "Opt Out"
    }

    public companion object {

        /**
         * Converts the given integer value into the associated IdentityStatus.
         */
        public fun fromValue(value: Int): IdentityStatus = entries.first { it.value == value }
    }
}
