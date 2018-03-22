package com.docustream.encryption

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
import java.security.spec.AlgorithmParameterSpec
import java.util.Calendar
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec
import javax.security.auth.x500.X500Principal

private const val LOG_TAG = "BasicKeyStore"

private const val ANDROID_KEYSTORE = "AndroidKeyStore"

private const val RSA = "RSA"

/**
 * The master pair is an RSA backed pair that encrypts/decrypts keys used for encryption
 */
private const val MASTER_PAIR = "MASTER_PAIR"

//private const val SECRET = "SECRET"
//private const val VECTOR = "VECTOR"


class DataCipher(private val context: Context) : Scrambler {

    private val store = KeyStore.getInstance(ANDROID_KEYSTORE)
    private val dataCipher = Cipher.getInstance("AES/CBC/PKCS5PADDING")
    private val masterCipher = Cipher.getInstance("RSA/ECB/PKCS1PADDING")
    private val keyDocument: DocuStream<Keys>
    private lateinit var secretKey: SecretKeySpec

    init {
        Log.i(LOG_TAG, "init() ----------------------------------------")
        // Make sure we're dealing with application context
        val applicationContext = context.applicationContext
        if (context != applicationContext) {
            throw IllegalArgumentException("Context must be of application!")
        }

        store.load(null)

        if (!store.containsAlias(MASTER_PAIR)) {
            initialize(MASTER_PAIR)
        }

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
            Log.d(LOG_TAG, "(generated) secretString: $secretString (byte hash: ${secretBytes.hashCode()})")
            val secretEncryptedString = encryptKey(secretString, "secret")
            keys.secret = secretEncryptedString
            keyDocument.setData(keys)
        } else {
            keys.secret?.let { encryptedSecretString ->
                Log.d(LOG_TAG, "(from file) encryptedSecretString: $encryptedSecretString")

                val decryptedSecretString = decryptKey(encryptedSecretString, "secret")

                val secretBytes = Base64.decode(decryptedSecretString, Base64.NO_WRAP)
                Log.d(LOG_TAG, "decryptedSecretString: $decryptedSecretString (byte hash: ${secretBytes.hashCode()})")

                secretKey = SecretKeySpec(secretBytes, 0, secretBytes.size, "AES")
            }
        }

        Log.i(LOG_TAG, "---------------------------------------- int()")
    }

    // TODO handle case where the certificate is expired
    // TODO (decrypt everything -> renew master pair -> encrypt everything)
    private fun initialize(masterName: String) {
        // Only one master pair can exist at once
        if (store.containsAlias(masterName)) {
            return
            //throw IllegalStateException("Already have a master pair (alias used: $masterName)")
        }

        val start = Calendar.getInstance()
        val end = Calendar.getInstance()
        end.add(Calendar.YEAR, 1)

        // Initialize a KeyPair generator using the the intended algorithm (in this example, RSA and the KeyStore.  This example uses the AndroidKeyStore.
        val generator = KeyPairGenerator.getInstance(RSA, ANDROID_KEYSTORE)

        // The KeyPairGeneratorSpec object is how parameters for your key pair are passed to the KeyPairGenerator
        val spec: AlgorithmParameterSpec

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            val builder = KeyPairGeneratorSpec.Builder(context)
            builder.setAlias(masterName)
            builder.setSubject(X500Principal("CN=" + masterName))
            builder.setSerialNumber(BigInteger.valueOf(1337))
            builder.setStartDate(start.time)
            builder.setEndDate(end.time)

            spec = builder.build()
        } else {
            val builder = KeyGenParameterSpec.Builder(masterName, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
            builder.setCertificateSubject(X500Principal("CN=" + masterName))
            builder.setDigests(KeyProperties.DIGEST_SHA256)
            builder.setSignaturePaddings(KeyProperties.SIGNATURE_PADDING_RSA_PKCS1)
            builder.setCertificateSerialNumber(BigInteger.valueOf(1337))
            builder.setCertificateNotBefore(start.time)
            builder.setCertificateNotAfter(end.time)

            spec = builder.build()
        }

        generator.initialize(spec)
        generator.generateKeyPair()
    }

    private fun generateSecret(): ByteArray {
        val generator = KeyGenerator.getInstance("AES")
        generator.init(128)
        val key = generator.generateKey()

        return key.encoded
    }

    override fun generateVector(): ByteArray {
        //Log.i(LOG_TAG, "generateVectorBytes()")

        val bytes = ByteArray(16)
        val random = SecureRandom()
        random.nextBytes(bytes)

        return bytes
    }

    override fun setVector(plainBytes: ByteArray) {
        //Log.i(LOG_TAG, "setVector(${plainBytes.size} [hash: ${plainBytes.hashCode()}])")

        val keys = keyDocument.getData()

        if (isNew(keys.vector, plainBytes)) {

            val byteString = Base64.encodeToString(plainBytes, Base64.NO_WRAP)
            //Log.d(LOG_TAG, "(un-encrypted) byteString: $byteString (to save)")
            val encryptedString = encryptKey(byteString, "vector")
            //Log.d(LOG_TAG, "(encrypted) encryptedString: $encryptedString (to save)")

            keys.vector = encryptedString
            keyDocument.setData(keys)
        }
    }

    override fun getVector(): ByteArray {
        //Log.i(LOG_TAG, "getVector()")

        val keys = keyDocument.getData()
        //Log.v(LOG_TAG, keyDocument.getFileContents())

        if (keys.vector.isNullOrEmpty()) {
            //Log.v(LOG_TAG, "generateVectorBytes()")
            throw IllegalStateException("Vector should not be null. Try generating it instead.")
        }

        //Log.d(LOG_TAG, "keys.vector: ${keys.vector} (to-decrypt)")

        val decryptedString = decryptKey(keys.vector!!, "vector")
        //Log.d(LOG_TAG, "decryptedString: $decryptedString")

        val vector = Base64.decode(decryptedString, Base64.NO_WRAP)
        //Log.d(LOG_TAG, "vector.size: [${vector.size} [hash: ${vector.hashCode()}]")

        return vector
    }

    @Throws(IllegalArgumentException::class)
    private fun isNew(oldVector: String?, newVectorBytes: ByteArray): Boolean {
        Log.i(LOG_TAG, "isNew(${newVectorBytes.hashCode()})")

        oldVector?.let { encryptedString ->
            val decryptedString = decryptKey(encryptedString, "vector")
            val previousVector = decryptedString.toByteArray()
            Log.v(LOG_TAG, "previousVector.hashCode(): ${previousVector.hashCode()}")

            if (newVectorBytes.contentEquals(previousVector)) {
                throw IllegalArgumentException("Bad implementation; can only store new bytes.")
                //Log.e(LOG_TAG, "new and old bytes are the same; this is an implementation issue!")
                //return false
            }
        }

        Log.v(LOG_TAG, "vector bytes are new.")
        return true
    }

    /**
     * Gives the public key. Can be used for third parties to encrypt data and securely send back.
     */
    fun getPublic(): PublicKey {
        return store.getCertificate(MASTER_PAIR).publicKey
    }

    private fun getPrivate(): PrivateKey {
        val entry = store.getEntry(MASTER_PAIR, null) as KeyStore.PrivateKeyEntry
        return entry.privateKey
    }

    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    fun encryptKey(key: String, reason: String = "test"): String {
        Log.i(LOG_TAG, "$reason:encryptKey($key)")

        val masterKey = getPublic()
        masterCipher.init(Cipher.ENCRYPT_MODE, masterKey)
        val encrypted = masterCipher.doFinal(key.toByteArray())
        val encryptedString = Base64.encodeToString(encrypted, Base64.NO_WRAP)
        //Log.d(LOG_TAG, "encryptedString: $encryptedString (also encoded)")

        return encryptedString
    }

    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    fun decryptKey(encryptedKey: String, reason: String = "test"): String {
        //Log.i(LOG_TAG, "decryptKey($encryptedKey) --------------------------------------------------")

        val masterKey = getPrivate()
        masterCipher.init(Cipher.DECRYPT_MODE, masterKey)
        val bytes = masterCipher.doFinal(Base64.decode(encryptedKey, Base64.NO_WRAP))
        val decryptedString = String(bytes)
        //Log.d(LOG_TAG, "decryptedString: $decryptedString")

        //Log.i(LOG_TAG, "-------------------------------------------------- decryptKey($decryptedString)")
        return decryptedString
    }

    override fun encrypt(raw: String, vector: ByteArray): String {
        //Log.i(LOG_TAG, "encrypt($secretKey)")

        dataCipher.init(Cipher.ENCRYPT_MODE, secretKey, IvParameterSpec(vector))
        val encrypted = dataCipher.doFinal(raw.toByteArray())
        return Base64.encodeToString(encrypted, Base64.NO_WRAP)
    }

    override fun decrypt(encrypted: String, vector: ByteArray): String {
        //Log.i(LOG_TAG, "decrypt(${vector.size})")

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
