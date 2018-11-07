package com.afrogleap.docustream

import android.support.test.InstrumentationRegistry
import android.support.test.runner.AndroidJUnit4
import android.util.Log
import com.afrogleap.docustream.encryption.DataCipher
import com.afrogleap.docustream.model.Nullable
import com.afrogleap.docustream.model.Simple
import com.google.gson.Gson
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.io.PrintWriter

/**
 * Tests around values being null from [DocuStream.getData]. In kotlin, this resulted in runtime issues
 * because the object is defined as <i>T</i> and not <i>T?</i>.
 *
 * Created by Killian on 12/09/2018.
 */
@RunWith(AndroidJUnit4::class)
class KotlinNullTest : BaseTest() {

    private val context = InstrumentationRegistry.getTargetContext()

    @Before
    fun setup() {
        Log.i("setup", "setup() ----------------------------------------")

        assertEquals("com.afrogleap.docustream.test", context.packageName)

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

    private fun getFile(directory: File, name: String): File {
        if (!directory.isDirectory) {
            throw IllegalArgumentException("directory param is not a directory")
        }

        for (file in directory.listFiles()) {
            if (file.nameWithoutExtension == name) {
                return file
            }
        }

        throw IllegalStateException("Could not find file [$name]")
    }

    /* ********** [ problem description ] ********** */

    @Test(expected = IllegalStateException::class)
    fun a1_kotlinNonNullException() {
        val ignore: Nullable = Gson().fromJson("", Nullable::class.java)

        // no assert
    }

    @Test
    fun a2_kotlinNonNullException() {
        val nonNullObject = Gson().fromJson("{}", Nullable::class.java)
        assertNotNull(nonNullObject)
    }

    /* ********** [ this is the solution in isolation ] ********** */

    @Test
    fun b1_kotlinNonNullException() {
        val gson = Gson()

        val nonNullObject: Nullable = try {
            gson.fromJson("", Nullable::class.java)
        } catch (e: IllegalStateException) {
            gson.fromJson("{}", Nullable::class.java)
        }

        assertNotNull(nonNullObject)
    }

    /*
    For documentation the Java version. In kotlin you cannot give an optional value to a non-optional
    param. Java doesn't know the difference between the two at compile-time.

    @Test
    public void b1_streamNonNullException() {
        DocuStream<Nullable> stream = getDocustream();

        assertNotNull(stream.getData());

        stream.setData(null); <----- This would not be possible to compile in kotlin

        // not going to end up here because of a crash
    }
     */

    /* ********** [ this is the solution implemented ] ********** */

    @Test
    fun c1_kotlinNonNullException() {
        val stream = DocuStream(context.applicationContext, rootType = Nullable::class.java)

        val initialData = stream.getData()
        assertNotNull(initialData)

        // make the stored object null (invalid because it's not going be be JSON)
        val directory = context.applicationContext.filesDir
        val file = getFile(directory, "Nullable")

        val writer = PrintWriter(file)
        writer.print("") // This means that the file has invalid contents (NOT JSON)
        writer.close()

        // verify that we end up with a valid object
        val data = stream.getData()
        assertNotNull(data)
    }

    /* ********** [ this is the solution implemented ] ********** */

    @Test
    fun d1_malformedRecovery() {
        val stream = DocuStream(context.applicationContext, rootType = Simple::class.java)

        val simple = Simple(contents = "normal valid json")
        stream.setData(simple)

        val fileContents = stream.getFileContents()
        Log.v(LOG_TAG("d1_malformedRecovery"), fileContents)

        // make the stored object null (invalid because it's not going be be JSON)
        val directory = context.applicationContext.filesDir
        val file = getFile(directory, "Simple")

        val writer = PrintWriter(file)
        writer.print("{\"calender\":{\"year\":2018,\"month\":10,\"dayOfMonthhourOfDay\":11,\"minute\":25,\"second\":32},\"contentsnormal valid json\"}") // This means that the file has invalid contents (NOT JSON)
        writer.close()

        // verify that we end up with a valid object
        val data = stream.getData()
        assertNotNull(data)
    }

}
