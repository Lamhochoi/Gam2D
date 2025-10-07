package com.example.game2d.core

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.*
import android.util.AttributeSet
import android.util.Log
import android.view.MotionEvent
import android.view.SurfaceHolder
import androidx.core.content.res.ResourcesCompat
import com.example.game2d.ChallengeLeaderboardActivity
import com.example.game2d.R
import com.example.game2d.entities.Player
import com.example.game2d.managers.ChallengeCollisionManager
import com.example.game2d.managers.ChallengeEntityManager
import com.example.game2d.managers.DifficultyManager
import com.example.game2d.managers.MusicManager
import com.example.game2d.managers.SoundManager
import com.example.game2d.graphics.ChallengeRenderer
import com.example.game2d.managers.ChallengeDifficultyManager

class ChallengeGameView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : GameView(context, attrs, defStyleAttr) {

    val levels = listOf("MARS", "MERCURY", "SATURN")
    var currentLevelIndex = 0
    private var totalGameStartTime: Long = 0L
    private var pauseStartTime: Long? = null // Thời điểm bắt đầu tạm dừng
    private var totalPausedTime: Long = 0L // Tổng thời gian đã tạm dừng
    private val challengePrefs: SharedPreferences = context.getSharedPreferences("challenge_leaderboard", Context.MODE_PRIVATE)
    private var btnContinueRect: RectF? = null

    override val entityManager = ChallengeEntityManager(this)
    override val collisionManager = ChallengeCollisionManager(this)
    override val renderer = ChallengeRenderer(this)

    private val victoryBitmap by lazy { BitmapFactory.decodeResource(resources, R.drawable.congratulations) }
    private var scaledVictory: Bitmap? = null

    override fun surfaceCreated(holder: SurfaceHolder) {
        screenW = holder.surfaceFrame.width()
        screenH = holder.surfaceFrame.height()
        entityManager.initResources(screenW, screenH)
        totalGameStartTime = System.currentTimeMillis()
        applyLevel()
        scaledVictory = Bitmap.createScaledBitmap(victoryBitmap, (screenW * 0.7f).toInt(), (screenW * 0.7f).toInt(), true)
        btnContinueRect = RectF(
            screenW / 2f - screenW * 0.25f,
            screenH / 2f + 100,
            screenW / 2f + screenW * 0.25f,
            screenH / 2f + 250
        )
        btnMenuRect = RectF(
            screenW / 2f - screenW * 0.25f,
            screenH / 2f + 270,
            screenW / 2f + screenW * 0.25f,
            screenH / 2f + 420
        )
        btnRestartRect = RectF(
            screenW / 2f - screenW * 0.25f,
            screenH / 2f - 50,
            screenW / 2f + screenW * 0.25f,
            screenH / 2f + 100
        )
        btnLeaderboardRect = RectF(
            screenW / 2f - screenW * 0.25f,
            screenH / 2f + 440,
            screenW / 2f + screenW * 0.25f,
            screenH / 2f + 590
        )
        super.surfaceCreated(holder)
    }

    private fun applyLevel() {
        val level = levels[currentLevelIndex]
        ChallengeDifficultyManager.apply(level, this, keepPlayerState = true)
        entityManager.reset(screenW, screenH)
        gameState = GameState.RUNNING
        gameStartTime = System.currentTimeMillis()
        gameEndTime = null  // Thêm dòng này: Reset gameEndTime để tổng thời gian tiếp tục chạy dựa trên thời gian thực
    }

    override var gameState = GameState.RUNNING
        set(value) {
            field = value
            when (value) {
                GameState.GAME_OVER -> {
                    overlayStartTime = System.currentTimeMillis()
                    gameEndTime = System.currentTimeMillis()
                    MusicManager.pause()
                    onGameEnd?.invoke()
                }
                GameState.WIN -> {
                    overlayStartTime = System.currentTimeMillis()
                    gameEndTime = System.currentTimeMillis()
                    if (currentLevelIndex < levels.size - 1) {
                        onGameEnd?.invoke()
                    } else {
                        val totalTime = getTotalGameTime()
                        saveChallengeTime(totalTime)
                        onGameEnd?.invoke()
                    }
                }
                GameState.PAUSED -> {
                    pauseStartTime = System.currentTimeMillis() // Lưu thời điểm bắt đầu tạm dừng
                    MusicManager.pause()
                }
                GameState.RUNNING -> {
                    pauseStartTime?.let {
                        totalPausedTime += System.currentTimeMillis() - it // Cập nhật tổng thời gian tạm dừng
                        pauseStartTime = null
                    }
                }
                else -> {}
            }
        }

    fun getTotalGameTime(): Long {
        val currentTime = if (gameState == GameState.PAUSED && pauseStartTime != null) {
            pauseStartTime!! // Dùng thời điểm tạm dừng nếu đang PAUSED
        } else {
            gameEndTime ?: System.currentTimeMillis() // Dùng thời gian hiện tại hoặc kết thúc
        }
        return currentTime - totalGameStartTime - totalPausedTime // Trừ thời gian tạm dừng
    }

    private fun saveChallengeTime(time: Long) {
        val topTimesStr = challengePrefs.getString("top_times", "") ?: ""
        val topTimes = topTimesStr.split(",").mapNotNull { it.toLongOrNull() }.toMutableList()
        topTimes.add(time)
        topTimes.sort()
        if (topTimes.size > 10) topTimes.removeLast()
        challengePrefs.edit().putString("top_times", topTimes.joinToString(",")).apply()
        Log.d("ChallengeGameView", "Saved challenge time: $time")
    }

    fun getChallengeTopTimes(): List<Long> {
        val topTimesStr = challengePrefs.getString("top_times", "") ?: ""
        return topTimesStr.split(",").mapNotNull { it.toLongOrNull() }.sorted()
    }

    override fun drawOverlay(canvas: Canvas, title: String) {
        val elapsed = (System.currentTimeMillis() - overlayStartTime) / 1000f
        val bgPaint = Paint().apply { color = Color.argb(240, 0, 0, 0) }
        canvas.drawRect(0f, 0f, screenW.toFloat(), screenH.toFloat(), bgPaint)

        val customFont = ResourcesCompat.getFont(context, R.font.robotomono_bold)
        val titlePaint = Paint().apply {
            color = Color.WHITE
            textSize = screenW * 0.08f * (1f + 0.03f * kotlin.math.sin(elapsed * 3))
            textAlign = Paint.Align.CENTER
            typeface = customFont
            setShadowLayer(20f, 0f, 0f, if (gameState == GameState.WIN) Color.GREEN else Color.RED)
        }

        val titleText = when (gameState) {
            GameState.GAME_OVER -> "THẤT BẠI"
            GameState.WIN -> if (currentLevelIndex < levels.size - 1) "VƯỢT MÀN ${levels[currentLevelIndex]}" else "HOÀN THÀNH KHIÊU CHIẾN"
            GameState.PAUSED -> "TẠM DỪNG"
            else -> return
        }
        canvas.drawText(titleText, screenW / 2f, screenH / 3f, titlePaint)

        val iconSize = (screenW * 0.7f).toInt()
        val iconX = (screenW - iconSize) / 2f
        val iconY = screenH / 4f - iconSize / 2f + 50f

        when (gameState) {
            GameState.GAME_OVER -> scaledSkull?.let { canvas.drawBitmap(it, iconX, iconY, null) }
            GameState.WIN -> if (currentLevelIndex < levels.size - 1) {
                scaledVictory?.let { canvas.drawBitmap(it, iconX, iconY, null) }
            } else {
                scaledTrophy?.let { canvas.drawBitmap(it, iconX, iconY, null) }
            }
            GameState.PAUSED -> scaledPause?.let { canvas.drawBitmap(it, iconX, iconY, null) }
            else -> {}
        }

        val glowAlpha = (128 + 127 * kotlin.math.sin(elapsed * 4)).toInt()
        val buttonBorder = Paint().apply {
            color = Color.argb(glowAlpha, 0, 255, 255)
            style = Paint.Style.STROKE
            strokeWidth = 8f
            setShadowLayer(15f, 0f, 0f, Color.CYAN)
        }
        val buttonTextPaint = Paint().apply {
            color = Color.WHITE
            textSize = screenW * 0.05f
            textAlign = Paint.Align.CENTER
            typeface = customFont
            setShadowLayer(12f, 0f, 0f, Color.CYAN)
        }

        fun drawButton(rect: RectF, text: String) {
            val btnPaint = Paint().apply {
                shader = LinearGradient(
                    rect.left, rect.top, rect.right, rect.bottom,
                    intArrayOf(Color.argb(220, 0, 255, 255), Color.argb(220, 0, 100, 200)),
                    null, Shader.TileMode.CLAMP
                )
            }
            canvas.drawRoundRect(rect, 30f, 30f, btnPaint)
            canvas.drawRoundRect(rect, 30f, 30f, buttonBorder)
            val textY = rect.centerY() - (buttonTextPaint.descent() + buttonTextPaint.ascent()) / 2
            canvas.drawText(text, rect.centerX(), textY, buttonTextPaint)
        }

        if (gameState == GameState.WIN && currentLevelIndex < levels.size - 1) {
            btnContinueRect?.let { drawButton(it, "▶ Tiếp tục") }
            btnMenuRect?.let { drawButton(it, "☰ Trang chủ") }
        } else if (gameState == GameState.GAME_OVER || (gameState == GameState.WIN && currentLevelIndex == levels.size - 1)) {
            btnRestartRect?.let { drawButton(it, "▶ Chơi lại") }
            btnMenuRect?.let { drawButton(it, "☰ Trang chủ") }
            btnLeaderboardRect?.let { drawButton(it, "🏆 Bảng xếp hạng") }
        } else if (gameState == GameState.PAUSED) {
            btnRestartRect?.let { drawButton(it, "▶ Tiếp tục") }
            btnMenuRect?.let { drawButton(it, "☰ Trang chủ") }
        }

        if (gameState == GameState.WIN && currentLevelIndex == levels.size - 1) {
            val leaderboardRect = RectF(100f, screenH * 0.6f, screenW - 100f, screenH - 100f)
            val leaderboardBgPaint = Paint().apply {
                color = Color.argb(200, 20, 20, 20)
            }
            canvas.drawRoundRect(leaderboardRect, 30f, 30f, leaderboardBgPaint)

            val leaderboardBorder = Paint().apply {
                color = Color.YELLOW
                style = Paint.Style.STROKE
                strokeWidth = 8f
                setShadowLayer(15f, 0f, 0f, Color.YELLOW)
            }
            canvas.drawRoundRect(leaderboardRect, 30f, 30f, leaderboardBorder)

            val leaderboardTitlePaint = Paint().apply {
                color = Color.WHITE
                textSize = screenW * 0.04f * (1f + 0.03f * kotlin.math.sin(elapsed * 3))
                textAlign = Paint.Align.CENTER
                typeface = customFont
                setShadowLayer(15f, 0f, 0f, Color.YELLOW)
            }
            canvas.drawText("Bảng Xếp Hạng Khiêu Chiến", leaderboardRect.centerX(), leaderboardRect.top + 60f, leaderboardTitlePaint)

            val topTimes = getChallengeTopTimes()
            topTimes.take(3).forEachIndexed { index, time ->
                val rank = index + 1
                val minutes = time / 60000
                val seconds = (time % 60000) / 1000
                val milliseconds = time % 1000
                val text = "Top $rank: %02d:%02d.%03d".format(minutes, seconds, milliseconds)
                val leaderboardPaint = Paint().apply {
                    color = when (rank) {
                        1 -> Color.rgb(255, 215, 0)
                        2 -> Color.rgb(192, 192, 192)
                        else -> Color.rgb(205, 127, 50)
                    }
                    textSize = screenW * 0.035f
                    textAlign = Paint.Align.CENTER
                    typeface = customFont
                    setShadowLayer(10f, 0f, 0f, Color.BLACK)
                }
                canvas.drawText(text, leaderboardRect.centerX(), leaderboardRect.top + 120f + index * 60f, leaderboardPaint)
            }

            val currentTime = getTotalGameTime()
            val minutes = currentTime / 60000
            val seconds = (currentTime % 60000) / 1000
            val milliseconds = currentTime % 1000
            val leaderboardPaint = Paint().apply {
                color = Color.YELLOW
                textSize = screenW * 0.055f
                textAlign = Paint.Align.CENTER
                typeface = customFont
                setShadowLayer(10f, 0f, 0f, Color.BLACK)
            }
            canvas.drawText(
                "Thời gian của bạn: %02d:%02d.%03d".format(minutes, seconds, milliseconds),
                leaderboardRect.centerX(),
                leaderboardRect.top + 300f,
                leaderboardPaint
            )
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (gameState == GameState.WIN && currentLevelIndex < levels.size - 1) {
            if (event.action == MotionEvent.ACTION_DOWN) {
                btnContinueRect?.let { rect ->
                    if (rect.contains(event.x, event.y)) {
                        currentLevelIndex++
                        applyLevel()
                        return true
                    }
                }
                btnMenuRect?.let { rect ->
                    if (rect.contains(event.x, event.y)) {
                        onBackToMenu?.invoke()
                        return true
                    }
                }
            }
        } else if (gameState == GameState.GAME_OVER || (gameState == GameState.WIN && currentLevelIndex == levels.size - 1)) {
            if (event.action == MotionEvent.ACTION_DOWN) {
                btnRestartRect?.let { rect ->
                    if (rect.contains(event.x, event.y)) {
                        currentLevelIndex = 0
                        totalGameStartTime = System.currentTimeMillis()
                        totalPausedTime = 0L // Reset thời gian tạm dừng khi chơi lại
                        player.reset(screenW, screenH)
                        player.coins = 0  // Thêm dòng này để reset coins
                        tvCoin?.post { tvCoin?.text = "0" }  // Cập nhật UI coin về 0 (post để chạy trên UI thread)
                        gameEndTime = null  // Thêm dòng này
                        applyLevel()
                        return true
                    }
                }
                btnMenuRect?.let { rect ->
                    if (rect.contains(event.x, event.y)) {
                        onBackToMenu?.invoke()
                        return true
                    }
                }
                btnLeaderboardRect?.let { rect ->
                    if (rect.contains(event.x, event.y)) {
                        onLeaderboard?.invoke()
                        return true
                    }
                }
            }
        } else if (gameState == GameState.PAUSED) {
            if (event.action == MotionEvent.ACTION_DOWN) {
                btnRestartRect?.let { rect ->
                    if (rect.contains(event.x, event.y)) {
                        resume()
                        gameState = GameState.RUNNING
                        return true
                    }
                }
                btnMenuRect?.let { rect ->
                    if (rect.contains(event.x, event.y)) {
                        onBackToMenu?.invoke()
                        return true
                    }
                }
            }
        }
        return entityManager.handleTouch(event)
    }

    override fun getGameTime(): Long {
        return if (currentLevelIndex == levels.size - 1 && gameState == GameState.WIN) {
            getTotalGameTime()
        } else {
            super.getGameTime()
        }
    }

    override fun saveGameTime(time: Long) {
        if (currentLevelIndex == levels.size - 1) {
            saveChallengeTime(time)
        }
    }

    override fun getTopTimes(): List<Long> {
        return if (currentLevelIndex == levels.size - 1 && gameState == GameState.WIN) {
            getChallengeTopTimes()
        } else {
            super.getTopTimes()
        }
    }

    override fun onLeaderboard() {
        val intent = Intent(context, ChallengeLeaderboardActivity::class.java)
        context.startActivity(intent)
    }
    override fun surfaceDestroyed(holder: SurfaceHolder) {
        super.surfaceDestroyed(holder)
        Log.d("ChallengeGameView", "surfaceDestroyed in ChallengeGameView")
    }
    override fun pause() {
        super.pause()  // Gọi cha để stop loop và music
        if (gameState == GameState.RUNNING) {  // Chỉ set nếu đang running
            pauseStartTime = System.currentTimeMillis()
            gameState = GameState.PAUSED
        }
    }

    override fun resume() {
        if (gameState == GameState.PAUSED && pauseStartTime != null) {
            totalPausedTime += System.currentTimeMillis() - pauseStartTime!!
            pauseStartTime = null
        }
        super.resume()  // Gọi cha để start loop và music
        gameState = GameState.RUNNING
    }
}