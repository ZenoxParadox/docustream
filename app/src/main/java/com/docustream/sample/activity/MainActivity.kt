package com.docustream.sample.activity

import android.app.Activity
import android.os.Bundle
import com.docustream.R

class MainActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        //KeyChain.choosePrivateKeyAlias("alias", this, KeyProperties.BLOCK_MODE_ECB)

        //val ks = KeyStore.getInstance()


    }
}
