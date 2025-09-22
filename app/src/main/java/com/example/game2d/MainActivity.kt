package com.example.game2d

import android.content.pm.ActivityInfo
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Ép dọc cho MainActivity
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT

        setContentView(R.layout.activity_main)

        val btnMars = findViewById<Button>(R.id.btnEasy)     // Sao Hỏa
        val btnMercury = findViewById<Button>(R.id.btnMedium) // Sao Thủy
        val btnSaturn = findViewById<Button>(R.id.btnHard)   // Sao Thổ

        btnMars.setOnClickListener { startGame("MARS") }
        btnMercury.setOnClickListener { startGame("MERCURY") }
        btnSaturn.setOnClickListener { startGame("SATURN") }
    }

    private fun startGame(planet: String) {
        val intent = Intent(this, GameActivity::class.java)
        intent.putExtra("LEVEL", planet)
        startActivity(intent)
    }
}
