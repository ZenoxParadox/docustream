package com.docustream.encryption

/**
 * Scrambler interface
 *
 * Created by Killian on 20/03/2018.
 */
interface Scrambler {

    fun generateVector(): ByteArray

    fun getVector(): ByteArray

    fun setVector(plainBytes: ByteArray)

    fun encrypt(raw: String, vector: ByteArray): String

    fun decrypt(encrypted: String, vector: ByteArray): String

    fun reset(): Int

}