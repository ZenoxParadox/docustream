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
 * Document storage with any logic on your side. It's basically a JSON (de-)serializer that
 * encrypts itself using RSA (for keys) and AES (for the data) encryption.
 *
 * TODO:
 * - migration (versioning?)
 *      - OS Update; all of the sudden the RSA master key can differ.
 * - listeners -> what type? non-invasive method?
 *
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
        if (context != context.applicationContext) {
            throw IllegalArgumentException("Context must be of application!")
        }
    }

    private fun getFile(): File {
        val file = File(directory, fileName)
        val exists = file.exists()

        if (!exists) {
            file.createNewFile()

            // default data depends (strangely enough) on encryption
            if (cipher != null) {
                val vector = cipher.generateVector()
                val encryptedDefault = cipher.encrypt(DEFAULT_DATA, vector)
                file.writeText(encryptedDefault)
                cipher.setVector(vector)
            } else {
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
     * Set the data to be stored.
     */
    fun setData(data: T) {
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
     * Get the data from the top level element defined in the constructor.
     */
    fun getData(): T {
        val reader = getReadableFile()

        if (cipher != null) {
            val encryptedJson = getFileContents()
            val rawJson = cipher.decrypt(encryptedJson)
            return gson.fromJson(rawJson, rootType)
        }

        return gson.fromJson(reader, rootType)
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