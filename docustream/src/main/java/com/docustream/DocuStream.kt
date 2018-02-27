package com.docustream

import android.content.Context
import android.os.Environment
import android.os.storage.StorageManager
import android.support.annotation.VisibleForTesting
import android.util.Log
import com.google.gson.Gson
import java.io.BufferedReader
import java.io.BufferedWriter
import java.io.File
import java.io.FileReader
import java.io.FileWriter

private const val LOG_TAG = "DocuStream"
private const val DEFAULT_FILENAME = "document_storage"
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
 * Created by Killian on 23/01/2018.
 */
class DocuStream<T : Any>(
        context: Context,
        private val directory: File = context.filesDir,
        private val fileName: String = DEFAULT_FILENAME,
        private val rootType: Class<T>) {

    private val gson by lazy { Gson() }

    private lateinit var data: T

    init {
        // Make sure we're dealing with application context
        val applicationContext = context.applicationContext
        if (context != applicationContext) {
            throw IllegalArgumentException("Context must be of application!")
        }
    }

    private fun getFile(): File {
        val file = File(directory, fileName)
        if (!file.exists()) {
            file.createNewFile()
            file.writeText(DEFAULT_DATA)
        }

        return file
    }

    private fun getWritableFile(): BufferedWriter {
        val file = getFile()
        return BufferedWriter(FileWriter(file))
    }

    private fun getReadableFile(): BufferedReader {
        val fileReader = FileReader(getFile())
        return BufferedReader(fileReader)
    }

    private fun save() {
        val writer = getWritableFile()
        gson.toJson(data, writer)
        writer.close()
    }

    /**
     * Get the data from the top level element defined in the constructor.
     */
    fun getData(): T {
        val reader = getReadableFile()
        return gson.fromJson(reader, rootType)
    }

    /**
     * Set the data to be stored.
     */
    fun setData(data: T) {
        this.data = data
        save()
    }

    /**
     * Reset the file by removing it. This will delete all settings.
     */
    fun reset(): Boolean {
        val file = getFile()
        val success = file.delete()
        Log.w(LOG_TAG, "file [${file.name}] deleted. success = $success")
        return success
    }

}