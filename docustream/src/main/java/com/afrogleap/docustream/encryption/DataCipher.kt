package com.afrogleap.docustream.encryption

import android.annotation.SuppressLint
import android.content.Context
import android.os.Build
import android.security.KeyPairGeneratorSpec
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.support.annotation.VisibleForTesting
import android.util.Base64
import android.util.Log
import com.afrogleap.docustream.DocuStream
import java.math.BigInteger
import java.security.KeyPairGenerator
import java.security.KeyStore
import java.security.PrivateKey
import java.security.PublicKey
import java.security.SecureRandom
import java.security.spec.RSAKeyGenParameterSpec
import java.util.Calendar
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec
import javax.security.auth.x500.X500Principal

private const val LOG_TAG = "DataCipher"

private const val ANDROID_KEYSTORE = "AndroidKeyStore"

private const val SECRET_KEY_ALGORITHM = "AES"
private const val SECRET_KEY_SIZE = 128
private const val VECTOR_SIZE = 16

/**
 * The master pair is an RSA backed pair that encrypts/decrypts keys used for encryption
 */
private const val MASTER_ENCRYPTION_TYPE = "RSA"
private const val MASTER_KEY_SIZE = 2048
private const val MASTER_PAIR = "MASTER_PAIR"

@SuppressLint("VisibleForTests")
class DataCipher(private val context: Context) : Scrambler {

    private val store = KeyStore.getInstance(ANDROID_KEYSTORE)
    private val dataCipher = Cipher.getInstance("AES/CTR/PKCS5PADDING")
    private val masterCipher = Cipher.getInstance("RSA/ECB/PKCS1PADDING")
    private val keyDocument: DocuStream<Keys>
    private lateinit var secretKey: SecretKeySpec

    init {
        // Make sure we're dealing with application context
        if (context != context.applicationContext) {
            throw IllegalArgumentException("Context must be of application!")
        }

        store.load(null)

        // Only one master pair can exist at once
        if (!store.containsAlias(MASTER_PAIR)) {
            initialize(MASTER_PAIR)
        }

        /*
        This keyDocument does NOT use a cipher. Instead it relies on the RSA pair to encrypt/decrypt
        it's keyDocument
        */
        keyDocument = DocuStream(context, rootType = Keys::class.java)
        val keys = keyDocument.getData()

        // Make sure we have a secret key
        if (keys.secret.isNullOrEmpty()) {
            val secretBytes = generateSecret()
            secretKey = getSecretKeySpec(secretBytes, SECRET_KEY_ALGORITHM)

            // store the key for later use
            val secretString = Base64.encodeToString(secretBytes, Base64.NO_WRAP)
            keys.secret = encryptAsymmetric(secretString)
            keyDocument.setData(keys)
        } else {
            keys.secret?.let { encryptedSecretString ->
                val decryptedSecretString = decryptAsymmetric(encryptedSecretString)
                val secretBytes = Base64.decode(decryptedSecretString, Base64.NO_WRAP)
                secretKey = getSecretKeySpec(secretBytes, SECRET_KEY_ALGORITHM)
            }
        }
    }

    private fun getSecretKeySpec(bytes: ByteArray, type: String): SecretKeySpec {
        return SecretKeySpec(bytes, 0, bytes.size, type)
    }

    private fun initialize(masterName: String) {

        val start = Calendar.getInstance()
        val end = Calendar.getInstance()
        end.add(Calendar.YEAR, 2)

        // Initialize a KeyPairGenerator from the AndroidKeyStore
        val generator = KeyPairGenerator.getInstance(MASTER_ENCRYPTION_TYPE, ANDROID_KEYSTORE)

        //  Object to pass parameters to the KeyPairGenerator
        val rsaSpec = RSAKeyGenParameterSpec(MASTER_KEY_SIZE, RSAKeyGenParameterSpec.F4)

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            val builder = KeyPairGeneratorSpec.Builder(context)
            builder.setAlias(masterName)
            builder.setSubject(X500Principal("CN=" + masterName))
            builder.setSerialNumber(BigInteger.valueOf(1337))
            builder.setStartDate(start.time)
            builder.setEndDate(end.time)

            if (Build.VERSION.SDK_INT > Build.VERSION_CODES.JELLY_BEAN_MR2) {
                builder.setAlgorithmParameterSpec(rsaSpec)
            }

            generator.initialize(builder.build())
        } else {
            val builder = KeyGenParameterSpec.Builder(masterName, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
            builder.setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_RSA_PKCS1)
            builder.setSignaturePaddings(KeyProperties.SIGNATURE_PADDING_RSA_PKCS1)
            builder.setDigests(KeyProperties.DIGEST_SHA512)
            builder.setCertificateNotBefore(start.time)
            builder.setCertificateNotAfter(end.time)
            builder.setAlgorithmParameterSpec(rsaSpec)

            generator.initialize(builder.build())
        }

        generator.generateKeyPair()
    }

    private fun generateSecret(): ByteArray {
        val generator = KeyGenerator.getInstance(SECRET_KEY_ALGORITHM)
        generator.init(SECRET_KEY_SIZE)
        val key = generator.generateKey()
        return key.encoded
    }

    /**
     * Method that will generate random bytes that can be used as vector bytes. Make sure you
     * save them after you used them successfully (using [setVector])
     */
    override fun generateVector(): ByteArray {
        val bytes = ByteArray(VECTOR_SIZE)
        val random = SecureRandom()
        random.nextBytes(bytes)
        return bytes
    }

    /**
     * Method for storing vector bytes that was used to encrypt (and store) data. The implementation
     * makes sure the vectors are (at least) different from the previously stored.
     */
    override fun setVector(plainBytes: ByteArray) {
        val keys = keyDocument.getData()

        if (isNew(keys.vector, plainBytes)) {
            val byteString = Base64.encodeToString(plainBytes, Base64.NO_WRAP)
            keys.vector = encryptAsymmetric(byteString)
            keyDocument.setData(keys)
        }
    }

    private fun getVector(): ByteArray {
        val keys = keyDocument.getData()

        keys.vector?.let { vector ->
            val decryptedString = decryptAsymmetric(vector)
            return Base64.decode(decryptedString, Base64.NO_WRAP)
        }

        // This is an implementation issue; vector should not be null at this point
        throw IllegalStateException("Vector is null.")
    }

    @Throws(IllegalArgumentException::class)
    private fun isNew(oldVector: String?, newVectorBytes: ByteArray): Boolean {
        oldVector?.let { encryptedString ->
            val decryptedString = decryptAsymmetric(encryptedString)
            val previousVector = decryptedString.toByteArray()

            if (newVectorBytes.contentEquals(previousVector)) {
                throw IllegalArgumentException("Bad implementation; can only store new bytes.")
            }
        }

        return true
    }

    private fun getPublic(): PublicKey {
        return store.getCertificate(MASTER_PAIR).publicKey
    }

    private fun getPrivate(): PrivateKey {
        val entry = store.getEntry(MASTER_PAIR, null) as KeyStore.PrivateKeyEntry
        return entry.privateKey
    }

    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    fun encryptAsymmetric(key: String): String {
        val masterKey = getPublic()
        masterCipher.init(Cipher.ENCRYPT_MODE, masterKey)
        val encrypted = masterCipher.doFinal(key.toByteArray())
        return Base64.encodeToString(encrypted, Base64.NO_WRAP)
    }

    fun decryptAsymmetric(encryptedKey: String): String {
        val masterKey = getPrivate()
        masterCipher.init(Cipher.DECRYPT_MODE, masterKey)
        val bytes = masterCipher.doFinal(Base64.decode(encryptedKey, Base64.NO_WRAP))
        return String(bytes)
    }

    /**
     * Method to encrypt data. The [vector] should be saved for the decryption process.
     */
    override fun encrypt(raw: String, vector: ByteArray): String {
        dataCipher.init(Cipher.ENCRYPT_MODE, secretKey, IvParameterSpec(vector))
        val encrypted = dataCipher.doFinal(raw.toByteArray())
        return Base64.encodeToString(encrypted, Base64.NO_WRAP)
    }

    /**
     * Method for decrypting data that was encrypted with this class.
     *
     * It's important that the vector that was used to encrypt this data has been stored. The last
     * vector will be obtained internally. If they do not match; your data is lost.
     */
    override fun decrypt(encrypted: String): String {
        val vector = getVector()
        return decrypt(encrypted, vector)
    }

    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    fun decrypt(encrypted: String, vector: ByteArray): String {
        dataCipher.init(Cipher.DECRYPT_MODE, secretKey, IvParameterSpec(vector))
        val decryptedBytes = dataCipher.doFinal(Base64.decode(encrypted, Base64.NO_WRAP))
        return String(decryptedBytes)
    }

    override fun reset(): Int {
        var removed = 0

        for (alias in store.aliases()) {
            Log.w(LOG_TAG, "removing [$alias] from keystore.")
            store.deleteEntry(alias)
            removed++
        }

        val documentRemoved = keyDocument.reset()
        if (documentRemoved == 1) {
            return removed
        }

        return removed.unaryMinus()
    }

}
