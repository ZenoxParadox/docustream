package com.docustream.model

/**
 * Created by Killian on 26/02/2018.
 */
data class Example(
    val name: String = "default",
    val age: Int = -1,
    var variable: String = "this value is from the default file (unchanged and untouched)"
)