package com.afrogleap.docustream.encryption

/**
 * container for keys. You might add any amount of keys that will be stored.
 *
 * Created by Killian on 22/03/2018.
 */
data class Keys(
        var secret: String? = null,
        var vector: String? = null
)