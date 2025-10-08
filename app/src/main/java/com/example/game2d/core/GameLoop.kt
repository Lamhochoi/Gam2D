package com.example.game2d.core

import android.graphics.Canvas
import android.view.SurfaceHolder

class GameLoop(
    private val gameView: GameView,
    private val holder: SurfaceHolder
) : Thread() {

    @Volatile
    private var running = false
    private val targetFPS = 60
    private val frameTimeNs = 1_000_000_000L / targetFPS

    // Đếm FPS thực tế
    private var frames = 0
    private var fpsTimer = System.currentTimeMillis()
    var currentFPS = 0
        private set

    val isRunning: Boolean
        get() = running

    fun startLoop() {
        if (!running) {
            running = true
            start()
        }
    }

    fun stopLoop() {
        running = false
        if (this != currentThread()) {
            try {
                join(500) // đợi tối đa 100ms rồi thoát
            } catch (_: InterruptedException) { }
        }
    }

    override fun run() {
        var lastTime = System.nanoTime()

        while (running) {
            val now = System.nanoTime()
            val deltaTime = (now - lastTime) / 1_000_000_000f // giây
            lastTime = now

            // Update logic
            gameView.update(deltaTime)

            // Render an toàn
            var canvas: Canvas? = null
            try {
                if (!holder.surface.isValid) continue
                canvas = holder.lockCanvas()
                if (canvas != null) {
                    synchronized(holder) {
                        gameView.render(canvas)
                    }
                }
            } finally {
                if (canvas != null) {
                    holder.unlockCanvasAndPost(canvas)
                }
            }

            // Đếm FPS
            frames++
            val nowMs = System.currentTimeMillis()
            if (nowMs - fpsTimer >= 1000) {
                currentFPS = frames
                frames = 0
                fpsTimer = nowMs
            }

            // Giới hạn FPS
            val frameDuration = System.nanoTime() - now
            val sleepTime = (frameTimeNs - frameDuration) / 1_000_000L
            if (sleepTime > 0) {
                try {
                    sleep(sleepTime)
                } catch (_: InterruptedException) { }
            } else {
                yield() // Game đang lag → nhường CPU
            }
        }
    }
}
