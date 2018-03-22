package com.docustream.encryption

/**
 * container for keys. You might add any amount of keys that will be stored. You can see it as a
 * kind of shared preferences.
 *
 * Created by Killian on 22/03/2018.
 */
data class Keys(
        var secret: String? = null,
        var vector: String? = null
) {

//    fun getSecretBytes(): ByteArray? {
//        return secret?.toByteArray(StandardCharsets.UTF_8)
//    }


//    override fun hasKey(name: String): Boolean {
//        when (name) {
//            "VECTOR" -> return vector.isEmpty()
//            "SECRET" -> return secret.isEmpty()
//        }
//
//        throw IllegalArgumentException("No such key [$name]")
//    }

//    override fun getKey(name: String): String {
//        when (name) {
//            "VECTOR" -> return vector
//            "SECRET" -> return secret
//        }
//
//        throw IllegalArgumentException("No such key [$name]")
//    }
//
//    override fun setKey(name: String, value: String) {
//        when (name) {
//            "VECTOR" -> vector = value
//            "SECRET" -> secret = value
//        }
//
//        throw IllegalArgumentException("No such key [$name]")
//    }

}