package com.example.game2d.managers

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.RectF
import android.view.MotionEvent
import com.example.game2d.R
import com.example.game2d.core.GameView
import com.example.game2d.entities.*
import android.content.Context
import android.graphics.BitmapFactory.Options
import android.util.Log

class EntityManager(private val gameView: GameView) {
    var goal: Int = 0 // default, có thể override bởi DifficultyManager
    private var bgY1 = 0f
    private var bgY2 = 0f
    private var backgroundSpeed = 5f

    private val bulletPool = mutableListOf<Bullet>()
    val activeBullets = mutableListOf<Bullet>()
    private val enemyBulletPool = mutableListOf<Bullet>()
    val activeEnemyBullets = mutableListOf<Bullet>()
    private val enemyPool = mutableListOf<Enemy>()
    val activeEnemies = mutableListOf<Enemy>()
    private val bossPool = mutableListOf<Enemy>()

    // Vụ Nổ
    private val explosionPool = mutableListOf<Explosion>()
    val activeExplosions = mutableListOf<Explosion>()
    private var explosionFrames: List<Bitmap>? = null

    // ✅ Bổ sung FallingObject
    private val fallingObjectPool = mutableListOf<FallingObject>()
    val activeFallingObjects = mutableListOf<FallingObject>()

    private var bossSpawned = false
    var enemiesKilled = 0
        private set
    private var lastEnemySpawn: Long = 0

    private var playerBitmap: Bitmap? = null
    private var enemyBitmap: Bitmap? = null
    private var bossBitmap: Bitmap? = null
    private var bulletBitmap: Bitmap? = null
    private var backgroundBitmap: Bitmap? = null
    private var scaledBackground: Bitmap? = null

    // Cached scaled sprites
    private var cachedPlayerBitmap: Bitmap? = null
    private var cachedEnemyBitmap: Bitmap? = null
    private var cachedBossBitmap: Bitmap? = null
    private var cachedBulletBitmapPlayer: Bitmap? = null
    private var cachedBulletBitmapEnemy: Bitmap? = null

    // ✅ Thêm sprite FallingObject
    private var fallingBitmap: Bitmap? = null
    private var cachedFallingBitmap: Bitmap? = null
    private var fallingObjectSize: Int = 0

    // Kích thước bullet đã scale
    private var playerBulletSize: Int = 0
    private var enemyBulletSize: Int = 0

    // Flag báo đã nạp tài nguyên
    private var resourcesInitialized = false

    fun setBackground(context: Context, resId: Int, width: Int, height: Int) {
        val optsBounds = Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeResource(context.resources, resId, optsBounds)
        val sampleSize = calculateInSampleSize(optsBounds, width, height)
        val opts = Options().apply {
            inSampleSize = sampleSize
            inPreferredConfig = Bitmap.Config.RGB_565
        }
        val raw = BitmapFactory.decodeResource(context.resources, resId, opts)
        scaledBackground = if (raw.width != width || raw.height != height) {
            Bitmap.createScaledBitmap(raw, width, height, true)
        } else raw
    }

    private var isDraggingPlayer = false

    //Khởi tạo các tài nguyên
    fun initResources(screenW: Int, screenH: Int) {
        val res = gameView.resources
        // Explosion frames
        val explosionSheet = BitmapFactory.decodeResource(res, R.drawable.explosion)
        val frameCount = 6 // số frame trong ảnh explosion.png
        val frameW = explosionSheet.width / frameCount
        val frameH = explosionSheet.height

        explosionFrames = (0 until frameCount).map { i ->
            Bitmap.createBitmap(explosionSheet, i * frameW, 0, frameW, frameH)
        }

        initExplosionPool(20) // tạo sẵn 20 vụ nổ


        playerBitmap = BitmapFactory.decodeResource(res, R.drawable.player4)
        enemyBitmap = BitmapFactory.decodeResource(res, R.drawable.enemyred)
        bossBitmap = BitmapFactory.decodeResource(res, R.drawable.boss_end)
        bulletBitmap = BitmapFactory.decodeResource(res, R.drawable.bullet3)
        fallingBitmap = BitmapFactory.decodeResource(res, R.drawable.rock)
        //Player
        val playerSize = (screenW * 0.14f * GameView.SCALE_FACTOR).toInt()
        gameView.player.size = playerSize
        cachedPlayerBitmap = Bitmap.createScaledBitmap(playerBitmap!!, playerSize, playerSize, false)
        gameView.player.bitmap = cachedPlayerBitmap
        gameView.player.x = screenW / 2f - playerSize / 2
        gameView.player.y = screenH * 0.7f

        // Enemy + Boss
        initEnemyPool(20)

        // Bullet (scale 1 lần duy nhất)
        playerBulletSize = (screenW * 0.05f * GameView.SCALE_FACTOR).toInt()
        cachedBulletBitmapPlayer = Bitmap.createScaledBitmap(bulletBitmap!!, playerBulletSize, playerBulletSize, false)

        enemyBulletSize = (screenW * 0.03f * GameView.SCALE_FACTOR).toInt()
        cachedBulletBitmapEnemy = Bitmap.createScaledBitmap(bulletBitmap!!, enemyBulletSize, enemyBulletSize, false)

        // Bullet pool
        initBulletPool(50)

        // ✅ FallingObject pool
        initFallingObjectPool(15)

        // Reset trạng thái
        bgY1 = 0f
        bgY2 = -screenH.toFloat()
        bossSpawned = false
        enemiesKilled = 0

        resourcesInitialized = true
    }
    private fun initExplosionPool(size: Int) {
        if (explosionPool.size < size) {
            repeat(size - explosionPool.size) {
                explosionPool.add(Explosion(0f, 0f, active = false))
            }
        } else {
            explosionPool.forEach { it.active = false }
        }
    }


    private fun initBulletPool(size: Int) {
        if (bulletPool.size < size) {
            repeat(size - bulletPool.size) { bulletPool.add(Bullet(0f, 0f, bitmap = null, active = false)) }
        } else {
            bulletPool.forEach { it.active = false }
        }

        if (enemyBulletPool.size < size) {
            repeat(size - enemyBulletPool.size) { enemyBulletPool.add(Bullet(0f, 0f, bitmap = null, active = false)) }
        } else {
            enemyBulletPool.forEach { it.active = false }
        }
    }

    private fun initEnemyPool(size: Int) {
        val screenW = gameView.screenW
        val enemySize = (screenW * 0.06f * GameView.SCALE_FACTOR).toInt()
        cachedEnemyBitmap = Bitmap.createScaledBitmap(enemyBitmap!!, enemySize, enemySize, false)

        if (enemyPool.size < size) {
            repeat(size - enemyPool.size) {
                enemyPool.add(
                    Enemy(0f, 0f, enemySize,
                        speed = DifficultyManager.EnemyDefaults.speed.toFloat(),
                        hp = DifficultyManager.EnemyDefaults.hp,
                        maxHp = DifficultyManager.EnemyDefaults.hp,
                        bitmap = cachedEnemyBitmap,
                        active = false
                    )
                )
            }
        } else {
            enemyPool.forEach { e ->
                e.size = enemySize
                e.bitmap = cachedEnemyBitmap
                e.active = false
            }
        }

        val bossSize = (screenW * 0.3f * GameView.SCALE_FACTOR).toInt()
        cachedBossBitmap = Bitmap.createScaledBitmap(bossBitmap!!, bossSize, bossSize, false)

        if (bossPool.isEmpty()) {
            bossPool.add(
                Enemy(0f, 0f, bossSize,
                    speed = DifficultyManager.BossDefaults.speed.toFloat(),
                    hp = DifficultyManager.BossDefaults.hp,
                    maxHp = DifficultyManager.BossDefaults.hp,
                    isBoss = true,
                    bitmap = cachedBossBitmap,
                    active = false
                )
            )
        } else {
            bossPool.forEach { b ->
                b.size = bossSize
                b.bitmap = cachedBossBitmap
                b.active = false
            }
        }
    }
    // ✅ Hàm init FallingObject pool
    private fun initFallingObjectPool(size: Int) {
        val screenW = gameView.screenW
        fallingObjectSize = (screenW * 0.08f * GameView.SCALE_FACTOR).toInt()
        cachedFallingBitmap = Bitmap.createScaledBitmap(fallingBitmap!!, fallingObjectSize, fallingObjectSize, false)

        if (fallingObjectPool.size < size) {
            repeat(size - fallingObjectPool.size) {
                fallingObjectPool.add(
                    FallingObject(0f, 0f, fallingObjectSize, speed = 10f, bitmap = cachedFallingBitmap, active = false)
                )
            }
        } else {
            fallingObjectPool.forEach { obj ->
                obj.size = fallingObjectSize
                obj.bitmap = cachedFallingBitmap
                obj.active = false
            }
        }
    }

    private fun getBulletFromPool(): Bullet? = bulletPool.find { !it.active }?.also { it.active = true }
    private fun getEnemyBulletFromPool(): Bullet? = enemyBulletPool.find { !it.active }?.also { it.active = true }
    private fun getEnemyFromPool(): Enemy? = enemyPool.find { !it.active }?.also { it.active = true }
    private fun getBossFromPool(): Enemy? = bossPool.find { !it.active }?.also { it.active = true }
    private fun getExplosionFromPool(): Explosion? =
        explosionPool.find { !it.active }?.also { it.active = true }


    // ✅ Spawn FallingObject
    private fun spawnFallingObject() {
        val screenW = gameView.screenW
        fallingObjectPool.find { !it.active }?.apply {
            x = (0..(screenW - size)).random().toFloat()
            y = -size.toFloat()
            speed = (8..15).random().toFloat()
            bitmap = cachedFallingBitmap
            active = true
            activeFallingObjects.add(this)
        }
    }


    fun update(deltaTime: Float) {
        if (!resourcesInitialized) return
        if (gameView.gameState == GameView.GameState.WIN ||
            gameView.gameState == GameView.GameState.GAME_OVER) {
            return
        }

        val screenH = gameView.screenH
        val screenW = gameView.screenW

        // Background scroll
        bgY1 += backgroundSpeed * deltaTime * 60f
        bgY2 += backgroundSpeed * deltaTime * 60f
        if (bgY1 >= screenH) bgY1 = bgY2 - screenH
        if (bgY2 >= screenH) bgY2 = bgY1 - screenH

        // Player die check — chỉ set GAME_OVER khi đang RUNNING
        if (gameView.gameState == GameView.GameState.RUNNING && gameView.player.hp <= 0) {
            Log.d("EntityManager", "Player HP <= 0 -> GAME_OVER")
            gameView.gameState = GameView.GameState.GAME_OVER
            // không return ở đây; update() sẽ sớm early-return vì guard ở đầu hàm
        }


        val now = System.currentTimeMillis()

        // Player auto-shoot
        if (now - gameView.player.lastShot > gameView.player.shotDelay) {
            getBulletFromPool()?.let { bullet ->
                bullet.x = gameView.player.x + gameView.player.size / 2 - playerBulletSize / 2
                bullet.y = gameView.player.y
                bullet.size = playerBulletSize.toFloat()
                bullet.speed = 30f
                bullet.bitmap = cachedBulletBitmapPlayer
                activeBullets.add(bullet)

                // 🔊 Âm thanh bắn
                SoundManager.playShoot()
            }
            gameView.player.lastShot = now
        }

        // Spawn enemy
        if (now - lastEnemySpawn > 2000) {
            spawnEnemy()
            lastEnemySpawn = now
        }

        val iteratorEnemy = activeEnemies.iterator()
        while (iteratorEnemy.hasNext()) {
            val e = iteratorEnemy.next()
            if (e.active) {
                e.y += e.speed * deltaTime * 60f
                e.x += 2f * deltaTime * 60f * e.directionX
                // Kiểm tra va chạm với mép màn hình ngang
                if (e.x < 0) {
                    e.x = 0f
                    e.directionX *= -1
                    // 🔊 Âm thanh nảy khi chạm rìa
                    SoundManager.playEnemyBounce()
                } else if (e.x + e.size > screenW) {
                    e.x = (screenW - e.size).toFloat()
                    e.directionX *= -1
                    // 🔊 Âm thanh nảy khi chạm rìa
                    SoundManager.playEnemyBounce()
                }
                val maxY = (screenH * 0.66f).toInt() // enemy và boss chỉ ở nửa trên

                if (e.y < 0) {
                    e.y = 0f
                    e.speed = -e.speed
                } else if (e.y + e.size > maxY) {
                    e.y = (maxY - e.size).toFloat()
                    e.speed = -e.speed
                }

                if (now - e.lastShot > e.shotDelay) {
                    getEnemyBulletFromPool()?.let { bullet ->
                        bullet.x = e.x + e.size / 2 - enemyBulletSize / 2
                        bullet.y = e.y + e.size
                        bullet.size = enemyBulletSize.toFloat()
                        bullet.speed = 15f
                        bullet.angle = 90f
                        bullet.bitmap = cachedBulletBitmapEnemy
                        activeEnemyBullets.add(bullet)
                    }
                    e.lastShot = now
                }

                if (e.y > screenH) {
                    e.active = false
                    iteratorEnemy.remove()
                }
            }
        }

        // ✅ FIX: Sửa điều kiện spawn boss để phù hợp với goal từ DifficultyManager
        if (!bossSpawned && enemiesKilled >= goal) spawnBoss()

        activeBullets.removeAll { bullet ->
            bullet.y -= bullet.speed * deltaTime * 60f
            bullet.y < -bullet.size
        }

        activeEnemyBullets.removeAll { bullet ->
            bullet.x += (kotlin.math.cos(Math.toRadians(bullet.angle.toDouble())) * bullet.speed * deltaTime * 60f).toFloat()
            bullet.y += (kotlin.math.sin(Math.toRadians(bullet.angle.toDouble())) * bullet.speed * deltaTime * 60f).toFloat()
            bullet.y > screenH + bullet.size || bullet.x < -bullet.size || bullet.x > screenW + bullet.size
        }

        var bossJustDefeated = false

        activeEnemies.removeAll { enemy ->
            if (enemy.hp <= 0) {
                // 🔥 Spawn Explosion tại vị trí enemy chết
                spawnExplosion(
                    enemy.x + enemy.size / 2f,
                    enemy.y + enemy.size / 2f,
                    enemy.size
                )
                if (enemy.isBoss) {
                    Log.d("EntityManager", "Boss HP <= 0 detected (inside removeAll)")
                    bossJustDefeated = true
                    enemy.active = false
                    true
                } else {
                    increaseEnemiesKilled()
                    enemy.active = false
                    true
                }
            } else {
                false
            }
        }

// Sau removeAll: chỉ set WIN khi đang RUNNING (tránh ghi đè)
        if (bossJustDefeated && gameView.gameState == GameView.GameState.RUNNING) {
            Log.d("EntityManager", "Boss defeated -> setting WIN")
            gameView.gameState = GameView.GameState.WIN
            // Không gọi gameView.pause() hay stopLoop() ở đây — render phải tiếp tục để vẽ overlay
            // Ngoài ra có thể tắt nhạc hoặc phát hiệu ứng chiến thắng:
            // MusicManager.pause()
        }




        // ✅ Spawn FallingObject ngẫu nhiên
        if ((0..1000).random() < 3) {
            spawnFallingObject()
            SoundManager.playFallingHit()
        }

        // ✅ Update FallingObject với việc xoá khỏi pool khi inactive
        activeFallingObjects.removeAll { obj ->
            if (obj.active) {
                obj.y += obj.speed * deltaTime * 60f
                if (obj.y > screenH) {
                    obj.active = false
                    true
                } else false
            } else {
                true // Xóa objects không active
            }
        }
        // ✅ Update Explosions
        activeExplosions.removeAll { exp ->
            if (exp.active) {
                exp.update(deltaTime)
                false // vẫn còn active, giữ lại
            } else {
                true  // inactive thì remove
            }
        }
    }

    private fun spawnEnemy() {
        val screenW = gameView.screenW
        getEnemyFromPool()?.apply {
            x = (50..screenW - size - 50).random().toFloat()
            y = (50..300).random().toFloat()
            hp = DifficultyManager.EnemyDefaults.hp
            maxHp = hp
            speed = DifficultyManager.EnemyDefaults.speed.toFloat()
            shotDelay = DifficultyManager.EnemyDefaults.shotDelay // ✅ FIX: Thêm dòng này
            directionX = if ((0..1).random() == 0) -1 else 1
            activeEnemies.add(this)
        }
    }

    private fun spawnBoss() {
        if (!bossSpawned) {
            getBossFromPool()?.apply {
                x = gameView.screenW / 2f - size / 2
                y = 100f
                hp = DifficultyManager.BossDefaults.hp
                maxHp = hp
                speed = DifficultyManager.BossDefaults.speed.toFloat()
                shotDelay = DifficultyManager.BossDefaults.shotDelay // ✅ FIX: Thêm dòng này
                active = true
                activeEnemies.add(this)
            }
            bossSpawned = true
        }
    }
    fun spawnExplosion(x: Float, y: Float, size: Int) {
        SoundManager.playExplosion()
        getExplosionFromPool()?.let { exp ->
            exp.x = x
            exp.y = y
            exp.setFrames(explosionFrames!!, size.toFloat()) // ✅ Truyền frames gốc + size
            activeExplosions.add(exp)
        }
    }




    fun handleTouch(event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                val rect = RectF(
                    gameView.player.x, gameView.player.y,
                    gameView.player.x + gameView.player.size,
                    gameView.player.y + gameView.player.size
                )
                if (rect.contains(event.x, event.y)) {
                    isDraggingPlayer = true
                    //SoundManager.playPlayerThruster() // 🚀 bắt đầu engine
                }
            }
            MotionEvent.ACTION_MOVE -> {
                if (isDraggingPlayer) {
                    var newX = event.x - gameView.player.size / 2
                    var newY = event.y - gameView.player.size / 2

                    if (newX < 0) {
                        newX = 0f
                        SoundManager.playEnemyBounce()
                    }
                    if (newY < 0) {
                        newY = 0f
                        SoundManager.playEnemyBounce()
                    }
                    if (newX > gameView.screenW - gameView.player.size) {
                        newX = (gameView.screenW - gameView.player.size).toFloat()
                        SoundManager.playEnemyBounce()
                    }

                    if (newY > gameView.screenH - gameView.player.size) {
                        newY = (gameView.screenH - gameView.player.size).toFloat()
                        SoundManager.playEnemyBounce()
                    }
                    gameView.player.x = newX
                    gameView.player.y = newY
                }

            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                isDraggingPlayer = false
                //SoundManager.stopPlayerThruster() // 🚀 tắt engine
            }
        }
        return true
    }

    fun getBackground(): Bitmap? = scaledBackground
    fun getBgY1() = bgY1
    fun getBgY2() = bgY2

    fun increaseEnemiesKilled() {
        enemiesKilled++
    }

    private fun calculateInSampleSize(options: Options, reqWidth: Int, reqHeight: Int): Int {
        val (height: Int, width: Int) = options.outHeight to options.outWidth
        var inSampleSize = 1
        if (height > reqHeight || width > reqWidth) {
            var halfHeight = height / 2
            var halfWidth = width / 2
            while ((halfHeight / inSampleSize) >= reqHeight && (halfWidth / inSampleSize) >= reqWidth) {
                inSampleSize *= 2
            }
        }
        return inSampleSize
    }

    fun reset(screenW: Int, screenH: Int) {
        activeBullets.clear()
        activeEnemyBullets.clear()
        activeEnemies.clear()
        // ✅ Reset FallingObject
        activeFallingObjects.clear()

        activeExplosions.clear()
        explosionPool.forEach { it.active = false }

        bulletPool.forEach { it.active = false }
        enemyBulletPool.forEach { it.active = false }
        enemyPool.forEach { it.active = false }
        bossPool.forEach { it.active = false }
        fallingObjectPool.forEach { it.active = false }

        bossSpawned = false
        enemiesKilled = 0
        lastEnemySpawn = 0

        bgY1 = 0f
        bgY2 = -screenH.toFloat()

        gameView.player.reset(screenW, screenH)
    }
}