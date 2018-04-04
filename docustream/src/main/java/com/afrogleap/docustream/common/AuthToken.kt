package com.afrogleap.docustream.common

import java.util.Calendar
import java.util.concurrent.TimeUnit

/**
 * https://tools.ietf.org/html/rfc6749
 *
 * Created by Killian on 27/02/2018.
 */
data class AuthToken(
        var access_token: String,
        var refresh_token: String,
        private var expires_in: Long = 0 // in seconds
) {

    val expireMoment: Calendar

    init {
        val calendar = Calendar.getInstance()
        var now = calendar.timeInMillis
        now += TimeUnit.MILLISECONDS.convert(expires_in, TimeUnit.SECONDS)
        calendar.timeInMillis = now

        expireMoment = calendar
    }
}

