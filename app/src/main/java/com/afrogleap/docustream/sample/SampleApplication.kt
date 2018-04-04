package com.afrogleap.docustream.sample

import android.app.Application
import android.util.Log
import com.afrogleap.docustream.DocuStream
import com.afrogleap.docustream.encryption.DataCipher
import com.afrogleap.docustream.sample.model.User

private const val LOG_TAG = "SampleApplication"

/**
 * df
 *
 * Created by Killian on 19/01/2018.
 */
class SampleApplication : Application() {

    lateinit var stream: DocuStream<User>

    override fun onCreate() {
        super.onCreate()

        val cipher = DataCipher(this)
        try {
            stream = DocuStream(this, cipher = cipher, rootType = User::class.java)
            stream.getData()
        } catch (e: Exception){
            Log.w(LOG_TAG, e.message, e)
            stream.reset()
        }

    }

}