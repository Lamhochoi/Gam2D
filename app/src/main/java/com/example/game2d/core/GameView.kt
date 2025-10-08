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
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.delay

open class GameView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : SurfaceView(context, attrs, defStyleAttr), SurfaceHolder.Callback, LifecycleOwner {

    // Thêm Lifecycle để dùng lifecycleScope
    private val lifecycleRegistry = LifecycleRegistry(this)
    override val lifecycle: Lifecycle get() = lifecycleRegistry

    init {
        lifecycleRegistry.currentState = Lifecycle.State.CREATED
        holder.addCallback(this)
        MusicManager.init(context) // Khởi tạo MusicManager trong init
    }

    enum class GameState { RUNNING, GAME_OVER, WIN, PAUSED }

    var lastHitTime: Long = 0L
    var tvCoin: TextView? = null
    protected var gameStartTime: Long = 0L
    protected var gameEndTime: Long? = null
    protected val sharedPrefs: SharedPreferences = context.getSharedPreferences("leaderboard", Context.MODE_PRIVATE)
    protected var pausedTime: Long = 0L

    companion object {
        const val SCALE_FACTOR = 2f
    }
    var onGameEnd: (() -> Unit)? = null
    var onLeaderboard: (() -> Unit)? = null

    open var gameState = GameState.RUNNING
        set(value) {
            val previousState = field
            field = value
            when (value) {
                GameState.PAUSED -> {
                    pausedTime = System.currentTimeMillis() - gameStartTime
                    pause()
                }
                GameState.RUNNING -> {
                    if (previousState == GameState.PAUSED) {
                        gameStartTime = System.currentTimeMillis() - pausedTime
                    }
                }
                GameState.GAME_OVER, GameState.WIN -> {
                    overlayStartTime = System.currentTimeMillis()
                    gameEndTime = System.currentTimeMillis()
                    if (value == GameState.WIN) {
                        val time = getGameTime()
                        saveGameTime(time)
                        sharedPrefs.edit().putLong("last_game_end_time", gameEndTime ?: 0L).apply()
                        Log.d("GameView", "Saved gameEndTime: $gameEndTime")
                    }
                    onGameEnd?.invoke()
                }
                else -> {}
            }
        }

    val player = Player()
    protected var gameLoop: GameLoop? = null

    val currentFPS: Int
        get() = gameLoop?.currentFPS ?: 0

    open val entityManager = EntityManager(this)
    open val collisionManager = CollisionManager(this)
    open val renderer = Renderer(this)

    var onBackToMenu: (() -> Unit)? = null
    var onRestart: (() -> Unit)? = null

    var screenW = 0
    var screenH = 0

    protected var btnMenuRect: RectF? = null
    protected var btnRestartRect: RectF? = null
    protected var btnLeaderboardRect: RectF? = null

    protected var overlayStartTime = 0L

    protected val skullBitmap by lazy { BitmapFactory.decodeResource(resources, R.drawable.you_lose) }
    protected val trophyBitmap by lazy { BitmapFactory.decodeResource(resources, R.drawable.congratulations) }
    protected val pauseBitmap by lazy { BitmapFactory.decodeResource(resources, R.drawable.ic_pause_bg) }
    protected var scaledSkull: Bitmap? = null
    protected var scaledTrophy: Bitmap? = null
    protected var scaledPause: Bitmap? = null

    protected var isResourcesInitialized = false
    protected var surfaceReady = false

    // Lazy cho bitmaps chung
    protected val scaledSkullLazy by lazy { scaleBitmap(skullBitmap) }
    protected val scaledTrophyLazy by lazy { scaleBitmap(trophyBitmap) }
    protected val scaledPauseLazy by lazy { scaleBitmap(pauseBitmap) }

    private fun scaleBitmap(bitmap: Bitmap): Bitmap {
        val iconSize = (screenW * 0.5f).toInt()
        return Bitmap.createScaledBitmap(bitmap, iconSize, iconSize, true)
    }

    override fun surfaceCreated(holder: SurfaceHolder) {
        Log.d("GameView", "surfaceCreated() bắt đầu")
        surfaceReady = true
        screenW = holder.surfaceFrame.width()
        screenH = holder.surfaceFrame.height()
        lifecycleRegistry.currentState = Lifecycle.State.STARTED

        // Chạy loading bằng coroutine
        lifecycleScope.launch {
            initResourcesAndStart()
        }
    }

    protected open suspend fun initResourcesAndStart() {
        if (isResourcesInitialized) return

        Log.d("GameView", "Đang load resources chung...")

        try {
            // Chạy I/O tasks trên Dispatchers.IO
            withContext(Dispatchers.IO) {
                // Load bitmaps
                scaledSkull = scaledSkullLazy
                scaledTrophy = scaledTrophyLazy
                scaledPause = scaledPauseLazy

                // Init managers nếu chưa init
                if (!SoundManager.isInitialized()) {
                    Log.d("GameView", "Khởi tạo SoundManager...")
                    SoundManager.init(context)
                } else {
                    Log.d("GameView", "SoundManager đã khởi tạo")
                }
                if (!MusicManager.isInitialized()) {
                    Log.d("GameView", "Khởi tạo MusicManager...")
                    MusicManager.init(context)
                } else {
                    Log.d("GameView", "MusicManager đã khởi tạo")
                }
                entityManager.initResources(context,screenW, screenH)
                DifficultyManager.apply("MARS", this@GameView)
            }

            // Setup buttons và thời gian trên Main thread
            withContext(Dispatchers.Main) {
                val btnWidth = screenW * 0.5f
                val btnHeight = 150f
                val centerX = screenW / 2f
                btnRestartRect = RectF(centerX - btnWidth / 2, screenH / 2f + 100, centerX + btnWidth / 2, screenH / 2f + 100 + btnHeight)
                btnMenuRect = RectF(centerX - btnWidth / 2, screenH / 2f + 300, centerX + btnWidth / 2, screenH / 2f + 300 + btnHeight)
                btnLeaderboardRect = RectF(centerX - btnWidth / 2, screenH / 2f + 500, centerX + btnWidth / 2, screenH / 2f + 500 + btnHeight)

                if (gameState == GameState.WIN) {
                    gameEndTime = sharedPrefs.getLong("last_game_end_time", 0L)
                    if (gameEndTime == 0L) gameEndTime = System.currentTimeMillis()
                } else {
                    gameStartTime = System.currentTimeMillis()
                    gameEndTime = null
                }

                loadAdditionalResources()
            }

            isResourcesInitialized = true
            Log.d("GameView", "Load resources chung hoàn tất")

            // Start game loop và music
            withContext(Dispatchers.Main) {
                if (surfaceReady && isResourcesInitialized) {
                    gameLoop = GameLoop(this@GameView, holder).apply { startLoop() }
                    if (MusicManager.isMusicEnabled()) {
                        MusicManager.start(context)
                        Log.d("GameView", "MusicManager started")
                    }
                    SoundManager.playShoot() // Test sound
                    Log.d("GameView", "Test playShoot")
                } else {
                    delay(100)
                    initResourcesAndStart()
                }
            }
        } catch (e: Exception) {
            Log.e("GameView", "Lỗi load resources", e)
            isResourcesInitialized = false
        }
    }

    protected open suspend fun loadAdditionalResources() {
        // Không làm gì ở base
    }

    override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {}

    override fun surfaceDestroyed(holder: SurfaceHolder) {
        surfaceReady = false
        lifecycleRegistry.currentState = Lifecycle.State.DESTROYED
        gameLoop?.stopLoop()
        gameLoop = null
        SoundManager.release()
        MusicManager.pause()
    }

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

    open fun update(deltaTime: Float) {
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
            vibrate(100)
            if (player.hp <= 0 && gameState == GameState.RUNNING) {
                Log.d("GameView", "Player died -> GAME_OVER")
                gameState = GameState.GAME_OVER
            }
        }
    }

    open fun getGameTime(): Long {
        return when (gameState) {
            GameState.PAUSED -> pausedTime
            GameState.WIN, GameState.GAME_OVER -> gameEndTime?.minus(gameStartTime) ?: 0L
            else -> System.currentTimeMillis() - gameStartTime
        }
    }

    open fun saveGameTime(time: Long) {
        val editor = sharedPrefs.edit()
        val topTimesStr = sharedPrefs.getString("top_times", "") ?: ""
        val topTimes = topTimesStr.split(",").mapNotNull { it.toLongOrNull() }.toMutableList()
        topTimes.add(time)
        topTimes.sort()
        if (topTimes.size > 10) topTimes.removeAt(topTimes.size - 1)
        editor.putString("top_times", topTimes.joinToString(",")).apply()
    }

    open fun getTopTimes(): List<Long> {
        val topTimesStr = sharedPrefs.getString("top_times", "") ?: ""
        return topTimesStr.split(",").mapNotNull { it.toLongOrNull() }.sorted()
    }

    open fun onLeaderboard() {
        // Override in subclasses
    }

    fun render(canvas: Canvas) {
        if (!isResourcesInitialized || !surfaceReady) {
            val paint = Paint().apply {
                color = Color.WHITE
                textSize = 50f
                textAlign = Paint.Align.CENTER
            }
            canvas.drawColor(Color.BLACK)
            canvas.drawText("Loading...", screenW / 2f, screenH / 2f, paint)
            return
        }
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

    open fun drawOverlay(canvas: Canvas, title: String) {
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
            val leaderboardWidth = screenW * 0.7f
            val leaderboardHeight = 320f
            val leaderboardRect = RectF(
                screenW / 2f - leaderboardWidth / 2,
                screenH / 2f + 750,
                screenW / 2f + leaderboardWidth / 2,
                screenH / 2f + 750 + leaderboardHeight
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

            val leaderboardTitlePaint = Paint().apply {
                color = Color.WHITE
                textSize = screenW * 0.04f * (1f + 0.04f * kotlin.math.sin(elapsed * 3))
                textAlign = Paint.Align.CENTER
                typeface = customFont
                setShadowLayer(15f, 0f, 0f, Color.YELLOW)
            }
            canvas.drawText("Bảng Xếp Hạng", leaderboardRect.centerX(), leaderboardRect.top + 60f, leaderboardTitlePaint)

            val topTimes = getTopTimes()
            Log.d("GameView", "Top times in drawOverlay: $topTimes")
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
            if (topTimes.isEmpty()) {
                val leaderboardPaint = Paint().apply {
                    color = Color.YELLOW
                    textSize = screenW * 0.035f
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
                textSize = screenW * 0.040f
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

    open fun pause() {
        if (gameLoop?.isRunning == true) {
            gameLoop?.stopLoop()
            MusicManager.pause()
        }
    }

    open fun resume() {
        if (gameLoop == null || !gameLoop!!.isRunning) {
            gameLoop = GameLoop(this, holder).apply { startLoop() }
        }
        if (MusicManager.isMusicEnabled()) {
            MusicManager.start(context)
        }
    }
}
