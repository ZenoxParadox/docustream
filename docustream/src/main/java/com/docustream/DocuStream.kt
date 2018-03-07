package com.docustream

import android.content.Context
import android.support.annotation.VisibleForTesting
import android.util.Log
import com.docustream.encryption.DataCipher
import com.google.gson.Gson
import java.io.BufferedReader
import java.io.BufferedWriter
import java.io.File
import java.io.FileReader
import java.io.FileWriter
import java.nio.charset.StandardCharsets


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
        private val fileName: String = DEFAULT_FILENAME,
        private val cipher: DataCipher? = null,
        private val rootType: Class<T>) {

    private val gson: Gson by lazy { Gson() }

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

            // default data depends (strangely enough) on encryption
            if (cipher != null) {
                val bytes = cipher.generateVectorBytes()
                val encryptedDefault = cipher.encrypt(DEFAULT_DATA, bytes)
                file.writeText(encryptedDefault)
                cipher.saveVectorBytes(bytes)
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
     * Get the data from the top level element defined in the constructor.
     */
    fun getData(): T {
        val reader = getReadableFile()

        if (cipher != null) {
            val bytes = cipher.getVectorBytes()

            val encryptedJson = getFileContents()
            val rawJson = cipher.decrypt(encryptedJson, bytes)
            return gson.fromJson(rawJson, rootType)
        }

        return gson.fromJson(reader, rootType)
    }

    /**
     * Set the data to be stored.
     */
    fun setData(data: T) {
        val writer = getWritableFile()

        if (cipher != null) {
            val bytes = cipher.generateVectorBytes()

            val rawJson = gson.toJson(data)
            val encryptedJson = cipher.encrypt(rawJson, bytes)
            writer.write(encryptedJson)

            cipher.saveVectorBytes(bytes)
        } else {
            gson.toJson(data, writer)
        }

        writer.close()
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