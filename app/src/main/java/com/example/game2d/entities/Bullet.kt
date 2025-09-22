package com.example.game2d.entities

import android.graphics.Bitmap

data class Bullet(
    var x: Float = 0f,
    var y: Float = 0f,
    var size: Float = 20f,
    var speed: Float = 20f,
    var angle: Float = 270f,
    var bitmap: Bitmap? = null,
    var active: Boolean = false
)
