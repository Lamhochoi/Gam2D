package com.example.game2d.managers

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import com.example.game2d.R

object SoundManager {
    private var soundPool: SoundPool? = null
    private val soundMap: MutableMap<String, Int> = mutableMapOf()
    private var soundEnabled = true
    private var initialized = false

    fun init(context: Context) {
        if (initialized) return

        val attrs = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_GAME)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()

        soundPool = SoundPool.Builder()
            .setMaxStreams(6)
            .setAudioAttributes(attrs)
            .build()

        // Nạp các file âm thanh (đảm bảo có trong res/raw)
        soundMap["shoot"]        = soundPool!!.load(context, R.raw.shot, 1)
        soundMap["enemy_bounce"] = soundPool!!.load(context, R.raw.bomb, 1)
        soundMap["hit"]          = soundPool!!.load(context, R.raw.hit, 1)
        soundMap["falling_hit"]  = soundPool!!.load(context, R.raw.explosion, 1)

        initialized = true
    }

    fun setSoundEnabled(enabled: Boolean) {
        soundEnabled = enabled
    }

    fun isSoundEnabled(): Boolean = soundEnabled

    fun playShoot() = play("shoot")
    fun playEnemyBounce() = play("enemy_bounce")
    fun playHit() = play("hit")
    fun playFallingHit() = play("falling_hit")

    private fun play(key: String) {
        if (!soundEnabled || !initialized) return
        val id = soundMap[key] ?: return
        soundPool?.play(id, 1f, 1f, 1, 0, 1f)
    }

    fun release() {
        try {
            soundPool?.release()
        } catch (_: IllegalStateException) { }
        soundPool = null
        soundMap.clear()
        initialized = false
    }
}
