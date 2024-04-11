package com.uid2.utils

import com.uid2.extensions.decodeBase64
import org.json.JSONArray
import java.security.KeyFactory
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.PublicKey
import java.security.SecureRandom
import java.security.spec.ECGenParameterSpec
import java.security.spec.X509EncodedKeySpec
import javax.crypto.KeyAgreement
import javax.crypto.SecretKey
import javax.crypto.spec.SecretKeySpec

/**
 * An class containing utility methods for generating Security based keys, e.g. [PublicKey], [KeyPair] and [SecretKey].
 */
internal interface KeyUtils {
    /**
     * Generates the additional authentication data required when generating an identity.
     */
    fun generateAad(now: Long, appName: String): String

    /**
     * Generates a new IV of the given length.
     */
    fun generateIv(length: Int): ByteArray

    /**
     * Generates the [PublicKey] that was provided by the UID2 API server.
     */
    fun generateServerPublicKey(publicKey: String): PublicKey?

    /**
     * Generates a new Public/Private [KeyPair].
     */
    fun generateKeyPair(): KeyPair?

    /**
     * For a given [PublicKey] and [KeyPair], generates a [SecretKey].
     */
    fun generateSharedSecret(serverPublicKey: PublicKey, clientKeyPair: KeyPair): SecretKey?

    companion object Default : KeyUtils {

        override fun generateAad(now: Long, appName: String): String {
            return JSONArray().apply {
                put(now)
                put(appName)
            }.toString()
        }

        override fun generateIv(length: Int): ByteArray {
            return ByteArray(length).apply { random.nextBytes(this) }
        }

        override fun generateServerPublicKey(publicKey: String): PublicKey? {
            val serverPublicKeyBytes =
                publicKey.substring(SERVER_PUBLIC_KEY_PREFIX_LENGTH).decodeBase64() ?: return null

            return KeyFactory.getInstance("EC")
                .generatePublic(
                    X509EncodedKeySpec(
                        serverPublicKeyBytes,
                    ),
                )
        }

        override fun generateKeyPair(): KeyPair? {
            return runCatching {
                KeyPairGenerator.getInstance("EC").apply {
                    initialize(ECGenParameterSpec("secp256r1"))
                }.genKeyPair()
            }.getOrNull()
        }

        override fun generateSharedSecret(serverPublicKey: PublicKey, clientKeyPair: KeyPair): SecretKey? {
            return runCatching {
                val secretKey = KeyAgreement.getInstance("ECDH").apply {
                    init(clientKeyPair.private)
                    doPhase(serverPublicKey, true)
                }.generateSecret()

                SecretKeySpec(secretKey, "AES")
            }.getOrNull()
        }

        private val random: SecureRandom by lazy { SecureRandom() }
        private const val SERVER_PUBLIC_KEY_PREFIX_LENGTH = 9
    }
}
