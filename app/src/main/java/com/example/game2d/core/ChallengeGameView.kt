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
import com.example.game2d.managers.MusicManager
import com.example.game2d.managers.SoundManager
import com.example.game2d.graphics.ChallengeRenderer
import com.example.game2d.managers.ChallengeDifficultyManager
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.min

class ChallengeGameView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : GameView(context, attrs, defStyleAttr) {

    val levels = listOf("MARS", "MERCURY", "SATURN")
    var currentLevelIndex = 0
    private var lastClearedLevelIndex = -1 // Lưu màn vừa hoàn thành
    private var totalGameStartTime: Long = 0L
    private var pauseStartTime: Long? = null
    private var totalPausedTime: Long = 0L
    private val customFont by lazy {
        ResourcesCompat.getFont(context, R.font.robotomono_bold)
    }
    private val challengePrefs: SharedPreferences = context.getSharedPreferences("challenge_leaderboard", Context.MODE_PRIVATE)
    private var btnContinueRect: RectF? = null
    private var isProcessingTouch = false // Biến cờ để chống lặp sự kiện

    override val entityManager = ChallengeEntityManager(this)
    override val collisionManager = ChallengeCollisionManager(this)
    override val renderer = ChallengeRenderer(this)

    private val victoryBitmap by lazy { BitmapFactory.decodeResource(resources, R.drawable.congratulations) }
    private var scaledVictory: Bitmap? = null
    private val scaledVictoryLazy by lazy { scaleBitmap(victoryBitmap) }

    private fun scaleBitmap(bitmap: Bitmap): Bitmap {
        val iconSize = (screenW * 0.5f).toInt()
        return Bitmap.createScaledBitmap(bitmap, iconSize, iconSize, true)
    }

    override fun surfaceCreated(holder: SurfaceHolder) {
        super.surfaceCreated(holder)
        Log.d("ChallengeGameView", "surfaceCreated, surfaceReady=$surfaceReady")
    }

    override suspend fun initResourcesAndStart() {
        super.initResourcesAndStart()
        withContext(Dispatchers.Main) {
            applyLevel()
        }
    }

    override suspend fun loadAdditionalResources() {
        Log.d("ChallengeGameView", "Đang load resources thêm...")
        withContext(Dispatchers.IO) {
            scaledVictory = scaledVictoryLazy
        }
        withContext(Dispatchers.Main) {
            btnContinueRect = RectF(screenW/2f - screenW*0.25f, screenH/2f + 100, screenW/2f + screenW*0.25f, screenH/2f + 250)
            btnMenuRect = RectF(screenW/2f - screenW*0.25f, screenH/2f + 350, screenW/2f + screenW*0.25f, screenH/2f + 500)
            btnRestartRect = RectF(screenW/2f - screenW*0.25f, screenH/2f + 550, screenW/2f + screenW*0.25f, screenH/2f + 700)
            btnLeaderboardRect = RectF(screenW/2f - screenW*0.25f, screenH/2f + 750, screenW/2f + screenW*0.25f, screenH/2f + 900)
            totalGameStartTime = System.currentTimeMillis()
            Log.d("ChallengeGameView", "Button rects initialized: Continue=$btnContinueRect, Menu=$btnMenuRect, Restart=$btnRestartRect, Leaderboard=$btnLeaderboardRect")
        }
        Log.d("ChallengeGameView", "Load resources thêm hoàn tất")
    }

    private fun applyLevel() {
        if (!isResourcesInitialized) {
            Log.w("ChallengeGameView", "Tài nguyên chưa sẵn sàng, thử lại sau 100ms")
            lifecycleScope.launch {
                delay(100)
                applyLevel()
            }
            return
        }
        applyLevelInternal()
    }

    private fun applyLevelInternal() {
        if (currentLevelIndex >= levels.size) {
            Log.w("ChallengeGameView", "currentLevelIndex=$currentLevelIndex out of bounds, resetting to ${levels.size - 1}")
            currentLevelIndex = levels.size - 1
        }
        val level = levels[currentLevelIndex]
        Log.d("ChallengeGameView", "applyLevelInternal() started for level: $level, index: $currentLevelIndex, player.hp=${player.hp}")
        ChallengeDifficultyManager.apply(level, this, keepPlayerState = true)
        entityManager.reset(screenW, screenH)
        gameState = GameState.RUNNING
        gameStartTime = System.currentTimeMillis()
        gameEndTime = null
        Log.d("ChallengeGameView", "applyLevelInternal() completed for level: $level, index: $currentLevelIndex, player.hp=${player.hp}")
    }

    private val bgPaint = Paint().apply { color = Color.argb(240, 0, 0, 0) }
    private val buttonBorderPaint = Paint().apply {
        color = Color.CYAN
        style = Paint.Style.STROKE
        strokeWidth = 8f
        setShadowLayer(15f, 0f, 0f, Color.CYAN)
    }
    private val buttonTextPaint = Paint().apply {
        color = Color.WHITE
        textSize = screenW * 0.05f
        textAlign = Paint.Align.CENTER
        typeface = ResourcesCompat.getFont(context, R.font.robotomono_bold)
        setShadowLayer(12f, 0f, 0f, Color.CYAN)
    }
    private val leaderboardBorderPaint = Paint().apply {
        color = Color.YELLOW
        style = Paint.Style.STROKE
        strokeWidth = 8f
        setShadowLayer(15f, 0f, 0f, Color.YELLOW)
    }
    private val leaderboardBgPaint = Paint().apply {
        color = Color.argb(200, 20, 20, 20)
    }

    private fun getButtonGradient(rect: RectF): LinearGradient {
        return LinearGradient(
            rect.left, rect.top, rect.right, rect.bottom,
            intArrayOf(Color.argb(220, 0, 255, 255), Color.argb(220, 0, 100, 200)),
            null, Shader.TileMode.CLAMP
        )
    }

    override fun drawOverlay(canvas: Canvas, title: String) {
        val elapsed = (System.currentTimeMillis() - overlayStartTime) / 1000f

        canvas.drawRect(0f, 0f, screenW.toFloat(), screenH.toFloat(), bgPaint)

        buttonTextPaint.textSize = screenW * 0.08f * (1f + 0.03f * kotlin.math.sin(elapsed * 3))
        buttonTextPaint.setShadowLayer(20f, 0f, 0f, if (gameState == GameState.WIN) Color.GREEN else Color.RED)

        val titleText = when (gameState) {
            GameState.GAME_OVER -> "THẤT BẠI"
            GameState.WIN -> {
                val clearedLevelIndex = if (lastClearedLevelIndex >= 0) lastClearedLevelIndex else currentLevelIndex
                if (clearedLevelIndex < levels.size - 1) {
                    val justCleared = levels[clearedLevelIndex]
                    Log.d("ChallengeGameView", "Displaying victory for level: $justCleared, clearedLevelIndex=$clearedLevelIndex")
                    "VƯỢT MÀN $justCleared"
                } else {
                    Log.d("ChallengeGameView", "Displaying final victory, clearedLevelIndex=$clearedLevelIndex")
                    "HOÀN THÀNH KHIÊU CHIẾN"
                }
            }
            GameState.PAUSED -> "TẠM DỪNG"
            else -> return
        }
        canvas.drawText(titleText, screenW / 2f, screenH / 3f, buttonTextPaint)

        val iconSize = (screenW * 0.5f).toInt()
        val iconX = (screenW - iconSize) / 2f
        val iconY = screenH / 3f - iconSize / 2f - 50f  // Điều chỉnh vị trí icon

        when (gameState) {
            GameState.WIN -> scaledVictory?.let { canvas.drawBitmap(it, iconX, iconY, null) }
            GameState.GAME_OVER -> scaledSkull?.let { canvas.drawBitmap(it, iconX, iconY, null) }
            GameState.PAUSED -> scaledPause?.let { canvas.drawBitmap(it, iconX, iconY, null) }
            else -> {}
        }

        val clearedLevelIndex = if (lastClearedLevelIndex >= 0) lastClearedLevelIndex else currentLevelIndex

        if (gameState == GameState.WIN && clearedLevelIndex == levels.size - 1) {
            drawLeaderboard(canvas)
        }

        btnRestartRect?.let { drawButton(canvas, it, "Chơi lại") }
        btnMenuRect?.let { drawButton(canvas, it, "Về menu") }

        if (gameState == GameState.WIN) {
            if (clearedLevelIndex < levels.size - 1) {
                btnContinueRect?.let { drawButton(canvas, it, "Tiếp tục") }
            } else {
                btnLeaderboardRect?.let { drawButton(canvas, it, "Bảng xếp hạng") }
            }
        } else if (gameState == GameState.PAUSED) {
            btnRestartRect?.let { drawButton(canvas, it, "Tiếp tục") }
        }
    }

    private fun drawButton(canvas: Canvas, rect: RectF, text: String) {
        val buttonPaint = Paint().apply {
            shader = getButtonGradient(rect)
            isAntiAlias = true
        }
        canvas.drawRoundRect(rect, 30f, 30f, buttonPaint)
        canvas.drawRoundRect(rect, 30f, 30f, buttonBorderPaint)

        buttonTextPaint.textSize = screenW * 0.05f
        canvas.drawText(text, rect.centerX(), rect.centerY() + buttonTextPaint.textSize / 3, buttonTextPaint)
    }

    private fun drawLeaderboard(canvas: Canvas) {
        val topTimes = getTopTimes()
        val lbWidth = screenW * 0.7f
        val lbHeight = screenH * 0.5f
        val lbLeft = (screenW - lbWidth) / 2f
        val lbTop = screenH * 0.4f
        val lbRect = RectF(lbLeft, lbTop, lbLeft + lbWidth, lbTop + lbHeight)

        canvas.drawRoundRect(lbRect, 20f, 20f, leaderboardBgPaint)
        canvas.drawRoundRect(lbRect, 20f, 20f, leaderboardBorderPaint)

        val titlePaint = Paint(buttonTextPaint).apply {
            textSize = screenW * 0.06f
            color = Color.YELLOW
            setShadowLayer(15f, 0f, 0f, Color.YELLOW)
        }
        canvas.drawText("Bảng xếp hạng", lbRect.centerX(), lbTop + titlePaint.textSize + 20f, titlePaint)

        val entryPaint = Paint(buttonTextPaint).apply {
            textSize = screenW * 0.04f
            color = Color.WHITE
        }
        val entryHeight = entryPaint.textSize * 1.5f
        topTimes.forEachIndexed { index, time ->
            val minutes = time / 60000
            val seconds = (time % 60000) / 1000
            val milliseconds = time % 1000
            val entryText = "Top ${index + 1}: %02d:%02d.%03d".format(minutes, seconds, milliseconds)
            canvas.drawText(entryText, lbLeft + 40f, lbTop + titlePaint.textSize + 50f + index * entryHeight, entryPaint)
        }
    }

    private var previousGameState: GameState = GameState.RUNNING

    override fun update(deltaTime: Float) {
        previousGameState = gameState
        super.update(deltaTime)
        if (previousGameState == GameState.RUNNING && gameState == GameState.WIN) {
            onLevelCompleted()
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (event.action == MotionEvent.ACTION_DOWN) {
            if (isProcessingTouch) {
                Log.d("ChallengeGameView", "Touch event ignored: isProcessingTouch=true")
                return true
            }
            isProcessingTouch = true
            Log.d("ChallengeGameView", "isProcessingTouch set to true")

            if (gameState == GameState.WIN) {
                val clearedLevelIndex = if (lastClearedLevelIndex >= 0) lastClearedLevelIndex else currentLevelIndex
                Log.d("ChallengeGameView", "WIN state touch: clearedLevelIndex=$clearedLevelIndex, levels.size=${levels.size}")

                // Chỉ kiểm tra btnContinue nếu chưa hoàn thành hết (tương ứng với khi nó được vẽ)
                if (clearedLevelIndex < levels.size - 1) {
                    btnContinueRect?.let { rect ->
                        Log.d("ChallengeGameView", "Checking Continue button: rect=$rect, touchX=${event.x}, touchY=${event.y}")
                        if (rect.contains(event.x, event.y)) {
                            currentLevelIndex = lastClearedLevelIndex + 1
                            applyLevel()
                            Log.d("ChallengeGameView", "Continuing to next level, currentLevelIndex=$currentLevelIndex")
                            lifecycleScope.launch {
                                delay(200)
                                isProcessingTouch = false
                                Log.d("ChallengeGameView", "isProcessingTouch reset to false")
                            }
                            return true
                        }
                    }
                } else {
                    // Chỉ kiểm tra btnLeaderboard nếu đã hoàn thành hết (tương ứng với khi nó được vẽ)
                    btnLeaderboardRect?.let { rect ->
                        Log.d("ChallengeGameView", "Checking Leaderboard button: rect=$rect, touchX=${event.x}, touchY=${event.y}")
                        if (rect.contains(event.x, event.y)) {
                            onLeaderboard?.invoke()
                            lifecycleScope.launch {
                                delay(200)
                                isProcessingTouch = false
                                Log.d("ChallengeGameView", "isProcessingTouch reset to false")
                            }
                            return true
                        }
                    }
                }

                // Các nút khác luôn kiểm tra (vì luôn vẽ ở WIN)
                btnRestartRect?.let { rect ->
                    Log.d("ChallengeGameView", "Checking Restart button: rect=$rect, touchX=${event.x}, touchY=${event.y}")
                    if (rect.contains(event.x, event.y)) {
                        currentLevelIndex = 0
                        lastClearedLevelIndex = -1
                        totalGameStartTime = System.currentTimeMillis()
                        totalPausedTime = 0L
                        player.reset(screenW, screenH)
                        player.coins = 0
                        tvCoin?.post { tvCoin?.text = "0" }
                        gameEndTime = null
                        applyLevel()
                        Log.d("ChallengeGameView", "Restart game, reset to MARS")
                        lifecycleScope.launch {
                            delay(200)
                            isProcessingTouch = false
                            Log.d("ChallengeGameView", "isProcessingTouch reset to false")
                        }
                        return true
                    }
                }

                btnMenuRect?.let { rect ->
                    Log.d("ChallengeGameView", "Checking Menu button: rect=$rect, touchX=${event.x}, touchY=${event.y}")
                    if (rect.contains(event.x, event.y)) {
                        onBackToMenu?.invoke()
                        lifecycleScope.launch {
                            delay(200)
                            isProcessingTouch = false
                            Log.d("ChallengeGameView", "isProcessingTouch reset to false")
                        }
                        return true
                    }
                }

                Log.d("ChallengeGameView", "No button matched for touch at x=${event.x}, y=${event.y}")
                lifecycleScope.launch {
                    delay(200)
                    isProcessingTouch = false
                    Log.d("ChallengeGameView", "isProcessingTouch reset to false")
                }
            } else if (gameState == GameState.GAME_OVER) {
                btnRestartRect?.let { rect ->
                    Log.d("ChallengeGameView", "Checking Restart button: rect=$rect, touchX=${event.x}, touchY=${event.y}")
                    if (rect.contains(event.x, event.y)) {
                        currentLevelIndex = 0
                        lastClearedLevelIndex = -1
                        totalGameStartTime = System.currentTimeMillis()
                        totalPausedTime = 0L
                        player.reset(screenW, screenH)
                        player.coins = 0
                        tvCoin?.post { tvCoin?.text = "0" }
                        gameEndTime = null
                        applyLevel()
                        Log.d("ChallengeGameView", "Restart game, reset to MARS")
                        lifecycleScope.launch {
                            delay(200)
                            isProcessingTouch = false
                            Log.d("ChallengeGameView", "isProcessingTouch reset to false")
                        }
                        return true
                    }
                }
                btnMenuRect?.let { rect ->
                    Log.d("ChallengeGameView", "Checking Menu button: rect=$rect, touchX=${event.x}, touchY=${event.y}")
                    if (rect.contains(event.x, event.y)) {
                        onBackToMenu?.invoke()
                        lifecycleScope.launch {
                            delay(200)
                            isProcessingTouch = false
                            Log.d("ChallengeGameView", "isProcessingTouch reset to false")
                        }
                        return true
                    }
                }
            } else if (gameState == GameState.PAUSED) {
                btnRestartRect?.let { rect ->
                    Log.d("ChallengeGameView", "Checking Restart button: rect=$rect, touchX=${event.x}, touchY=${event.y}")
                    if (rect.contains(event.x, event.y)) {
                        resume()
                        lifecycleScope.launch {
                            delay(200)
                            isProcessingTouch = false
                            Log.d("ChallengeGameView", "isProcessingTouch reset to false")
                        }
                        return true
                    }
                }
                btnMenuRect?.let { rect ->
                    Log.d("ChallengeGameView", "Checking Menu button: rect=$rect, touchX=${event.x}, touchY=${event.y}")
                    if (rect.contains(event.x, event.y)) {
                        onBackToMenu?.invoke()
                        lifecycleScope.launch {
                            delay(200)
                            isProcessingTouch = false
                            Log.d("ChallengeGameView", "isProcessingTouch reset to false")
                        }
                        return true
                    }
                }
            }
            Log.d("ChallengeGameView", "No button matched for touch at x=${event.x}, y=${event.y}")
            lifecycleScope.launch {
                delay(200)
                isProcessingTouch = false
                Log.d("ChallengeGameView", "isProcessingTouch reset to false")
            }
        } else if (event.action == MotionEvent.ACTION_UP) {
            Log.d("ChallengeGameView", "Touch event ignored: isProcessingTouch=$isProcessingTouch")
        }
        return entityManager.handleTouch(event) || super.onTouchEvent(event)
    }

    fun onLevelCompleted() {
        lastClearedLevelIndex = currentLevelIndex
        gameState = GameState.WIN
        gameEndTime = System.currentTimeMillis()
        if (lastClearedLevelIndex == levels.size - 1) {
            saveGameTime(getTotalGameTime())
        }
        Log.d("ChallengeGameView", "Level completed, lastClearedLevelIndex: $lastClearedLevelIndex, level: ${levels[lastClearedLevelIndex]}, player.hp=${player.hp}")
    }

    override fun getGameTime(): Long {
        return if (lastClearedLevelIndex == levels.size - 1 && gameState == GameState.WIN) {
            getTotalGameTime()
        } else {
            super.getGameTime()
        }
    }

    override fun saveGameTime(time: Long) {
        if (lastClearedLevelIndex == levels.size - 1) {
            saveChallengeTime(time)
        }
    }

    override fun getTopTimes(): List<Long> {
        return if (lastClearedLevelIndex == levels.size - 1 && gameState == GameState.WIN) {
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
        surfaceReady = false
        releaseResources()
        super.surfaceDestroyed(holder)
        Log.d("ChallengeGameView", "surfaceDestroyed in ChallengeGameView, surfaceReady=$surfaceReady")
    }

    private fun releaseResources() {
        scaledVictory?.recycle()
        scaledVictory = null
        scaledSkull?.recycle()
        scaledSkull = null
        scaledTrophy?.recycle()
        scaledTrophy = null
        scaledPause?.recycle()
        scaledPause = null
        isResourcesInitialized = false
        Log.d("ChallengeGameView", "Đã giải phóng tài nguyên bitmap")
    }

    override fun pause() {
        if (gameState == GameState.RUNNING) {
            pauseStartTime = System.currentTimeMillis()
            super.pause()
            gameState = GameState.PAUSED
        }
    }

    override fun resume() {
        if (gameState == GameState.PAUSED && pauseStartTime != null) {
            totalPausedTime += System.currentTimeMillis() - pauseStartTime!!
            pauseStartTime = null
        }
        super.resume()
        gameState = GameState.RUNNING
    }

    private fun getTotalGameTime(): Long {
        return if (gameEndTime != null) {
            gameEndTime!! - totalGameStartTime - totalPausedTime
        } else {
            System.currentTimeMillis() - totalGameStartTime - totalPausedTime
        }
    }

    private fun saveChallengeTime(time: Long) {
        val topTimesStr = challengePrefs.getString("top_times", "") ?: ""
        val topTimes = topTimesStr.split(",").mapNotNull { it.toLongOrNull() }.toMutableList()
        topTimes.add(time)
        topTimes.sort()
        if (topTimes.size > 10) topTimes.removeAt(topTimes.size - 1)
        challengePrefs.edit().putString("top_times", topTimes.joinToString(",")).apply()
        Log.d("ChallengeGameView", "Saved challenge time: $time")
    }

    private fun getChallengeTopTimes(): List<Long> {
        val topTimesStr = challengePrefs.getString("top_times", "") ?: ""
        return topTimesStr.split(",").mapNotNull { it.toLongOrNull() }.sorted()
    }
}