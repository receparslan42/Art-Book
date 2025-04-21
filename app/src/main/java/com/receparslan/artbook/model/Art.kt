package com.receparslan.artbook.model

import android.graphics.Bitmap

// This class is used to store the data of the art.
class Art {

    var id: Int? = null
    lateinit var name: String
    lateinit var artistName: String
    lateinit var date: String
    var image: Bitmap? = null
}