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
 * A class containing utility methods for generating Security based keys, e.g. [PublicKey], [KeyPair] and [SecretKey].
 * This is useful to allow these methods to be easily mocked for unit tests.
 */
internal class KeyUtils {

    private val random: SecureRandom by lazy { SecureRandom() }

    /**
     * Generates the additional authentication data required when generating an identity.
     */
    fun generateAad(now: Long): String {
        return JSONArray().apply {
            put(now)
        }.toString()
    }

    /**
     * Generates a new IV of the given length.
     */
    fun generateIv(length: Int): ByteArray {
        return ByteArray(length).apply { random.nextBytes(this) }
    }

    /**
     * Generates the [PublicKey] that was provided by the UID2 API server.
     */
    fun generateServerPublicKey(publicKey: String): PublicKey? {
        val serverPublicKeyBytes = publicKey.substring(SERVER_PUBLIC_KEY_PREFIX_LENGTH).decodeBase64() ?: return null

        return KeyFactory.getInstance("EC")
            .generatePublic(
                X509EncodedKeySpec(
                    serverPublicKeyBytes,
                ),
            )
    }

    /**
     * Generates a new Public/Private [KeyPair].
     */
    fun generateKeyPair(): KeyPair? {
        return runCatching {
            KeyPairGenerator.getInstance("EC").apply {
                initialize(ECGenParameterSpec("secp256r1"))
            }.genKeyPair()
        }.getOrNull()
    }

    /**
     * For a given [PublicKey] and [KeyPair], generates a [SecretKey].
     */
    fun generateSharedSecret(serverPublicKey: PublicKey, clientKeyPair: KeyPair): SecretKey? {
        return runCatching {
            val secretKey = KeyAgreement.getInstance("ECDH").apply {
                init(clientKeyPair.private)
                doPhase(serverPublicKey, true)
            }.generateSecret()

            SecretKeySpec(secretKey, "AES")
        }.getOrNull()
    }

    private companion object {
        const val SERVER_PUBLIC_KEY_PREFIX_LENGTH = 9
    }
}
