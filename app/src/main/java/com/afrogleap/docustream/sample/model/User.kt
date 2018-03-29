package com.afrogleap.docustream.sample.model

import android.graphics.Bitmap

/**
 * TODO
 *
 * Created by Killian on 19/01/2018.
 */
data class User(
        var name: String = "",
        var age: Int = 0,
        var favoriteColor: String = "",
        var bitmap: Bitmap? = null
)


