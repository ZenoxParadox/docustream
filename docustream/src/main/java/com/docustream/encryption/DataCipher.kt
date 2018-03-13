package com.docustream.encryption

import android.content.Context
import android.util.Base64
import android.util.Log
import hugo.weaving.DebugLog
import java.security.KeyStore
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec


private const val LOG_TAG = "DataCipher"

const val KEYSPEC_ALGORITHM = "AES"
const val CIPHER_ALGORITHM = "AES/CBC/PKCS5PADDING"

private const val MINIMUM_KEY_SIZE = 16
private const val SECRET_KEY_SIZE = 256

private const val SECRET_ALIAS = "SECRET"
private const val VECTOR_ALIAS = "VECTOR"

/**
 * [vectorSpec] should be random each time you encrypt/decrypt a message. Therefore the value should
 * be changed each time the contents are decrypted (You can't change the vector in the encrypted
 * state because you need it in order to decrypt it).
 *
 * Created by Killian on 02/03/2018.
 */
class DataCipher(
        context: Context,
        private val keySpecAlgorithm: String = KEYSPEC_ALGORITHM,
        cipherAlgorithm: String = CIPHER_ALGORITHM
) {

    private val keySpec: SecretKeySpec //(secretKey.toByteArray(StandardCharsets.UTF_8), keySpecAlgorithm)
    private val cipher = Cipher.getInstance(cipherAlgorithm)

    /**
     * Used to store secret-key and initialisation-vector. Keys from here are more difficult to
     * extract then the typical data.
     */
    private val keyStore = KeyStore.getInstance(KeyStore.getDefaultType())

    init {
        Log.d(LOG_TAG, "keyStore.type: ${keyStore.type}")
        Log.d(LOG_TAG, "keyStore.provider: ${keyStore.provider}")

//        val stream = context.resources.openRawResource(R.raw.example)
//        keyStore.load(stream, "example".toCharArray())
        keyStore.load(null)

        Log.d(LOG_TAG, "----------")
        Log.d(LOG_TAG, "alias count: ${keyStore.aliases().asSequence().count()}")
        for (alias in keyStore.aliases()) {
            Log.v(LOG_TAG, "alias: $alias")
        }
        Log.d(LOG_TAG, "----------")

        //keyStore.load(null) // loads the package contents

        if (!hasKey(SECRET_ALIAS)) {
            val generator = KeyGenerator.getInstance(keySpecAlgorithm)
            generator.init(SECRET_KEY_SIZE)
            val key = generator.generateKey()

            setBytes(SECRET_ALIAS, key.encoded)
        }

        val bytes = getBytes(SECRET_ALIAS)
        keySpec = getKeySpec(bytes)
    }

    private fun hasKey(alias: String): Boolean {
        return keyStore.containsAlias(alias)
    }

    private fun getKeySpec(bytes: ByteArray): SecretKeySpec {
        return SecretKeySpec(bytes, 0, bytes.size, keySpecAlgorithm)
    }

    private fun setBytes(alias: String, bytes: ByteArray) {
        val key = getKeySpec(bytes)
        keyStore.setEntry(alias, KeyStore.SecretKeyEntry(key), null)
    }

    /**
     * secret key is once generated and will never change.
     */
    @DebugLog
    private fun getBytes(alias: String): ByteArray {
        if (!hasKey(alias)) {
            throw IllegalArgumentException("Don't have key $alias. This is an implementation issue!")
        }
        val entry = keyStore.getEntry(alias, null) as KeyStore.SecretKeyEntry
        return entry.secretKey.encoded
    }

    @DebugLog
    fun getVectorBytes(): ByteArray {
        if (hasKey(VECTOR_ALIAS)) {
            return getBytes(VECTOR_ALIAS)
        }

        return generateVectorBytes()
    }

    @DebugLog
    fun generateVectorBytes(): ByteArray {
        val bytes = ByteArray(MINIMUM_KEY_SIZE)
        val random = SecureRandom()
        random.nextBytes(bytes)
        return bytes
    }

    /**
     * saves new [bytes] as the new initialisation vector bytes after checking if they are in fact
     * new bytes and not bytes previously used. We are assuming (of course) that they are in fact
     * never used if they are different.
     */
    @DebugLog
    fun saveVectorBytes(bytes: ByteArray) {
        try {
            if (isNew(bytes)) {
                setBytes(VECTOR_ALIAS, bytes)
            }
        } catch (exception: IllegalArgumentException) {
            Log.e(LOG_TAG, "Trying to store old bytes for vector.")
        }
    }

    /**
     * method to make sure that the bytes we are about to store are different from the vector bytes
     * that are stored now. If they are not, it means that the implementation is wrong.
     * @throws IllegalArgumentException when new bytes equal the current bytes
     */
    @Throws(IllegalArgumentException::class)
    private fun isNew(newVectorBytes: ByteArray): Boolean {
        Log.i(LOG_TAG, "isNew($newVectorBytes)")
        if (hasKey(VECTOR_ALIAS)) {
            val previousBytes = getVectorBytes()

            if (newVectorBytes.contentEquals(previousBytes)) {
                throw IllegalArgumentException("Bad implementation; can only store new bytes.")
            }
        }

        return true
    }

    fun encrypt(value: String, vectorBytes: ByteArray): String {
        Log.i(LOG_TAG, "encrypt($value)")

        cipher.init(Cipher.ENCRYPT_MODE, keySpec, IvParameterSpec(vectorBytes))

        val encrypted = cipher.doFinal(value.toByteArray())
        return Base64.encodeToString(encrypted, Base64.DEFAULT)
    }

    fun decrypt(value: String, vectorBytes: ByteArray): String {
        Log.i(LOG_TAG, "decrypt($value)")

        cipher.init(Cipher.DECRYPT_MODE, keySpec, IvParameterSpec(vectorBytes))

        val decryptedBytes = cipher.doFinal(Base64.decode(value, Base64.DEFAULT))
        return String(decryptedBytes)
    }

}