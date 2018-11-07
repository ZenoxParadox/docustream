package com.afrogleap.docustream

import android.support.test.InstrumentationRegistry
import android.support.test.runner.AndroidJUnit4
import android.util.Log
import com.afrogleap.docustream.encryption.DataCipher
import com.afrogleap.docustream.model.Example
import com.afrogleap.docustream.model.Simple
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.TimeUnit
import kotlin.concurrent.thread


/**
 * tests
 *
 * Created by Killian on 24/01/2018.
 */
@RunWith(AndroidJUnit4::class)
class InterruptedStreamTest : BaseTest() {

    private val context = InstrumentationRegistry.getTargetContext()

    @Before
    fun setup() {
        Log.i("setup", "setup() ----------------------------------------")

        assertEquals("com.afrogleap.docustream.test", context.packageName)

//        // Clear old settings
//        val storageExample = DocuStream(context.applicationContext, rootType = Example::class.java)
//        val storageExampleReset = storageExample.reset()
//        Log.d("setup", "storageExampleReset: $storageExampleReset")
//
//        // Clear old settings
//        val storageContainer = DocuStream(context.applicationContext, rootType = Container::class.java)
//        val containerReset = storageContainer.reset()
//        Log.d("setup", "containerReset: $containerReset")
//
        // Remove all files
        val directory = context.filesDir
        Log.v("setup", "directory [${directory.name}]")
        for (file in directory.listFiles()) {
            val name = file.name
            val removed = file.delete()
            Log.v("setup", "file [$name] removed [$removed]")
        }

        val cipher = DataCipher(context.applicationContext)
        val removed = cipher.reset()
        Log.d("setup", "cipher reset. Output: [$removed]")

        Log.i("setup", "---------------------------------------- setup()")
    }

    /* ********** [initialize] ********** */

    @Test
    fun a1_writeFromDifferentThreads() {
        Log.i(LOG_TAG("a1_writeFromDifferentThreads"), "---------------------------------------- begin")

        val lock = Any()
        val storage = DocuStream(context.applicationContext, rootType = Example::class.java)
        val fillerData = Simple(contents = "Example example example example example example example example example example example example example example example example example example example example example example example example")

        Log.d(LOG_TAG("a1_writeFromDifferentThreads"), "---")

        for (i in 0..10) {
            val first = thread(start = false, name = "first thread", block = {
                val data = Example(name = "first", variable = "first first first first first first first first first first first", simple = fillerData)
                storage.setData(data)
            })

            val second = thread(start = false, name = "second thread", block = {
                val data = Example(name = "second", variable = "second second second second second second second second second second", simple = fillerData)
                storage.setData(data)
            })

            synchronized(lock, block = {
                first.start()
                second.start()
                Log.v(LOG_TAG("a1_writeFromDifferentThreads"), "done synchronized block")
            })

            Thread.sleep(TimeUnit.SECONDS.toMillis(3))

            Log.d(LOG_TAG("a1_writeFromDifferentThreads"), "---")

            val data = storage.getFileContents()
            Log.d(LOG_TAG("a1_writeFromDifferentThreads"), "data: $data")

            val realObject = storage.getData()
            Log.d(LOG_TAG("a1_writeFromDifferentThreads"), "data: $data")
        }

        Log.i(LOG_TAG("a1_writeFromDifferentThreads"), "---------------------------------------- end")
    }

}