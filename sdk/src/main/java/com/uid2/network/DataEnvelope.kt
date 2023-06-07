package com.uid2.network

import com.uid2.extensions.decodeBase64
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

/**
 * This object is responsible for decoding encrypted responses when refreshing the Identity. The type of encryption used
 * as well as the format of the expected data can be found in the following documentation:
 *
 * **See Also:**
 * [GitHub](https://github.com/IABTechLab/uid2docs/blob/main/api/v2/getting-started/gs-encryption-decryption.md)
 */
object DataEnvelope {
    // The name and transformation of the encryption algorithm used.
    private const val ALGORITHM_NAME = "AES"
    private const val ALGORITHM_TRANSFORMATION = "AES/GCM/NoPadding"

    // The length of the authentication tag, in bits.
    private const val AUTHENTICATION_TAG_LENGTH_BITS = 128

    // The length of the IV, in bytes.
    private const val IV_LENGTH_BYTES = 12

    // The number of bytes expected in the decoded payload that represents the timestamp and nonce (used in the original
    // request.)
    private const val PAYLOAD_TIMESTAMP_LENGTH_BYTES = 8
    private const val PAYLOAD_NONCE_LENGTH_BYTES = 8

    /**
     * Decrypts the given data with the provided key.
     *
     * This relies on the format of the data matching that spec-ed in the API documentation. We assume that it's AES
     * encrypted, and includes the IV in the first 12 bytes of the buffer.
     *
     * @param key The key, in Base64 format, required to decode the given data.
     * @param data The data, in Base64 format, that needs to be decoded.
     * @return The unencrypted data. If this decryption fails, null is returned.
     */
    fun decrypt(key: String, data: String, isRefresh: Boolean): ByteArray? {
        // Attempt to decrypt the given data with the provided key. Both the key and data are expected to be in Base64
        // format. If this fails, then null will be returned.
        var payload = decryptWithCipher(key.decodeBase64(), data.decodeBase64()) ?: return null

        // If we are not refreshing, we expect the decoded payload to include both Timestamp and Nonce values.
        if (!isRefresh) {
            payload = payload.copyOfRange(
                PAYLOAD_TIMESTAMP_LENGTH_BYTES + PAYLOAD_NONCE_LENGTH_BYTES,
                payload.size,
            )
        }

        return payload
    }

    private fun decryptWithCipher(key: ByteArray?, data: ByteArray?): ByteArray? {
        key ?: return null
        data ?: return null

        val secret = SecretKeySpec(key, ALGORITHM_NAME)
        val spec = GCMParameterSpec(AUTHENTICATION_TAG_LENGTH_BITS, data, 0, IV_LENGTH_BYTES)

        // Initialise the appropriate AES Cipher.
        val cipher = Cipher.getInstance(ALGORITHM_TRANSFORMATION)?.apply {
            init(Cipher.DECRYPT_MODE, secret, spec)
        } ?: return null

        // Decrypt the data, skipping the first 12 bytes since that contains our IV.
        return cipher.doFinal(data, IV_LENGTH_BYTES, data.size - IV_LENGTH_BYTES)
    }
}
