package com.afrogleap.docustream.sample

import android.app.Application
import android.graphics.Bitmap
import android.util.Log
import com.afrogleap.docustream.DocuStream
import com.afrogleap.docustream.encryption.DataCipher
import com.afrogleap.docustream.sample.model.User
import com.docustream.common.adapter.BitmapAdapter
import com.google.gson.GsonBuilder

private const val LOG_TAG = "SampleApplication"

/**
 * Sample application class. Create instance for the library.
 *
 * Created by Killian on 19/01/2018.
 */
class SampleApplication : Application() {

    lateinit var stream: DocuStream<User>

    override fun onCreate() {
        super.onCreate()

        val gsonBuilder = GsonBuilder()
        gsonBuilder.registerTypeAdapter(Bitmap::class.java, BitmapAdapter())
        gsonBuilder.disableHtmlEscaping()

        val cipher = DataCipher(this)
        try {
            stream = DocuStream(this, cipher = cipher, builder = gsonBuilder, rootType = User::class.java)
        } catch (e: Exception){
            Log.w(LOG_TAG, e.message, e)
            stream.reset()
        }

    }

}