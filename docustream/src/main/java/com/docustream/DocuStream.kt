package com.docustream

import android.content.Context
import android.support.annotation.VisibleForTesting
import android.util.Log
import com.docustream.encryption.Scrambler
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import java.io.BufferedReader
import java.io.BufferedWriter
import java.io.File
import java.io.FileReader
import java.io.FileWriter
import java.nio.charset.StandardCharsets


private const val LOG_TAG = "DocuStream"
private const val DEFAULT_DATA = "{}"

/**
 * session: tokens. a few standard fields backed up with an optional model
 * data: any model structure you want. schemaless in json
 *
 * documentstream: {
 *   session<T>: {}
 *   data<T> {}
 * }
 *
 * TODO:
 * - session storage (make use of common package)
 * - Encryption;
 *      - Keystore API 18+
 * - migration (versioning?)
 * - listeners -> what type? non-invasive method?
 *
 * Done:
 * - File location (should it be optional? /data/data/[package]/files/[fileName])
 * - Encryption;
 *      - internal storage (private + no permissions)
 * Created by Killian on 23/01/2018.
 */
class DocuStream<T : Any>(
        context: Context,
        private val directory: File = context.filesDir,
        private val rootType: Class<T>,
        private val fileName: String = rootType.simpleName,
        private val cipher: Scrambler? = null) {

    private val gson: Gson by lazy { GsonBuilder().disableHtmlEscaping().create() }

    init {
        // Make sure we're dealing with application context
        val applicationContext = context.applicationContext
        if (context != applicationContext) {
            throw IllegalArgumentException("Context must be of application!")
        }

        //Thread.setDefaultUncaughtExceptionHandler(this)
    }

    private fun getFile(): File {
        //Log.i(LOG_TAG, "getFile($fileName)")

        val file = File(directory, fileName)
        val exists = file.exists()
        //Log.v(LOG_TAG, "file [$fileName] exists: $exists")

        if (!exists) {
            file.createNewFile()
            //Log.v(LOG_TAG, "file [${file.name}] created.")

            // default data depends (strangely enough) on encryption
            if (cipher != null) {
                val vector = cipher.generateVector()
                val encryptedDefault = cipher.encrypt(DEFAULT_DATA, vector)
                //Log.v(LOG_TAG, "writing encrypted default data [$encryptedDefault]")
                file.writeText(encryptedDefault)
                cipher.setVector(vector)
            } else {
                //Log.v(LOG_TAG, "writing plain default data [$DEFAULT_DATA]")
                file.writeText(DEFAULT_DATA)
            }
        }

        return file
    }

    private fun getWritableFile(): BufferedWriter {
        val file = getFile()
        return BufferedWriter(FileWriter(file))
    }

    private fun getReadableFile(): BufferedReader {
        val file = getFile()
        return BufferedReader(FileReader(file))
    }

    /**
     * Get the data from the top level element defined in the constructor.
     */
    fun getData(): T {
        Log.i(LOG_TAG, "getData($fileName)")
        val reader = getReadableFile()

        if (cipher != null) {
            val encryptedJson = getFileContents()
            val vector = cipher.getVector()
            val rawJson = cipher.decrypt(encryptedJson, vector)
            return gson.fromJson(rawJson, rootType)
        }

        //Log.d(LOG_TAG, "reader: $reader")
        //Log.d(LOG_TAG, "rootType: $rootType")

        //val raw = getFileContents()
        //Log.v(LOG_TAG, "raw: $raw")

        return gson.fromJson(reader, rootType)
    }

    /**
     * Set the data to be stored.
     */
    fun setData(data: T) {
        Log.i(LOG_TAG, "setData($fileName)")
        val writer = getWritableFile()

        if (cipher != null) {
            val rawJson = gson.toJson(data)
            val vector = cipher.generateVector()
            val encryptedJson = cipher.encrypt(rawJson, vector)
            writer.write(encryptedJson)
            cipher.setVector(vector)
        } else {
            gson.toJson(data, writer)
        }

        writer.close()
    }

    /**
     * Reset the file by removing it. This will delete all settings.
     */
    fun reset(): Int {
        Log.i(LOG_TAG, "reset()")
        val file = getFile()
        val fileRemoved = file.delete()
        Log.w(LOG_TAG, "file [${file.name}] deleted. success = $fileRemoved")

        if (cipher == null) {
            Log.v(LOG_TAG, "No cipher.")
        } else {
            return cipher.reset()
        }

        return if (fileRemoved) 1 else 0
    }

    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    fun getFileContents(): String {
        val file = getFile()

        val builder = StringBuilder()
        for (line in file.readLines(StandardCharsets.UTF_8)) {
            builder.append(line)
        }

        return builder.toString()
    }

}