package com.docustream.sample

import android.app.Application
import android.util.Log
import com.docustream.DocuStream
import com.docustream.model.Example

private const val LOG_TAG = "SampleApplication"
/**
 * Created by Killian on 19/01/2018.
 */
class SampleApplication : Application() {

    lateinit var stream: DocuStream<Example>

    override fun onCreate() {
        super.onCreate()

        stream = DocuStream(this, rootType = Example::class.java)
        val instance = stream.getData()

        // default value
        Log.i(LOG_TAG, "before -> ${instance.variable}")
        instance.variable= "new"

        // changed instance state
        Log.i(LOG_TAG, "before -> ${instance.variable}")

        stream.setData(instance)

        // retrieved state
        val savedInstance = stream.getData()
        Log.i(LOG_TAG, "after -> ${savedInstance.variable}")
    }

}