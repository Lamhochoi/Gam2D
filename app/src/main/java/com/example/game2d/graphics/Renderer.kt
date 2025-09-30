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
        drawCoins(canvas)         // ⬅️ thêm dòng này
        drawFallingObjects(canvas)   // ⬅️ đã gom vào EntityManager
        drawUI(canvas)
        drawBossHp(canvas)
        drawFPS(canvas)
        drawScore(canvas)
        drawPowerUps(canvas)
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
                color = Color.argb(100, 0, 255, 255)  // Xanh dương mờ
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

    // Vẽ FallingObjects
    private fun drawFallingObjects(canvas: Canvas) {
        val manager = gameView.entityManager
        manager.activeFallingObjects.forEach { f: FallingObject ->
            f.bitmap?.let { canvas.drawBitmap(it, f.x, f.y, null) }
        }
    }


    private fun drawUI(canvas: Canvas) {
        val marginTop = 20f        // cách top một khoảng
        val marginSide = 40f       // cách 2 bên một chút
        val hpBarHeight = 35f
        val maxWidth = gameView.screenW.toFloat() - 2 * marginSide

        val left = marginSide
        val top = marginTop
        val right = left + maxWidth
        val bottom = top + hpBarHeight

        // Nền bo tròn (màu xám tối)
        val bgPaint = Paint().apply {
            color = Color.argb(180, 50, 50, 50)
            style = Paint.Style.FILL
            isAntiAlias = true
        }
        canvas.drawRoundRect(left, top, right, bottom, 20f, 20f, bgPaint)

        // Viền ngoài phát sáng xanh dương
        val borderPaint = Paint().apply {
            color = Color.CYAN
            style = Paint.Style.STROKE
            strokeWidth = 4f
            isAntiAlias = true
            setShadowLayer(12f, 0f, 0f, Color.CYAN)
        }
        canvas.drawRoundRect(left, top, right, bottom, 20f, 20f, borderPaint)

        // Tính chiều rộng HP hiện tại
        val hpPercent = gameView.player.hp.toFloat() / gameView.player.maxHp
        val hpWidth = maxWidth * hpPercent

        // Đổi màu theo % máu: xanh → vàng → đỏ
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

        // Vẽ thanh máu
        if (hpWidth > 0) {
            canvas.drawRoundRect(left, top, left + hpWidth, bottom, 20f, 20f, hpPaint)
        }
    }


    // Vẽ Boss HP riêng (style đẹp hơn)
    private fun drawBossHp(canvas: Canvas) {
        val boss = gameView.entityManager.activeEnemies.find { it.isBoss && it.active }
        boss?.let {
            val marginTop = 80f        // cách top để không đè lên thanh player
            val marginSide = 40f
            val barHeight = 40f
            val maxWidth = gameView.screenW.toFloat() - 2 * marginSide

            val left = marginSide
            val top = marginTop
            val right = left + maxWidth
            val bottom = top + barHeight

            // Nền xám tối
            val bgPaint = Paint().apply {
                color = Color.argb(200, 30, 30, 30)
                style = Paint.Style.FILL
                isAntiAlias = true
            }
            canvas.drawRoundRect(left, top, right, bottom, 25f, 25f, bgPaint)

            // Viền tím phát sáng
            val borderPaint = Paint().apply {
                color = Color.MAGENTA
                style = Paint.Style.STROKE
                strokeWidth = 5f
                isAntiAlias = true
                setShadowLayer(15f, 0f, 0f, Color.MAGENTA)
            }
            canvas.drawRoundRect(left, top, right, bottom, 25f, 25f, borderPaint)

            // Tính % máu
            val hpPercent = it.hp.toFloat() / it.maxHp
            val hpWidth = maxWidth * hpPercent

            // Màu máu Boss: tím → đỏ khi gần hết
            val hpColor = when {
                hpPercent > 0.5f -> Color.MAGENTA
                hpPercent > 0.2f -> Color.rgb(255, 100, 180) // hồng đậm
                else -> Color.RED
            }

            val hpPaint = Paint().apply {
                color = hpColor
                style = Paint.Style.FILL
                isAntiAlias = true
            }

            // Vẽ thanh máu Boss
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

        // Tính kích thước text để vẽ nền
        val bounds = Rect()
        fpsPaint.getTextBounds(text, 0, text.length, bounds)

        val bgLeft = x - padding
        val bgTop = y + bounds.top - padding
        val bgRight = x + bounds.width() + padding
        val bgBottom = y + bounds.bottom + padding

        val bgPaint = Paint().apply {
            color = Color.argb(150, 20, 20, 20) // nền đen mờ
            style = Paint.Style.FILL
            isAntiAlias = true
        }

        // Vẽ nền bo tròn
        canvas.drawRoundRect(
            RectF(bgLeft, bgTop, bgRight, bgBottom),
            20f, 20f, bgPaint
        )

        // Vẽ chữ FPS
        canvas.drawText(text, x, y, fpsPaint)
    }

    // Vẽ Score với hộp nền đẹp
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

        // Tính kích thước text để vẽ nền
        val bounds = Rect()
        scorePaint.getTextBounds(text, 0, text.length, bounds)

        val bgLeft = x - padding
        val bgTop = y + bounds.top - padding
        val bgRight = x + bounds.width() + padding
        val bgBottom = y + bounds.bottom + padding

        val bgPaint = Paint().apply {
            color = Color.argb(150, 30, 30, 30) // nền xám mờ
            style = Paint.Style.FILL
            isAntiAlias = true
        }

        // Vẽ nền bo tròn
        canvas.drawRoundRect(
            RectF(bgLeft, bgTop, bgRight, bgBottom),
            20f, 20f, bgPaint
        )

        // Vẽ chữ Score
        canvas.drawText(text, x, y, scorePaint)
    }

}