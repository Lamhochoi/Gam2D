package com.example.game2d

import android.content.Intent
import android.os.Bundle
import android.view.animation.AnimationUtils
import android.widget.Button
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import android.view.animation.Animation
import android.view.animation.LinearInterpolator

class IntroActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_intro)

        val logoImage = findViewById<ImageView>(R.id.logoImage)  // đổi sang ImageView
        val hudRings = findViewById<ImageView>(R.id.hudRings)
        val hudGlow = findViewById<ImageView>(R.id.hudGlow)
        val btnStart = findViewById<Button>(R.id.btnStart)
        // Chỉ xoay vòng HUD
        val rotateAnim = AnimationUtils.loadAnimation(this, R.anim.rotate_ring)
        hudRings.startAnimation(rotateAnim)

// Nếu muốn glow cũng xoay ngược chiều cho đẹp
        val rotateReverse = AnimationUtils.loadAnimation(this, R.anim.rotate_ring).apply {
            duration = 12000  // chậm hơn
            repeatCount = Animation.INFINITE
            interpolator = LinearInterpolator()
        }
        hudGlow.startAnimation(rotateReverse)

        // Animation logo
        val anim = AnimationUtils.loadAnimation(this, R.anim.logo_anim)
        logoImage.startAnimation(anim)   // chạy animation cho ảnh

        // Click Start → vào MainActivity
        btnStart.setOnClickListener {
            it.startAnimation(AnimationUtils.loadAnimation(this, R.anim.button_click))
            // sau animation -> bắt đầu game (ví dụ)
            it.postDelayed({
                val intent = Intent(this, MainActivity::class.java)
                startActivity(intent)
            }, 220) // chờ animation ngắn (tùy chỉnh)
        }
    }
}
