package com.example.game2d.core

import android.content.Context
import android.content.SharedPreferences
import android.graphics.*
import android.view.SurfaceHolder
import android.view.SurfaceView
import android.view.MotionEvent
import androidx.core.content.res.ResourcesCompat
import com.example.game2d.R
import com.example.game2d.entities.Player
import com.example.game2d.managers.EntityManager
import com.example.game2d.managers.CollisionManager
import com.example.game2d.graphics.Renderer
import android.os.Vibrator
import android.os.VibrationEffect
import android.util.AttributeSet
import android.util.Log
import com.example.game2d.managers.MusicManager
import com.example.game2d.managers.SoundManager
import com.example.game2d.managers.DifficultyManager
import android.widget.TextView

open class GameView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : SurfaceView(context, attrs, defStyleAttr), SurfaceHolder.Callback {

    enum class GameState { RUNNING, GAME_OVER, WIN, PAUSED }

    var lastHitTime: Long = 0L
    var tvCoin: TextView? = null
    private var gameStartTime: Long = 0L // Thời gian bắt đầu trận
    private var gameEndTime: Long? = null // Thời gian kết thúc trận
    private val sharedPrefs: SharedPreferences = context.getSharedPreferences("leaderboard", Context.MODE_PRIVATE)

    companion object {
        const val SCALE_FACTOR = 2f
    }
    var onGameEnd: (() -> Unit)? = null
    var onLeaderboard: (() -> Unit)? = null
    var gameState = GameState.RUNNING
        set(value) {
            field = value
            if (value == GameState.GAME_OVER || value == GameState.WIN) {
                overlayStartTime = System.currentTimeMillis()
                gameEndTime = System.currentTimeMillis() // Lưu thời gian kết thúc
                if (value == GameState.WIN) {
                    // Lưu thời gian chơi khi thắng (mili giây)
                    val time = getGameTime()
                    saveGameTime(time)
                    // Lưu gameEndTime vào SharedPreferences
                    sharedPrefs.edit().putLong("last_game_end_time", gameEndTime ?: 0L).apply()
                    Log.d("GameView", "Saved gameEndTime: $gameEndTime")
                }
                onGameEnd?.invoke()
            }
        }

    val player = Player()
    private var gameLoop: GameLoop? = null

    val currentFPS: Int
        get() = gameLoop?.currentFPS ?: 0

    val entityManager = EntityManager(this)
    val collisionManager = CollisionManager(this)
    val renderer = Renderer(this)

    var onBackToMenu: (() -> Unit)? = null
    var onRestart: (() -> Unit)? = null

    var screenW = 0
    var screenH = 0

    private var btnMenuRect: RectF? = null
    private var btnRestartRect: RectF? = null
    private var btnLeaderboardRect: RectF? = null

    private var overlayStartTime = 0L

    private val skullBitmap by lazy { BitmapFactory.decodeResource(resources, R.drawable.you_lose) }
    private val trophyBitmap by lazy { BitmapFactory.decodeResource(resources, R.drawable.congratulations) }
    private val pauseBitmap by lazy { BitmapFactory.decodeResource(resources, R.drawable.ic_pause_bg) }
    private var scaledPause: Bitmap? = null
    private var scaledSkull: Bitmap? = null
    private var scaledTrophy: Bitmap? = null

    init {
        holder.addCallback(this)
        MusicManager.init(context)
    }

    override fun surfaceCreated(holder: SurfaceHolder) {
        screenW = holder.surfaceFrame.width()
        screenH = holder.surfaceFrame.height()
        entityManager.initResources(screenW, screenH)
        DifficultyManager.apply("MARS", this)
        gameLoop = GameLoop(this, holder).apply { startLoop() }
        // Khôi phục trạng thái nếu gameState là WIN
        if (gameState == GameState.WIN) {
            gameEndTime = sharedPrefs.getLong("last_game_end_time", 0L)
            if (gameEndTime == 0L) {
                gameEndTime = System.currentTimeMillis()
            }
            Log.d("GameView", "Restored gameEndTime: $gameEndTime")
        } else {
            gameStartTime = System.currentTimeMillis()
            gameEndTime = null
        }

        val iconSize = (screenW * 0.7f).toInt()
        scaledSkull = Bitmap.createScaledBitmap(skullBitmap, iconSize, iconSize, true)
        scaledTrophy = Bitmap.createScaledBitmap(trophyBitmap, iconSize, iconSize, true)
        scaledPause = Bitmap.createScaledBitmap(pauseBitmap, iconSize, iconSize, true)

        val btnWidth = screenW * 0.5f
        val btnHeight = 150f
        val centerX = screenW / 2f

        btnRestartRect = RectF(
            centerX - btnWidth / 2,
            screenH / 2f + 100,
            centerX + btnWidth / 2,
            screenH / 2f + 100 + btnHeight
        )

        btnMenuRect = RectF(
            centerX - btnWidth / 2,
            screenH / 2f + 300,
            centerX + btnWidth / 2,
            screenH / 2f + 300 + btnHeight
        )

        btnLeaderboardRect = RectF(
            centerX - btnWidth / 2,
            screenH / 2f + 500,
            centerX + btnWidth / 2,
            screenH / 2f + 500 + btnHeight
        )

        SoundManager.init(context)
        MusicManager.start(context)
    }

    override fun surfaceDestroyed(holder: SurfaceHolder) {
        gameLoop?.stopLoop()
        SoundManager.release()
        MusicManager.pause()
    }

    override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {}

    override fun onTouchEvent(event: MotionEvent): Boolean {
        return when (gameState) {
            GameState.GAME_OVER, GameState.WIN -> {
                if (event.action == MotionEvent.ACTION_DOWN) {
                    btnRestartRect?.let {
                        if (it.contains(event.x, event.y)) onRestart?.invoke()
                    }
                    btnMenuRect?.let {
                        if (it.contains(event.x, event.y)) onBackToMenu?.invoke()
                    }
                    btnLeaderboardRect?.let {
                        if (it.contains(event.x, event.y)) onLeaderboard?.invoke()
                    }
                }
                true
            }
            GameState.PAUSED -> {
                if (event.action == MotionEvent.ACTION_DOWN) {
                    btnRestartRect?.let {
                        if (it.contains(event.x, event.y)) {
                            gameState = GameState.RUNNING
                            resume()
                        }
                    }
                    btnMenuRect?.let {
                        if (it.contains(event.x, event.y)) onBackToMenu?.invoke()
                    }
                }
                true
            }
            else -> entityManager.handleTouch(event)
        }
    }

    fun update(deltaTime: Float) {
        if (gameState == GameState.RUNNING) {
            entityManager.update(deltaTime)
            collisionManager.checkCollisions()
            if (player.isInvincible && System.currentTimeMillis() >= player.invincibilityEndTime) {
                player.isInvincible = false
            }
        }
    }

    fun vibrate(duration: Long = 100) {
        val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        if (vibrator.hasVibrator()) {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                vibrator.vibrate(
                    VibrationEffect.createOneShot(duration, VibrationEffect.DEFAULT_AMPLITUDE)
                )
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(duration)
            }
        }
    }

    fun onPlayerHit(damage: Int = 1) {
        if (player.isInvincible) return
        if (player.shield > 0) {
            player.shield -= damage
            if (player.shield < 0) player.shield = 0
            vibrate(50)
        } else {
            player.hp -= damage
            lastHitTime = System.currentTimeMillis()
            vibrate(120)
            if (player.hp <= 0 && gameState == GameState.RUNNING) {
                Log.d("GameView", "Player died -> GAME_OVER")
                gameState = GameState.GAME_OVER
            }
        }
    }

    fun getGameTime(): Long {
        return if (gameState == GameState.WIN || gameState == GameState.GAME_OVER) {
            gameEndTime?.minus(gameStartTime) ?: 0L // Trả về thời gian cố định khi trận kết thúc
        } else {
            System.currentTimeMillis() - gameStartTime // Cập nhật thời gian khi đang chơi
        }
    }

    private fun saveGameTime(time: Long) {
        val editor = sharedPrefs.edit()
        val topTimes = sharedPrefs.getString("top_times", "")?.split(",")?.mapNotNull { it.toLongOrNull() }?.toMutableList() ?: mutableListOf()
        topTimes.add(time)
        topTimes.sort()
        if (topTimes.size > 10) topTimes.removeAt(topTimes.size - 1) // Giới hạn top 10
        editor.putString("top_times", topTimes.joinToString(","))
        editor.apply()
        Log.d("GameView", "Saved time: $time ms, topTimes: $topTimes")
    }

    fun getTopTimes(): List<Long> {
        return sharedPrefs.getString("top_times", "")?.split(",")?.mapNotNull { it.toLongOrNull() } ?: emptyList()
    }

    fun render(canvas: Canvas) {
        Log.v("GameView", "render(): state=$gameState")
        renderer.draw(canvas)
        if (System.currentTimeMillis() - lastHitTime < 150) {
            val flashPaint = Paint().apply { color = Color.argb(80, 255, 0, 0) }
            canvas.drawRect(0f, 0f, screenW.toFloat(), screenH.toFloat(), flashPaint)
        }
        when (gameState) {
            GameState.GAME_OVER -> drawOverlay(canvas, "Thất Bại!")
            GameState.WIN -> drawOverlay(canvas, "Chiến Thắng!")
            GameState.PAUSED -> drawOverlay(canvas, "Tạm dừng")
            else -> {}
        }
    }

    private fun drawOverlay(canvas: Canvas, title: String) {
        val elapsed = (System.currentTimeMillis() - overlayStartTime) / 1000f
        val customFont = ResourcesCompat.getFont(context, R.font.robotomono_bold)

        canvas.drawRect(
            0f, 0f, screenW.toFloat(), screenH.toFloat(),
            Paint().apply { color = Color.argb(180, 0, 0, 0) }
        )

        val bannerWidth = screenW * 0.8f
        val bannerRect = RectF(
            screenW / 2f - bannerWidth / 2,
            screenH / 2f - 420,
            screenW / 2f + bannerWidth / 2,
            screenH / 2f - 140
        )

        val bannerColors = if (gameState == GameState.WIN)
            intArrayOf(Color.argb(220, 0, 255, 150), Color.argb(220, 0, 120, 50))
        else
            intArrayOf(Color.argb(220, 255, 80, 80), Color.argb(220, 150, 0, 0))

        val bannerPaint = Paint().apply {
            shader = LinearGradient(
                bannerRect.left, bannerRect.top,
                bannerRect.right, bannerRect.bottom,
                bannerColors, null, Shader.TileMode.CLAMP
            )
        }

        val borderColor = if (gameState == GameState.WIN) Color.CYAN else Color.RED
        val borderPaint = Paint().apply {
            color = borderColor
            style = Paint.Style.STROKE
            strokeWidth = 8f
            setShadowLayer(15f, 0f, 0f, borderColor)
        }

        canvas.drawRoundRect(bannerRect, 40f, 40f, bannerPaint)
        canvas.drawRoundRect(bannerRect, 40f, 40f, borderPaint)

        when (gameState) {
            GameState.WIN -> scaledTrophy?.let {
                val offsetY = 100f
                val iconX = screenW / 2f - it.width / 2
                val iconY = offsetY
                canvas.drawBitmap(it, iconX, iconY, null)
            }
            GameState.GAME_OVER -> scaledSkull?.let {
                val offsetY = 100f
                val iconX = screenW / 2f - it.width / 2
                val iconY = offsetY
                canvas.drawBitmap(it, iconX, iconY, null)
            }
            GameState.PAUSED -> scaledPause?.let { bmp ->
                val offsetY = 150f
                val left = screenW / 2f - bmp.width / 2
                val top = offsetY
                canvas.drawBitmap(bmp, left, top, null)
            }
            else -> {}
        }

        val scale = 1f + 0.05f * kotlin.math.sin(elapsed * 2)
        val titlePaint = Paint().apply {
            color = Color.WHITE
            textSize = screenW * 0.1f * scale
            textAlign = Paint.Align.CENTER
            typeface = customFont
            setShadowLayer(20f, 0f, 0f, borderColor)
        }
        canvas.drawText(title, bannerRect.centerX(), bannerRect.centerY() + 40f, titlePaint)

        if (gameState == GameState.WIN) {
            // Vẽ nền cho bảng xếp hạng
            val leaderboardWidth = screenW * 0.7f
            val leaderboardHeight = 320f
            val leaderboardRect = RectF(
                screenW / 2f - leaderboardWidth / 2,
                screenH / 2f + 650, // Ngay dưới nút "Bảng xếp hạng" (500 + 150)
                screenW / 2f + leaderboardWidth / 2,
                screenH / 2f + 650 + leaderboardHeight
            )
            val leaderboardBgPaint = Paint().apply {
                shader = LinearGradient(
                    leaderboardRect.left, leaderboardRect.top,
                    leaderboardRect.right, leaderboardRect.bottom,
                    intArrayOf(Color.argb(220, 0, 150, 255), Color.argb(220, 0, 50, 150)),
                    null, Shader.TileMode.CLAMP
                )
            }
            val leaderboardBorderPaint = Paint().apply {
                color = Color.argb(255, 0, 255, 255)
                style = Paint.Style.STROKE
                strokeWidth = 6f
                setShadowLayer(12f, 0f, 0f, Color.CYAN)
            }
            canvas.drawRoundRect(leaderboardRect, 30f, 30f, leaderboardBgPaint)
            canvas.drawRoundRect(leaderboardRect, 30f, 30f, leaderboardBorderPaint)

            // Vẽ tiêu đề bảng xếp hạng
            val leaderboardTitlePaint = Paint().apply {
                color = Color.WHITE
                textSize = screenW * 0.04f * (1f + 0.03f * kotlin.math.sin(elapsed * 3)) // Hiệu ứng phóng to
                textAlign = Paint.Align.CENTER
                typeface = customFont
                setShadowLayer(15f, 0f, 0f, Color.YELLOW)
            }
            canvas.drawText("Bảng Xếp Hạng", leaderboardRect.centerX(), leaderboardRect.top + 60f, leaderboardTitlePaint)

            // Vẽ top 1, 2, 3 và thời gian hiện tại
            val topTimes = getTopTimes()
            Log.d("GameView", "Top times in drawOverlay: $topTimes") // Debug
            topTimes.take(3).forEachIndexed { index, time ->
                val rank = index + 1
                val minutes = time / 60000
                val seconds = (time % 60000) / 1000
                val milliseconds = time % 1000
                val text = "Top $rank: %02d:%02d.%03d".format(minutes, seconds, milliseconds)
                val leaderboardPaint = Paint().apply {
                    color = when (rank) {
                        1 -> Color.rgb(255, 215, 0) // Vàng cho Top 1
                        2 -> Color.rgb(192, 192, 192) // Bạc cho Top 2
                        else -> Color.rgb(205, 127, 50) // Đồng cho Top 3
                    }
                    textSize = screenW * 0.035f
                    textAlign = Paint.Align.CENTER
                    typeface = customFont
                    setShadowLayer(10f, 0f, 0f, Color.BLACK)
                }
                canvas.drawText(text, leaderboardRect.centerX(), leaderboardRect.top + 120f + index * 60f, leaderboardPaint)
            }
            if (topTimes.isEmpty()) {
                val leaderboardPaint = Paint().apply {
                    color = Color.YELLOW
                    textSize = screenW * 0.005f
                    textAlign = Paint.Align.CENTER
                    typeface = customFont
                    setShadowLayer(10f, 0f, 0f, Color.BLACK)
                }
                canvas.drawText("Chưa có dữ liệu", leaderboardRect.centerX(), leaderboardRect.top + 120f, leaderboardPaint)
            }
            val currentTime = getGameTime()
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

        val glowAlpha = (128 + 127 * kotlin.math.sin(elapsed * 4)).toInt()
        val buttonBorder = Paint().apply {
            color = Color.argb(glowAlpha, 0, 255, 255)
            style = Paint.Style.STROKE
            strokeWidth = 6f
            setShadowLayer(12f, 0f, 0f, Color.CYAN)
        }

        val buttonTextPaint = Paint().apply {
            color = Color.WHITE
            textSize = screenW * 0.05f
            textAlign = Paint.Align.CENTER
            typeface = customFont
            setShadowLayer(10f, 0f, 0f, Color.CYAN)
        }

        fun drawButton(rect: RectF, text: String) {
            val btnPaint = Paint().apply {
                shader = LinearGradient(
                    rect.left, rect.top, rect.right, rect.bottom,
                    intArrayOf(Color.argb(220, 0, 255, 255), Color.argb(220, 0, 100, 200)),
                    null, Shader.TileMode.CLAMP
                )
            }
            canvas.drawRoundRect(rect, 25f, 25f, btnPaint)
            canvas.drawRoundRect(rect, 25f, 25f, buttonBorder)
            val textY = rect.centerY() - (buttonTextPaint.descent() + buttonTextPaint.ascent()) / 2
            canvas.drawText(text, rect.centerX(), textY, buttonTextPaint)
        }

        if (gameState == GameState.GAME_OVER || gameState == GameState.WIN) {
            btnRestartRect?.let { drawButton(it, "▶ Chơi lại") }
            btnMenuRect?.let { drawButton(it, "☰ Home") }
            btnLeaderboardRect?.let { drawButton(it, "🏆 Bảng xếp hạng") }
        } else if (gameState == GameState.PAUSED) {
            btnRestartRect?.let { drawButton(it, "▶ Tiếp tục") }
            btnMenuRect?.let { drawButton(it, "☰ Home") }
        }
    }

    fun pause() {
        gameLoop?.stopLoop()
        MusicManager.pause()
    }

    fun resume() {
        if (gameLoop == null || !gameLoop!!.isRunning) {
            gameLoop = GameLoop(this, holder).apply { startLoop() }
        }
        if (MusicManager.isMusicEnabled()) {
            MusicManager.start(context)
        }
    }
}