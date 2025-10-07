package com.example.game2d

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.FrameLayout
import android.widget.ImageButton
import androidx.appcompat.app.AppCompatActivity
import com.example.game2d.core.ChallengeGameView
import com.example.game2d.managers.MusicManager
import com.example.game2d.managers.PlayerDataManager
import com.example.game2d.managers.SoundManager
import android.widget.TextView
import android.widget.Toast
import com.example.game2d.core.GameView

class ChallengeGameActivity : AppCompatActivity() {

    private lateinit var gameView: ChallengeGameView
    private lateinit var btnSettings: ImageButton
    private lateinit var btnMusic: ImageButton
    private lateinit var btnSound: ImageButton
    private lateinit var btnPause: ImageButton
    private var progressSaved = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d("ChallengeGameActivity", "Setting layout: R.layout.activity_game")
        setContentView(R.layout.activity_game)

        SoundManager.init(this)

        gameView = ChallengeGameView(this)
        val root = findViewById<FrameLayout>(R.id.game_container) ?: run {
            Log.e("ChallengeGameActivity", "game_container not found")
            finish()
            return
        }
        root.addView(gameView, 0)

        gameView.tvCoin = findViewById(R.id.tvCoin)
        gameView.player.coins = 0
        gameView.tvCoin?.text = "0"  // Sửa: Set "0" để khớp reset, không dùng getCoins (tổng saved)
        progressSaved = false
        Log.d("ChallengeGameActivity", "Challenge started, coins reset=0")

        gameView.onGameEnd = {
            runOnUiThread {
                saveProgress()
            }
        }

        gameView.onRestart = {
            runOnUiThread {
                recreate()
            }
        }

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

        gameView.onLeaderboard = {
            runOnUiThread {
                val intent = Intent(this, ChallengeLeaderboardActivity::class.java)
                startActivity(intent)
            }
        }

        btnSettings = findViewById(R.id.btnSettings)
        btnMusic = findViewById(R.id.btnMusic)
        btnSound = findViewById(R.id.btnSound)
        btnPause = findViewById(R.id.btnPause)

        MusicManager.init(this)
        MusicManager.setMusicEnabled(true)
        MusicManager.start(this)

        btnSettings.setOnClickListener {
            Log.d("ChallengeGameActivity", "Settings button clicked")
            Toast.makeText(this, "Chưa hỗ trợ cài đặt!", Toast.LENGTH_SHORT).show()
        }

        btnMusic.setOnClickListener {
            val enabled = !MusicManager.isMusicEnabled()
            MusicManager.setMusicEnabled(enabled)
            if (enabled) MusicManager.start(this) else MusicManager.pause()
            btnMusic.setImageResource(if (enabled) R.drawable.ic_music_on else R.drawable.ic_music_off)
            Log.d("ChallengeGameActivity", "Music toggled: enabled=$enabled")
        }

        btnSound.setOnClickListener {
            val enabled = !SoundManager.isSoundEnabled()
            SoundManager.setSoundEnabled(enabled)
            MusicManager.setMusicEnabled(enabled)
            if (enabled) MusicManager.start(this) else MusicManager.pause()
            btnSound.setImageResource(if (enabled) R.drawable.ic_sound_on else R.drawable.ic_sound_off)
            btnMusic.setImageResource(if (enabled) R.drawable.ic_music_on else R.drawable.ic_music_off)
            Log.d("ChallengeGameActivity", "Sound toggled: enabled=$enabled")
        }

        btnPause.setOnClickListener {
            if (gameView.gameState == GameView.GameState.RUNNING) {
                gameView.pause()
                gameView.gameState = GameView.GameState.PAUSED
                btnPause.setImageResource(R.drawable.ic_play)
                Log.d("ChallengeGameActivity", "Game paused")
            } else if (gameView.gameState == GameView.GameState.PAUSED) {
                gameView.resume()
                gameView.gameState = GameView.GameState.RUNNING
                btnPause.setImageResource(R.drawable.ic_pause)
                Log.d("ChallengeGameActivity", "Game resumed")
            }
        }
    }

    override fun onBackPressed() {
        if (!progressSaved && gameView.player.coins > 0) {
            saveProgress()
            Log.d("ChallengeGameActivity", "onBackPressed: saved coins before back")
        }
        super.onBackPressed()
    }

    override fun onPause() {
        super.onPause()
        gameView.pause()
        MusicManager.pause()
        if (!progressSaved && gameView.player.coins > 0) {
            saveProgress()
            Log.d("ChallengeGameActivity", "onPause: saved coins (any state)")
        }
    }
    override fun onStop() {
        super.onStop()
        if (!progressSaved && gameView.player.coins > 0) {
            saveProgress()
            Log.d("ChallengeGameActivity", "onStop: saved coins")
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
            Log.d("ChallengeGameActivity", "saveProgress(): already saved -> skipping")
            return
        }
        try {
            val earned = gameView.player.coins
            val oldTotal = PlayerDataManager.getCoins(this)
            Log.d("ChallengeGameActivity", "saveProgress(): earned=$earned, oldTotal=$oldTotal")
            if (earned > 0) {
                PlayerDataManager.addCoins(this, earned)
                val newTotal = PlayerDataManager.getCoins(this)
                Log.d("ChallengeGameActivity", "Saved: newTotal=$newTotal")
                PlayerDataManager.debugPrefs(this)
            } else {
                Log.d("ChallengeGameActivity", "No coins to save")
            }
            progressSaved = true
        } catch (e: Exception) {
            Log.e("ChallengeGameActivity", "saveProgress failed: ${e.message}", e)
        }
    }
}