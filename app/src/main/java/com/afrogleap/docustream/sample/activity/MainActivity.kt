package com.afrogleap.docustream.sample.activity

import android.app.Activity
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import com.afrogleap.docustream.sample.R
import com.afrogleap.docustream.sample.SampleApplication

private const val LOG_TAG = "MainActivity"

class MainActivity : Activity() {

    private val etName by lazy { findViewById<EditText>(R.id.activity_main_name) }
    private val etAge by lazy { findViewById<EditText>(R.id.activity_main_age) }
    private val etColor by lazy { findViewById<EditText>(R.id.activity_main_color) }
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

        buttonSave.setOnClickListener { v ->
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
