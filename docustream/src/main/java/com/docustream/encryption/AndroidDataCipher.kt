package com.docustream.encryption

import android.content.Context
import android.security.KeyPairGeneratorSpec
import android.util.Base64
import android.util.Log
import java.math.BigInteger
import java.security.KeyPairGenerator
import java.security.KeyStore
import java.util.Calendar
import javax.crypto.Cipher
import javax.security.auth.x500.X500Principal


private const val LOG_TAG = "AndroidDataCipher"

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
class AndroidDataCipher(
        private val context: Context
) {

    private val cipher = Cipher.getInstance("AES/CBC/PKCS5PADDING")

    /**
     * Used to store secret-key and initialisation-vector. Keys from here are more difficult to
     * extract then the typical data.
     */
    private val keyStore = KeyStore.getInstance("AndroidKeyStore")

        init {
        keyStore.load(null)

        Log.d(LOG_TAG, "keyStore.type: ${keyStore.type}")
        Log.d(LOG_TAG, "keyStore.provider: ${keyStore.provider}")

        Log.d(LOG_TAG, "----------")
        Log.d(LOG_TAG, "alias count: ${keyStore.aliases().asSequence().count()}")
        for (alias in keyStore.aliases()) {
            Log.v(LOG_TAG, "alias: $alias")
        }
        Log.d(LOG_TAG, "----------")
    }

    fun createKey(alias: String) {
        Log.i(LOG_TAG, "createKey($alias)")

        val notBefore = Calendar.getInstance()
        val notAfter = Calendar.getInstance()
        notAfter.add(Calendar.YEAR, 1)

        val spec = KeyPairGeneratorSpec.Builder(context)
                .setAlias(alias)
                //.setKeyType("RSA")
                .setKeySize(2048)
                .setSubject(X500Principal("CN=test"))
                .setSerialNumber(BigInteger.ONE)
                .setStartDate(notBefore.time)
                .setEndDate(notAfter.time)
                .build()
        val generator = KeyPairGenerator.getInstance("RSA", "AndroidKeyStore")
        generator.initialize(spec)

        val keyPair = generator.generateKeyPair()

        Log.d(LOG_TAG, "keyPair.public: ${keyPair.public}")
        Log.d(LOG_TAG, "keyPair.private: ${keyPair.private}")
    }

    fun setValue() {
        Log.i(LOG_TAG, "setValue()")

        val generator = KeyPairGenerator.getInstance("RSA")
        val keyPair = generator.genKeyPair()

        Log.d(LOG_TAG, "keyPair.public: ${keyPair.public}")
        Log.d(LOG_TAG, "keyPair.private: ${keyPair.private}")

        val keyEntry = KeyStore.PrivateKeyEntry(keyPair.private, null)

        //keyStore.setEntry("String alias, Entry entry, ProtectionParameter protParam")
        keyStore.setEntry("alias", keyEntry, null)
    }

    fun encrypt(value: String, vectorBytes: ByteArray): String {
        Log.i(LOG_TAG, "encrypt($value)")

        //cipher.init(Cipher.ENCRYPT_MODE, keySpec, IvParameterSpec(vectorBytes))

        val encrypted = cipher.doFinal(value.toByteArray())
        return Base64.encodeToString(encrypted, Base64.DEFAULT)
    }

    fun decrypt(value: String, vectorBytes: ByteArray): String {
        Log.i(LOG_TAG, "decrypt($value)")

        //cipher.init(Cipher.DECRYPT_MODE, keySpec, IvParameterSpec(vectorBytes))

        val decryptedBytes = cipher.doFinal(Base64.decode(value, Base64.DEFAULT))
        return String(decryptedBytes)
    }

}