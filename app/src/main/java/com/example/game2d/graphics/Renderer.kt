package com.example.game2d.graphics

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import com.example.game2d.core.GameView
import com.example.game2d.entities.FallingObject

class Renderer(private val gameView: GameView) {

    private val enemyHpPaint = Paint().apply { color = Color.RED }
    private val playerHpPaint = Paint().apply { color = Color.GREEN }
    private val rectReusable = RectF()

    // Paint cho chữ FPS
    private val fpsPaint = Paint().apply {
        color = Color.WHITE
        textSize = 40f
        isAntiAlias = true
    }

    // Paint cho điểm số
    private val scorePaint = Paint().apply {
        color = Color.YELLOW
        textSize = 50f
        isAntiAlias = true
    }

    // Paint cho Boss HP bar
    private val bossHpBackPaint = Paint().apply { color = Color.DKGRAY }
    private val bossHpPaint = Paint().apply { color = Color.MAGENTA }

    fun draw(canvas: Canvas) {
        drawBackground(canvas)
        drawPlayer(canvas)
        drawEnemies(canvas)
        drawExplosions(canvas)   // ⬅️ chuyển lên ngay sau enemy
        drawBullets(canvas)
        drawFallingObjects(canvas)   // ⬅️ đã gom vào EntityManager
        drawUI(canvas)
        drawBossHp(canvas)
        drawFPS(canvas)
        drawScore(canvas)
    }
    // Vẽ Explosion
    private fun drawExplosions(canvas: Canvas) {
        val manager = gameView.entityManager
        manager.activeExplosions.forEach { exp ->
            exp.draw(canvas)  // Explosion đã có sẵn hàm draw(canvas)
        }
    }

    private fun drawBackground(canvas: Canvas) {
        val manager = gameView.entityManager
        val bg = manager.getBackground()
        bg?.let {
            canvas.drawBitmap(it, 0f, manager.getBgY1(), null)
            canvas.drawBitmap(it, 0f, manager.getBgY2(), null)
        } ?: run { canvas.drawColor(Color.BLACK) }
    }

    private fun drawPlayer(canvas: Canvas) {
        val p = gameView.player
        p.bitmap?.let { canvas.drawBitmap(it, p.x, p.y, null) }
    }

    private fun drawEnemies(canvas: Canvas) {
        val manager = gameView.entityManager
        manager.activeEnemies.forEach { e ->
            e.bitmap?.let { canvas.drawBitmap(it, e.x, e.y, null) }
            val barHeight = 10f
            val hpWidth = e.size * (e.hp.toFloat() / e.maxHp)
            rectReusable.set(e.x, e.y - barHeight, e.x + hpWidth, e.y)
            canvas.drawRect(rectReusable, enemyHpPaint)
        }
    }

    private fun drawBullets(canvas: Canvas) {
        val manager = gameView.entityManager
        manager.activeBullets.forEach { b -> b.bitmap?.let { canvas.drawBitmap(it, b.x, b.y, null) } }
        manager.activeEnemyBullets.forEach { b -> b.bitmap?.let { canvas.drawBitmap(it, b.x, b.y, null) } }
    }

    // Vẽ FallingObjects
    private fun drawFallingObjects(canvas: Canvas) {
        val manager = gameView.entityManager
        manager.activeFallingObjects.forEach { f: FallingObject ->
            f.bitmap?.let { canvas.drawBitmap(it, f.x, f.y, null) }
        }
    }

    private val hpBackPaint = Paint().apply { color = Color.DKGRAY }

    private fun drawUI(canvas: Canvas) {
        val hpBarHeight = 30f
        val maxWidth = gameView.screenW.toFloat()

        // Background
        rectReusable.set(0f, 0f, maxWidth, hpBarHeight)
        canvas.drawRect(rectReusable, hpBackPaint)

        // Current HP
        val hpWidth = maxWidth * (gameView.player.hp.toFloat() / gameView.player.maxHp)
        rectReusable.set(0f, 0f, hpWidth, hpBarHeight)
        canvas.drawRect(rectReusable, playerHpPaint)
    }

    // Vẽ Boss HP riêng
    private fun drawBossHp(canvas: Canvas) {
        val boss = gameView.entityManager.activeEnemies.find { it.isBoss && it.active }
        boss?.let {
            val barHeight = 40f
            val maxWidth = gameView.screenW.toFloat()
            rectReusable.set(0f, 50f, maxWidth, 50f + barHeight)
            canvas.drawRect(rectReusable, bossHpBackPaint)

            val hpWidth = maxWidth * (it.hp.toFloat() / it.maxHp)
            rectReusable.set(0f, 50f, hpWidth, 50f + barHeight)
            canvas.drawRect(rectReusable, bossHpPaint)
        }
    }

    private fun drawFPS(canvas: Canvas) {
        val fps = gameView.currentFPS
        canvas.drawText("FPS: $fps", 50f, 100f, fpsPaint)
    }

    // Vẽ điểm số
    private fun drawScore(canvas: Canvas) {
        val score = gameView.entityManager.enemiesKilled
        canvas.drawText("Score: $score", 50f, 170f, scorePaint)
    }
}
