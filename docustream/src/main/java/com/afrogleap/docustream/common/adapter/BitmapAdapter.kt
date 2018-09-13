package com.afrogleap.docustream.common.adapter

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import android.util.JsonToken
import com.google.gson.TypeAdapter
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonWriter
import java.io.ByteArrayOutputStream

/**
 * Gson Type Adapter for a [Bitmap] type.
 *
 * Created by Killian on 28/03/2018.
 */
class BitmapAdapter : TypeAdapter<Bitmap>() {

    override fun write(writer: JsonWriter, bitmap: Bitmap?) {
        if (bitmap == null) {
            writer.nullValue()
            return
        }

        val stream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, stream)

        val encoded = Base64.encodeToString(stream.toByteArray(), Base64.NO_WRAP)
        writer.value(encoded)
    }

    override fun read(reader: JsonReader): Bitmap? {
        if (reader.peek() == JsonToken.NULL) {
            reader.nextNull()
            return null
        }

        val decodedBytes = Base64.decode(reader.nextString(), Base64.NO_WRAP)
        return BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)
    }

}