package com.example.game2d.graphics

import android.graphics.*
import com.example.game2d.core.ChallengeGameView
import com.example.game2d.core.GameView

class ChallengeRenderer(private val challengeGameView: ChallengeGameView) : Renderer(challengeGameView) {

    private val levelPaint = Paint().apply {
        color = Color.YELLOW
        textSize = 60f
        isAntiAlias = true
        typeface = Typeface.DEFAULT_BOLD
        setShadowLayer(10f, 0f, 0f, Color.BLACK)
    }

    override fun draw(canvas: Canvas) {
        super.draw(canvas)
    }

    override fun drawGameTime(canvas: Canvas) {
        val timeMs = challengeGameView.getGameTime()
        val minutes = timeMs / 60000
        val seconds = (timeMs % 60000) / 1000
        val milliseconds = timeMs % 1000

        val text = if (challengeGameView.currentLevelIndex == challengeGameView.levels.size - 1 &&
            challengeGameView.gameState == GameView.GameState.WIN) {
            "Tổng thời gian: %02d:%02d.%03d".format(minutes, seconds, milliseconds)
        } else {
            "Thời gian: %02d:%02d.%03d".format(minutes, seconds, milliseconds)
        }

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