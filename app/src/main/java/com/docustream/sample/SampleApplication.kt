package com.docustream.sample

import android.app.Application
import android.util.Log
import com.docustream.DocuStream
import com.docustream.encryption.DataCipher
import com.docustream.sample.model.User


private const val ALIAS = "example"
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
        stream = DocuStream(this, cipher = cipher, rootType = User::class.java)
        try {
            stream.getData()
        } catch (e: Exception){
            Log.w(LOG_TAG, e.message, e)
            stream.reset()
        }

    }

}