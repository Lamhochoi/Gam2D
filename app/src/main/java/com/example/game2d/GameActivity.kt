package com.example.game2d

import android.content.Intent
import android.content.pm.ActivityInfo
import android.os.Bundle
import android.util.Log
import android.view.Window
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.ImageButton
import androidx.appcompat.app.AppCompatActivity
import com.example.game2d.core.GameView
import com.example.game2d.managers.MusicManager
import com.example.game2d.managers.SoundManager

class GameActivity : AppCompatActivity() {

    private lateinit var gameView: GameView
    private lateinit var btnMusic: ImageButton
    private lateinit var btnSound: ImageButton

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d("GameActivity", "Setting layout: R.layout.activity_game")
        setContentView(R.layout.activity_game)

        SoundManager.init(this)

        val planet = intent.getStringExtra("LEVEL") ?: "MARS"
        gameView = when (planet) {
            "MERCURY" -> MercuryGameView(this)
            "SATURN" -> SaturnGameView(this)
            else -> MarsGameView(this)
        }

        val root = findViewById<FrameLayout>(R.id.game_container) ?: run {
            Log.e("GameActivity", "game_container not found")
            finish()
            return
        }
        root.removeAllViews()
        root.addView(gameView)

        gameView.onRestart = { runOnUiThread { recreate() } }
        gameView.onBackToMenu = {
            runOnUiThread {
                startActivity(Intent(this, MainActivity::class.java))
                finish()
            }
        }

        btnMusic = findViewById<ImageButton>(R.id.btnMusic) ?: run {
            Log.e("GameActivity", "btnMusic not found")
            return
        }
        btnSound = findViewById<ImageButton>(R.id.btnSound) ?: run {
            Log.e("GameActivity", "btnSound not found")
            return
        }

        MusicManager.init(this)
        MusicManager.setMusicEnabled(true)
        MusicManager.start(this)

        btnMusic.setOnClickListener {
            val enabled = !MusicManager.isMusicEnabled()
            MusicManager.setMusicEnabled(enabled)
            if (enabled) MusicManager.start(this) else MusicManager.pause()
            btnMusic.setImageResource(
                if (enabled) R.drawable.ic_music_on else R.drawable.ic_music_off
            )
        }

        btnSound.setOnClickListener {
            val enabled = !SoundManager.isSoundEnabled()
            SoundManager.setSoundEnabled(enabled)
            btnSound.setImageResource(
                if (enabled) R.drawable.ic_sound_on else R.drawable.ic_sound_off
            )
        }
    }

    override fun onPause() {
        super.onPause()
        gameView.pause()
        MusicManager.pause()
    }

    override fun onResume() {
        super.onResume()
        gameView.resume()
        if (MusicManager.isMusicEnabled()) {
            MusicManager.start(this)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        gameView.pause()
        MusicManager.stop()
    }
}