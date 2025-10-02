package com.example.game2d.graphics

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import com.example.game2d.core.GameView
import com.example.game2d.entities.FallingObject
import android.graphics.Typeface
import android.graphics.Rect

class Renderer(private val gameView: GameView) {

    private val enemyHpPaint = Paint().apply { color = Color.RED }
    private val playerHpPaint = Paint().apply { color = Color.GREEN }
    private val rectReusable = RectF()

    private val fpsPaint = Paint().apply {
        color = Color.WHITE
        textSize = 40f
        isAntiAlias = true
    }

    private val scorePaint = Paint().apply {
        color = Color.YELLOW
        textSize = 50f
        isAntiAlias = true
    }

    private val timePaint = Paint().apply {
        color = Color.CYAN
        textSize = 50f
        isAntiAlias = true
        typeface = Typeface.DEFAULT_BOLD
    }

    private val bossHpBackPaint = Paint().apply { color = Color.DKGRAY }
    private val bossHpPaint = Paint().apply { color = Color.MAGENTA }

    fun draw(canvas: Canvas) {
        drawBackground(canvas)
        drawPlayer(canvas)
        drawEnemies(canvas)
        drawExplosions(canvas)
        drawBullets(canvas)
        drawCoins(canvas)
        drawFallingObjects(canvas)
        drawUI(canvas)
        drawBossHp(canvas)
        drawFPS(canvas)
        drawScore(canvas)
        drawPowerUps(canvas)
        drawGameTime(canvas)
    }

    private fun drawPowerUps(canvas: Canvas) {
        val manager = gameView.entityManager
        manager.activePowerUps.forEach { pu ->
            pu.bitmap?.let { canvas.drawBitmap(it, pu.x, pu.y, null) }
        }
    }

    private fun drawCoins(canvas: Canvas) {
        val manager = gameView.entityManager
        manager.activeCoins.forEach { coin ->
            coin.bitmap?.let {
                canvas.drawBitmap(it, coin.x, coin.y, null)
            }
        }
    }

    private fun drawExplosions(canvas: Canvas) {
        val manager = gameView.entityManager
        manager.activeExplosions.forEach { exp ->
            exp.draw(canvas)
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
        p.bitmap?.let { bmp ->
            if (p.isInvincible) {
                val paint = Paint()
                paint.alpha = 128
                canvas.drawBitmap(bmp, p.x, p.y, paint)
            } else {
                canvas.drawBitmap(bmp, p.x, p.y, null)
            }
        }

        if (p.shield > 0) {
            val shieldPaint = Paint().apply {
                color = Color.argb(100, 0, 255, 255)
                style = Paint.Style.STROKE
                strokeWidth = 10f
            }
            canvas.drawCircle(p.x + p.size / 2f, p.y + p.size / 2f, p.size / 1.5f, shieldPaint)
        }
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

    private fun drawFallingObjects(canvas: Canvas) {
        val manager = gameView.entityManager
        manager.activeFallingObjects.forEach { f: FallingObject ->
            f.bitmap?.let { canvas.drawBitmap(it, f.x, f.y, null) }
        }
    }

    private fun drawUI(canvas: Canvas) {
        val marginTop = 20f
        val marginSide = 40f
        val hpBarHeight = 35f
        val maxWidth = gameView.screenW.toFloat() - 2 * marginSide

        val left = marginSide
        val top = marginTop
        val right = left + maxWidth
        val bottom = top + hpBarHeight

        val bgPaint = Paint().apply {
            color = Color.argb(180, 50, 50, 50)
            style = Paint.Style.FILL
            isAntiAlias = true
        }
        canvas.drawRoundRect(left, top, right, bottom, 20f, 20f, bgPaint)

        val borderPaint = Paint().apply {
            color = Color.CYAN
            style = Paint.Style.STROKE
            strokeWidth = 4f
            isAntiAlias = true
            setShadowLayer(12f, 0f, 0f, Color.CYAN)
        }
        canvas.drawRoundRect(left, top, right, bottom, 20f, 20f, borderPaint)

        val hpPercent = gameView.player.hp.toFloat() / gameView.player.maxHp
        val hpWidth = maxWidth * hpPercent

        val hpColor = when {
            hpPercent > 0.6f -> Color.GREEN
            hpPercent > 0.3f -> Color.YELLOW
            else -> Color.RED
        }

        val hpPaint = Paint().apply {
            color = hpColor
            style = Paint.Style.FILL
            isAntiAlias = true
        }

        if (hpWidth > 0) {
            canvas.drawRoundRect(left, top, left + hpWidth, bottom, 20f, 20f, hpPaint)
        }
    }

    private fun drawBossHp(canvas: Canvas) {
        val boss = gameView.entityManager.activeEnemies.find { it.isBoss && it.active }
        boss?.let {
            val marginTop = 80f
            val marginSide = 40f
            val barHeight = 40f
            val maxWidth = gameView.screenW.toFloat() - 2 * marginSide

            val left = marginSide
            val top = marginTop
            val right = left + maxWidth
            val bottom = top + barHeight

            val bgPaint = Paint().apply {
                color = Color.argb(200, 30, 30, 30)
                style = Paint.Style.FILL
                isAntiAlias = true
            }
            canvas.drawRoundRect(left, top, right, bottom, 25f, 25f, bgPaint)

            val borderPaint = Paint().apply {
                color = Color.MAGENTA
                style = Paint.Style.STROKE
                strokeWidth = 5f
                isAntiAlias = true
                setShadowLayer(15f, 0f, 0f, Color.MAGENTA)
            }
            canvas.drawRoundRect(left, top, right, bottom, 25f, 25f, borderPaint)

            val hpPercent = it.hp.toFloat() / it.maxHp
            val hpWidth = maxWidth * hpPercent

            val hpColor = when {
                hpPercent > 0.5f -> Color.MAGENTA
                hpPercent > 0.2f -> Color.rgb(255, 100, 180)
                else -> Color.RED
            }

            val hpPaint = Paint().apply {
                color = hpColor
                style = Paint.Style.FILL
                isAntiAlias = true
            }

            if (hpWidth > 0) {
                canvas.drawRoundRect(left, top, left + hpWidth, bottom, 25f, 25f, hpPaint)
            }
        }
    }

    private fun drawFPS(canvas: Canvas) {
        val fps = gameView.currentFPS
        val text = "FPS: $fps"

        val padding = 20f
        val x = 40f
        val y = 250f

        fpsPaint.apply {
            color = Color.GREEN
            textSize = 50f
            isAntiAlias = true
            setShadowLayer(10f, 0f, 0f, Color.BLACK)
            typeface = Typeface.MONOSPACE
        }

        val bounds = Rect()
        fpsPaint.getTextBounds(text, 0, text.length, bounds)

        val bgLeft = x - padding
        val bgTop = y + bounds.top - padding
        val bgRight = x + bounds.width() + padding
        val bgBottom = y + bounds.bottom + padding

        val bgPaint = Paint().apply {
            color = Color.argb(150, 20, 20, 20)
            style = Paint.Style.FILL
            isAntiAlias = true
        }

        canvas.drawRoundRect(
            RectF(bgLeft, bgTop, bgRight, bgBottom),
            20f, 20f, bgPaint
        )

        canvas.drawText(text, x, y, fpsPaint)
    }

    private fun drawScore(canvas: Canvas) {
        val score = gameView.entityManager.enemiesKilled
        val text = "Score: $score"

        val padding = 20f
        val x = 40f
        val y = 320f

        scorePaint.apply {
            color = Color.YELLOW
            textSize = 50f
            isAntiAlias = true
            setShadowLayer(10f, 0f, 0f, Color.BLACK)
            typeface = Typeface.DEFAULT_BOLD
        }

        val bounds = Rect()
        scorePaint.getTextBounds(text, 0, text.length, bounds)

        val bgLeft = x - padding
        val bgTop = y + bounds.top - padding
        val bgRight = x + bounds.width() + padding
        val bgBottom = y + bounds.bottom + padding

        val bgPaint = Paint().apply {
            color = Color.argb(150, 30, 30, 30)
            style = Paint.Style.FILL
            isAntiAlias = true
        }

        canvas.drawRoundRect(
            RectF(bgLeft, bgTop, bgRight, bgBottom),
            20f, 20f, bgPaint
        )

        canvas.drawText(text, x, y, scorePaint)
    }

    private fun drawGameTime(canvas: Canvas) {
        val timeMs = gameView.getGameTime()
        val minutes = timeMs / 60000
        val seconds = (timeMs % 60000) / 1000
        val milliseconds = timeMs % 1000
        val text = "Time: %02d:%02d.%03d".format(minutes, seconds, milliseconds)

        val padding = 20f
        val x = 40f
        val y = 390f

        timePaint.apply {
            color = Color.CYAN
            textSize = 50f
            isAntiAlias = true
            setShadowLayer(10f, 0f, 0f, Color.BLACK)
            typeface = Typeface.DEFAULT_BOLD
        }

        val bounds = Rect()
        timePaint.getTextBounds(text, 0, text.length, bounds)

        val bgLeft = x - padding
        val bgTop = y + bounds.top - padding
        val bgRight = x + bounds.width() + padding
        val bgBottom = y + bounds.bottom + padding

        val bgPaint = Paint().apply {
            color = Color.argb(150, 30, 30, 30)
            style = Paint.Style.FILL
            isAntiAlias = true
        }

        canvas.drawRoundRect(
            RectF(bgLeft, bgTop, bgRight, bgBottom),
            20f, 20f, bgPaint
        )

        canvas.drawText(text, x, y, timePaint)
    }
}