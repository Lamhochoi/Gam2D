package com.example.game2d.entities

import android.graphics.Bitmap

data class FallingObject(
    var x: Float,
    var y: Float,
    var size: Int,
    var speed: Float,
    var bitmap: Bitmap? = null,
    var active: Boolean = false
)
