package com.afrogleap.docustream.common

import android.support.test.runner.AndroidJUnit4
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.util.Calendar

/**
 * Created by Killian on 27/02/2018.
 */
@RunWith(AndroidJUnit4::class)
class AuthTokenTest {


    @Before
    fun setup() {
        // -
    }

    /* ********** [initialize] ********** */

    @Test
    fun a1_minimalConstructor() {
        val token = AuthToken("", "")
        assertNotNull(token)
    }

    /* ********** [expire] ********** */

    @Test
    fun b1_expireInAnHour() {
        val token = AuthToken("", "", expires_in = 3600)

        val expireCalendar = token.expireMoment
        val now = Calendar.getInstance()

        assertTrue(expireCalendar.after(now))
        assertTrue(expireCalendar.timeInMillis >= now.timeInMillis)
        assertEquals(3600000, expireCalendar.timeInMillis - now.timeInMillis)
    }

}