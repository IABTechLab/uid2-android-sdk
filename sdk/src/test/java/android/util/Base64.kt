@file:JvmName("Base64")

package android.util

import java.util.Base64

/**
 * This implementation allows us to utilise Android's Base64 support (with lower target API) but during tests, we will
 * redirect to the Java implementation that is available locally.
 */
object Base64 {
    @JvmStatic
    fun decode(str: String?, flags: Int): ByteArray {
        return Base64.getDecoder().decode(str)
    }
}
