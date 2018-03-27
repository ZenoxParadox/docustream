package com.docustream.encryption

import android.annotation.SuppressLint
import android.content.Context
import android.os.Build
import android.security.KeyPairGeneratorSpec
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.support.annotation.VisibleForTesting
import android.util.Base64
import android.util.Log
import com.docustream.DocuStream
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

/**
 * The master pair is an RSA backed pair that encrypts/decrypts keys used for encryption
 */
private const val MASTER_ENCRYPTION_TYPE = "RSA"
private const val MASTER_PAIR = "MASTER_PAIR"

@SuppressLint("VisibleForTests")
class DataCipher(private val context: Context) : Scrambler {

    private val store = KeyStore.getInstance(ANDROID_KEYSTORE)
    private val dataCipher = Cipher.getInstance("AES/CBC/PKCS5PADDING")
    private val masterCipher = Cipher.getInstance("RSA/ECB/PKCS1PADDING")
    private val keyDocument: DocuStream<Keys>
    private lateinit var secretKey: SecretKeySpec

    init {
        // Make sure we're dealing with application context
        if (context != context.applicationContext) {
            throw IllegalArgumentException("Context must be of application!")
        }

        store.load(null)

        initialize(MASTER_PAIR)

        /*
        This keyDocument does NOT use a masterCipher. Instead it relies on the RSA pair to encrypt/decrypt
        it's keyDocument
        */
        keyDocument = DocuStream(context, rootType = Keys::class.java)
        val keys = keyDocument.getData()

        // Make sure we have a secret key
        if (keys.secret.isNullOrEmpty()) {
            val secretBytes = generateSecret()
            secretKey = SecretKeySpec(secretBytes, 0, secretBytes.size, "AES")

            // store the key for later use
            val secretString = Base64.encodeToString(secretBytes, Base64.NO_WRAP)
            val secretEncryptedString = encryptAsymmetric(secretString)
            keys.secret = secretEncryptedString
            keyDocument.setData(keys)
        } else {
            keys.secret?.let { encryptedSecretString ->
                val decryptedSecretString = decryptAsymmetric(encryptedSecretString)
                val secretBytes = Base64.decode(decryptedSecretString, Base64.NO_WRAP)
                secretKey = SecretKeySpec(secretBytes, 0, secretBytes.size, "AES")
            }
        }
    }

    // TODO handle case where the certificate is expired
    // TODO (decrypt everything -> renew master pair -> encrypt everything)
    private fun initialize(masterName: String) {
        // Only one master pair can exist at once
        if (store.containsAlias(masterName)) {
            Log.v(LOG_TAG, "skipping RSA masterpair.")
            return
        }

        // TODO Research why this is -> what is a 'good' expirationtime?
        val start = Calendar.getInstance()
        val end = Calendar.getInstance()
        //end.add(Calendar.YEAR, 1)
        end.add(Calendar.SECOND, 1)

        // Initialize a KeyPairGenerator from the AndroidKeyStore
        val generator = KeyPairGenerator.getInstance(MASTER_ENCRYPTION_TYPE, ANDROID_KEYSTORE)

        //  Object to pass parameters to the KeyPairGenerator
        val rsaSpec = RSAKeyGenParameterSpec(2048, RSAKeyGenParameterSpec.F4)

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            val builder = KeyPairGeneratorSpec.Builder(context)
            builder.setAlias(masterName)
            builder.setSubject(X500Principal("CN=" + masterName))
            builder.setSerialNumber(BigInteger.valueOf(1337))
            builder.setStartDate(start.time)
            builder.setEndDate(end.time)
            builder.setAlgorithmParameterSpec(rsaSpec)

            generator.initialize(builder.build())
        } else {
            val builder = KeyGenParameterSpec.Builder(masterName, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
            builder.setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_RSA_PKCS1)
            builder.setSignaturePaddings(KeyProperties.SIGNATURE_PADDING_RSA_PKCS1)
            builder.setDigests(KeyProperties.DIGEST_SHA512)

//            builder.setCertificateSubject(X500Principal("CN=" + masterName))
//            builder.setCertificateSerialNumber(BigInteger.valueOf(1337))
            builder.setCertificateNotBefore(start.time)
            builder.setCertificateNotAfter(end.time)
            builder.setAlgorithmParameterSpec(rsaSpec)

            generator.initialize(builder.build())
        }

        generator.generateKeyPair()
    }

    private fun generateSecret(): ByteArray {
        val generator = KeyGenerator.getInstance("AES")
        generator.init(128)
        val key = generator.generateKey()
        return key.encoded
    }

    /**
     * Method that will generate random bytes that can be used as vector bytes. Make sure you
     * save them after you used them successfully (using [setVector])
     */
    override fun generateVector(): ByteArray {
        val bytes = ByteArray(16)
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
            Log.d(LOG_TAG, "byteString: $byteString")
            val encryptedString = encryptAsymmetric(byteString)
            Log.d(LOG_TAG, "(encrypted) encryptedString: $encryptedString")

            keys.vector = encryptedString
            keyDocument.setData(keys)
        }
    }

    private fun getVector(): ByteArray {
        val keys = keyDocument.getData()

        keys.vector?.let { vector ->
            Log.d(LOG_TAG, "(encrypted) vector: $vector")
            val decryptedString = decryptAsymmetric(vector)
            return Base64.decode(decryptedString, Base64.NO_WRAP)
        }

        // This is an implementation issue; vector should not be null at this point
        throw IllegalStateException("Vector is null.")
    }

    @Throws(IllegalArgumentException::class)
    private fun isNew(oldVector: String?, newVectorBytes: ByteArray): Boolean {
        Log.i(LOG_TAG, "isNew(${newVectorBytes.hashCode()})")

        oldVector?.let { encryptedString ->
            val decryptedString = decryptAsymmetric(encryptedString)
            val previousVector = decryptedString.toByteArray()
            Log.v(LOG_TAG, "previousVector.hashCode(): ${previousVector.hashCode()}")

            if (newVectorBytes.contentEquals(previousVector)) {
                throw IllegalArgumentException("Bad implementation; can only store new bytes.")
            }
        }

        Log.v(LOG_TAG, "vector bytes are new.")
        return true
    }

    /**
     * Gives the public key. Can be used for third parties to encrypt data and securely send back.
     *
     * TODO: create mechanism that can sends this key and a third party encrypts data with it
     * TODO: and later sends back
     */
    fun getPublic(): PublicKey {
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
        Log.i(LOG_TAG, "reset()")
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
