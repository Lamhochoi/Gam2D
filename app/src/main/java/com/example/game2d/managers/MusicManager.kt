package com.example.game2d.managers

import android.content.Context
import android.media.MediaPlayer
import com.example.game2d.R

object MusicManager {
    private var bgPlayer: MediaPlayer? = null
    private var musicEnabled = true
    private var initialized = false

    fun init(context: Context) {
        if (!initialized) {
            bgPlayer = MediaPlayer.create(context, R.raw.bg_music)?.apply {
                isLooping = true
                setVolume(0.5f, 0.5f)
            }
            initialized = true
        }
    }

    fun setMusicEnabled(enabled: Boolean) {
        musicEnabled = enabled
    }

    fun toggleMusic(context: Context) {
        setMusicEnabled(!musicEnabled)
        if (musicEnabled) {
            start(context)
        } else {
            pause()
        }
    }

    fun isMusicEnabled(): Boolean = musicEnabled

    fun start(context: Context) {
        if (!musicEnabled) return
        try {
            if (bgPlayer == null) {
                // nếu bị release thì tạo lại
                bgPlayer = MediaPlayer.create(context, R.raw.bg_music)?.apply {
                    isLooping = true
                    setVolume(0.5f, 0.5f)
                }
            }
            if (bgPlayer != null && !(bgPlayer?.isPlaying ?: false)) {
                bgPlayer?.start()
            }
        } catch (e: IllegalStateException) {
            bgPlayer = null
            initialized = false
        }
    }

    fun pause() {
        try {
            if (bgPlayer?.isPlaying == true) {
                bgPlayer?.pause()
            }
        } catch (e: IllegalStateException) {
            // Ignore if player is in invalid state
        }
    }

    fun stop() {
        try {
            bgPlayer?.let {
                if (it.isPlaying) it.stop()
                it.release()
            }
        } catch (_: IllegalStateException) { }
        bgPlayer = null
        initialized = false
    }

    fun release() {
        bgPlayer?.release()
        bgPlayer = null
        initialized = false
    }
}