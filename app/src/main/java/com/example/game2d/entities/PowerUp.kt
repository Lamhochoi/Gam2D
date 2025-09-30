package com.example.game2d.entities

import android.graphics.Bitmap

enum class PowerUpType {
    HEAL, SHIELD, DOUBLE_SHOT
}

data class PowerUp(
    var x: Float = 0f,
    var y: Float = 0f,
    var size: Int = 50,
    var speed: Float = 5f,
    var type: PowerUpType = PowerUpType.HEAL,
    var bitmap: Bitmap? = null,
    var active: Boolean = false
)