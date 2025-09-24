package com.example.game2d

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.FrameLayout
import android.widget.ImageButton
import androidx.appcompat.app.AppCompatActivity
import com.example.game2d.core.GameView
import com.example.game2d.managers.MusicManager
import com.example.game2d.managers.PlayerDataManager
import com.example.game2d.managers.SoundManager

class GameActivity : AppCompatActivity() {

    private lateinit var gameView: GameView
    private lateinit var btnMusic: ImageButton
    private lateinit var btnSound: ImageButton
    private lateinit var btnPause: ImageButton
    private var progressSaved = false

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
        gameView.tvCoin = findViewById(R.id.tvCoin)

        val root = findViewById<FrameLayout>(R.id.game_container) ?: run {
            Log.e("GameActivity", "game_container not found")
            finish()
            return
        }
        root.addView(gameView, 0)

        gameView.player.coins = 0
        progressSaved = false
        Log.d("GameActivity", "Game started: planet=$planet, coins reset=0")

        gameView.onGameEnd = {
            runOnUiThread {
                saveProgress()
            }
        }

        gameView.onRestart = { runOnUiThread { recreate() } }

        gameView.onBackToMenu = {
            runOnUiThread {
                saveProgress()
                val resultIntent = Intent().apply {
                    putExtra("COIN_UPDATED", true)
                }
                setResult(RESULT_OK, resultIntent)
                finish()
            }
        }

        btnMusic = findViewById(R.id.btnMusic)
        btnSound = findViewById(R.id.btnSound)
        btnPause = findViewById(R.id.btnPause)

        MusicManager.init(this)
        MusicManager.setMusicEnabled(true)
        MusicManager.start(this)

        btnMusic.setOnClickListener {
            val enabled = !MusicManager.isMusicEnabled()
            MusicManager.setMusicEnabled(enabled)
            if (enabled) MusicManager.start(this) else MusicManager.pause()
            btnMusic.setImageResource(if (enabled) R.drawable.ic_music_on else R.drawable.ic_music_off)
        }

        btnSound.setOnClickListener {
            val enabled = !SoundManager.isSoundEnabled()
            SoundManager.setSoundEnabled(enabled)
            MusicManager.setMusicEnabled(enabled)
            if (enabled) MusicManager.start(this) else MusicManager.pause()
            btnSound.setImageResource(if (enabled) R.drawable.ic_sound_on else R.drawable.ic_sound_off)
            btnMusic.setImageResource(if (enabled) R.drawable.ic_music_on else R.drawable.ic_music_off)
        }

        btnPause.setOnClickListener {
            if (gameView.gameState == GameView.GameState.RUNNING) {
                gameView.pause()
                gameView.gameState = GameView.GameState.PAUSED
                btnPause.setImageResource(R.drawable.ic_play)
            } else if (gameView.gameState == GameView.GameState.PAUSED) {
                gameView.resume()
                gameView.gameState = GameView.GameState.RUNNING
                btnPause.setImageResource(R.drawable.ic_pause)
            }
        }
    }

    override fun onBackPressed() {
        if (!progressSaved && gameView.player.coins > 0) {
            saveProgress()
            Log.d("GameActivity", "onBackPressed: saved coins before back")
        }
        super.onBackPressed()
    }

    override fun onPause() {
        super.onPause()
        gameView.pause()
        MusicManager.pause()
        if (!progressSaved && gameView.player.coins > 0) {
            saveProgress()
            Log.d("GameActivity", "onPause: saved coins (any state)")
        }
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

    private fun saveProgress() {
        if (progressSaved) {
            Log.d("GameActivity", "saveProgress(): already saved -> skipping")
            return
        }
        try {
            val earned = gameView.player.coins
            val oldTotal = PlayerDataManager.getCoins(this)
            Log.d("GameActivity", "saveProgress(): earned=$earned, oldTotal=$oldTotal")
            if (earned > 0) {
                PlayerDataManager.addCoins(this, earned)
                val newTotal = PlayerDataManager.getCoins(this)
                Log.d("GameActivity", "Saved: newTotal=$newTotal")
                PlayerDataManager.debugPrefs(this) // In SharedPreferences
            } else {
                Log.d("GameActivity", "No coins to save")
            }
            progressSaved = true
        } catch (e: Exception) {
            Log.e("GameActivity", "saveProgress failed: ${e.message}", e)
        }
    }
}