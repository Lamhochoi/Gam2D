package com.example.game2d.entities

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Color
import android.graphics.RectF
import android.graphics.BlurMaskFilter

class Explosion(
    var x: Float,
    var y: Float,
    var active: Boolean = false
) {
    private var frames: List<Bitmap> = emptyList()
    private var frameIndex = 0
    private var timer = 0f
    private var size = 0f
    private var alpha = 255
    private var scale = 1f

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)

    // thời lượng từng frame (giây)
    private val frameDurations = listOf(0.15f, 0.15f, 0.12f, 0.1f, 0.15f, 0.3f)

    fun setFrames(frames: List<Bitmap>, size: Float) {
        this.frames = frames
        this.size = size
        frameIndex = 0
        timer = 0f
        alpha = 255
        scale = 0.6f
        active = true
    }

    fun update(delta: Float) {
        if (!active) return

        timer += delta
        if (frameIndex < frameDurations.size) {
            val duration = frameDurations[frameIndex]
            if (timer > duration) {
                timer -= duration
                frameIndex++
                if (frameIndex >= frames.size) {
                    active = false
                    return
                }
            }
        }

        // scale easing: tăng nhanh ban đầu, chậm dần
        scale += (0.04f - scale * 0.002f) * (delta * 60f)

        // fade-out từ frame áp chót
        if (frameIndex >= frames.size - 2) {
            alpha -= (5f * delta * 60f).toInt()
            if (alpha <= 0) active = false
        }
    }

    fun draw(canvas: Canvas) {
        if (!active || frameIndex >= frames.size) return

        val bmp = frames[frameIndex]
        paint.alpha = alpha

        val half = (size * scale) / 2f
        val dst = RectF(
            x - half, y - half,
            x + half, y + half
        )

        // vẽ bitmap
        canvas.drawBitmap(bmp, null, dst, paint)

        // vẽ halo sáng mờ
        val haloPaint = Paint().apply {
            color = Color.YELLOW
            alpha = (alpha * 0.4f).toInt()
            maskFilter = BlurMaskFilter(half * 0.8f, BlurMaskFilter.Blur.NORMAL)
        }
        canvas.drawCircle(x, y, half * 0.9f, haloPaint)
    }
}
