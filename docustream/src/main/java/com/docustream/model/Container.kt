package com.docustream.model

/**
 * Created by Killian on 26/02/2018.
 */
data class Container(
        val title: String = "default",
        var priority: Priority = Priority.LOW,
        var items: MutableList<SubItem>? = null
)