package com.example.game2d.entities

import android.graphics.Bitmap

data class Player(
    var x: Float = 0f,
    var y: Float = 0f,
    var size: Int = 50,
    var hp: Int = 5,
    var maxHp: Int = 5,
    var lastShot: Long = 0,
    var shotDelay: Long = 500,
    var bitmap: Bitmap? = null,
    var coins: Int = 0,
    // Thêm mới cho power-ups
    var shield: Int = 0,  // Lá chắn: số lượng damage có thể chịu (ví dụ 3)
    var doubleShotActive: Boolean = false,
    var doubleShotEndTime: Long = 0,
    var isInvincible: Boolean = false,
    var invincibilityEndTime: Long = 0
) {
    fun reset(screenW: Int, screenH: Int) {
        hp = maxHp
        x = screenW / 2f - size / 2
        y = screenH * 0.7f
        lastShot = 0
        // Reset power-ups
        shield = 0
        doubleShotActive = false
        doubleShotEndTime = 0
        isInvincible = false
        invincibilityEndTime = 0
    }
}