package com.example.game2d.entities

import android.graphics.Bitmap

class Coin(
    var x: Float,
    var y: Float,
    var size: Int,
    var speed: Float = 10f,
    var bitmap: Bitmap? = null,
    var active: Boolean = false,
    var value: Int = 1 // giá trị đồng tiền
)
