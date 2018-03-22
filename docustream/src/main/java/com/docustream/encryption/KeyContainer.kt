package com.docustream.encryption

/**
 * Container interface
 *
 * Created by Killian on 22/03/2018.
 */
interface KeyContainer {

    fun getKey(name: String): String

    fun setKey(name: String, value: String)

    fun hasKey(name: String): Boolean

}