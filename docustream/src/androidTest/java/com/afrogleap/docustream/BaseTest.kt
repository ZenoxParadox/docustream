package com.afrogleap.docustream

/**
 * Base class that has some utility methods.
 *
 * Created by Killian on 12/09/2018.
 */
abstract class BaseTest {

    fun LOG_TAG(method: String): String {
        return "DocuStreamTest-$method"
    }

    fun fnumber(number: Int, size: Int = 2): String {
        return String.format("%0${size}d", number)
    }

}
