package com.afrogleap.docustream.model

import java.util.Calendar

/**
 * Created by Killian on 26/02/2018.
 */
data class Simple(
    var contents: String = "1234567890",
    val calender: Calendar = Calendar.getInstance()
)