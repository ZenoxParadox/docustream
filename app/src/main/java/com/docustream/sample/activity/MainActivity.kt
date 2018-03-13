package com.docustream.sample.activity

import android.app.Activity
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import com.docustream.sample.R

private const val LOG_TAG = "MainActivity"

class MainActivity : Activity() {

    private val etName by lazy { findViewById<EditText>(R.id.activity_main_name) }
    private val etAge by lazy { findViewById<EditText>(R.id.activity_main_age) }
    private val etColor by lazy { findViewById<EditText>(R.id.activity_main_color) }
    private val buttonSave by lazy { findViewById<Button>(R.id.activity_main_save) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

//        val app = applicationContext as SampleApplication
//        val user = app.stream.getData()
//
//        etName.setText(user.name)
//        etAge.setText(user.age.toString())
//        etColor.setText(user.favoriteColor)
//
//        buttonSave.setOnClickListener { v ->
//            user.name = etName.text.toString()
//            user.age = etAge.text.toString().toInt()
//            user.favoriteColor = etColor.text.toString()
//
//            app.stream.setData(user)
//        }


    }
}
