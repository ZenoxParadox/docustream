package com.docustream.encryption

import android.support.annotation.Size
import android.util.Base64
import java.nio.charset.StandardCharsets
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

const val KEYSPEC_ALGORITHM = "AES"
const val CIPHER_ALGORITHM = "AES/CBC/PKCS5PADDING"

private const val MINIMUM_KEY_SIZE = 16L

/**
 * Created by Killian on 02/03/2018.
 */
class DataCipher(
        @Size(min = MINIMUM_KEY_SIZE, multiple = MINIMUM_KEY_SIZE) secretKey: String,
        @Size(min = MINIMUM_KEY_SIZE, multiple = MINIMUM_KEY_SIZE) ivParameter: String,
        keySpecAlgorithm: String = KEYSPEC_ALGORITHM,
        cipherAlgorithm: String = CIPHER_ALGORITHM
) {

    init {
        if (!isMultipleOf(secretKey, MINIMUM_KEY_SIZE)) {
            throw IllegalArgumentException("secretKey length (${secretKey.length}) should be multiple of $MINIMUM_KEY_SIZE")
        }

        if (!isMultipleOf(ivParameter, MINIMUM_KEY_SIZE)) {
            throw IllegalArgumentException("ivParameter length (${ivParameter.length}) should be multiple of $MINIMUM_KEY_SIZE")
        }
    }

    private val keySpec = SecretKeySpec(secretKey.toByteArray(StandardCharsets.UTF_8), keySpecAlgorithm)
    private val vectorSpec = IvParameterSpec(ivParameter.toByteArray(StandardCharsets.UTF_8))
    private val cipher = Cipher.getInstance(cipherAlgorithm)

    private fun isMultipleOf(value: String, multiple: Long): Boolean {
        if (value.isEmpty()) return false
        return value.length % multiple == 0L
    }

    fun encrypt(value: String): String {
        cipher.init(Cipher.ENCRYPT_MODE, keySpec, vectorSpec)

        val encrypted = cipher.doFinal(value.toByteArray())
        return Base64.encodeToString(encrypted, Base64.DEFAULT)
    }

    fun decrypt(value: String): String {
        cipher.init(Cipher.DECRYPT_MODE, keySpec, vectorSpec)

        val decryptedBytes = cipher.doFinal(Base64.decode(value, Base64.DEFAULT))
        return String(decryptedBytes)
    }

}