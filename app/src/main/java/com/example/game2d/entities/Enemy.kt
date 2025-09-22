package com.example.game2d.entities

import android.graphics.Bitmap

data class Enemy(
    var x: Float = 0f,
    var y: Float = 0f,
    var size: Int = 10,
    var speed: Float = 2f,
    var hp: Int = 5,
    var maxHp: Int = 5,
    var shotDelay: Long = 1000,
    var lastShot: Long = 0,
    var isBoss: Boolean = false,
    var bitmap: Bitmap? = null,
    var active: Boolean = false,
    var directionX: Int = 1
)
