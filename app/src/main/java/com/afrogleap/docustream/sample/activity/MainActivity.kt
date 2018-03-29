package com.afrogleap.docustream.sample.activity

import android.app.Activity
import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import com.afrogleap.docustream.sample.R
import com.afrogleap.docustream.sample.SampleApplication
import android.widget.ImageView


private const val LOG_TAG = "MainActivity"

class MainActivity : Activity() {

    private val etName by lazy { findViewById<EditText>(R.id.activity_main_name) }
    private val etAge by lazy { findViewById<EditText>(R.id.activity_main_age) }
    private val etColor by lazy { findViewById<EditText>(R.id.activity_main_color) }
    private val ivBitmap by lazy { findViewById<ImageView>(R.id.activity_main_bitmap) }
    private val buttonSave by lazy { findViewById<Button>(R.id.activity_main_save) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val app = applicationContext as SampleApplication
        val user = app.stream.getData()

        etName.setText(user.name)
        etAge.setText(user.age.toString())
        etColor.setText(user.favoriteColor)

        Log.v(LOG_TAG, "rawContents: ${app.stream.getFileContents()}")

        // We're going to fake the bitmap selection and instead, load it from assets
        if (user.bitmap != null) {
            ivBitmap.setImageBitmap(user.bitmap)

            ivBitmap.setOnClickListener { _ ->
                // show alert to remove it
            }
        } else {
            Log.v(LOG_TAG, "does not have bitmap")
            ivBitmap.setOnClickListener { _ ->
                val logoStream = assets.open("afl_logo.JPG")
                val bitmap = BitmapFactory.decodeStream(logoStream)

                user.bitmap = bitmap
                ivBitmap.setImageBitmap(user.bitmap)
            }
        }

        buttonSave.setOnClickListener { _ ->
            user.name = etName.text.toString()

            val ageInput = etAge.text.toString()
            if (ageInput.isEmpty()) {
                user.age = 0
            } else {
                user.age = ageInput.toInt()
            }

            user.favoriteColor = etColor.text.toString()

            app.stream.setData(user)

            val rawContents = app.stream.getFileContents()
            Log.v(LOG_TAG, "rawContents: $rawContents")
        }

    }
}
