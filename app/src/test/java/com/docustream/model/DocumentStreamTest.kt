package com.docustream.model

import com.docustream.DocumentStream
import com.google.gson.GsonBuilder
import junit.framework.Assert
import org.junit.Before
import org.junit.Test

/**
 * Created by Killian on 19/01/2018.
 */
class DocumentStreamTest {

    val gson = GsonBuilder().serializeNulls().create()

    val stream = DocumentStream()

    @Before
    fun setup(){
        // -
    }

    @Test
    fun _Config_shouldAlwaysPass() {
        Assert.assertTrue(true)
    }

    /* ***** */

    @Test
    fun b0_getIntValueAsInt() {
        val user = User()

        stream.

    }

}
