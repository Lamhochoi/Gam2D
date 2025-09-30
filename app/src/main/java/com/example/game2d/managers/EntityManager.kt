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
import android.graphics.Matrix
import android.util.Log

// Lớp quản lý tất cả thực thể trong game (player, enemy, bullet, coin, power-up, v.v.)
class EntityManager(private val gameView: GameView) {
    // Mục tiêu số enemy cần tiêu diệt để spawn boss
    var goal: Int = 0
    // Tọa độ y của hai background để tạo hiệu ứng cuộn
    private var bgY1 = 0f
    private var bgY2 = 0f
    // Tốc độ cuộn background
    private var backgroundSpeed = 5f

    // Pool và danh sách active cho đạn của player
    private val bulletPool = mutableListOf<Bullet>()
    val activeBullets = mutableListOf<Bullet>()
    // Pool và danh sách active cho đạn của enemy
    private val enemyBulletPool = mutableListOf<Bullet>()
    val activeEnemyBullets = mutableListOf<Bullet>()
    // Pool và danh sách active cho enemy
    private val enemyPool = mutableListOf<Enemy>()
    val activeEnemies = mutableListOf<Enemy>()
    // Pool cho boss
    private val bossPool = mutableListOf<Enemy>()

    // Pool và danh sách active cho coin
    val coinPool = mutableListOf<Coin>()
    val activeCoins = mutableListOf<Coin>()
    var coinBitmap: Bitmap? = null
    var cachedCoinBitmap: Bitmap? = null
    var coinSize: Int = 0

    // Pool và danh sách active cho hiệu ứng nổ
    private val explosionPool = mutableListOf<Explosion>()
    val activeExplosions = mutableListOf<Explosion>()
    private var explosionFrames: List<Bitmap>? = null

    // Pool và danh sách active cho vật phẩm rơi (rock)
    private val fallingObjectPool = mutableListOf<FallingObject>()
    val activeFallingObjects = mutableListOf<FallingObject>()

    // Pool và danh sách active cho power-up
    private val powerUpPool = mutableListOf<PowerUp>()
    val activePowerUps = mutableListOf<PowerUp>()
    private var healBitmap: Bitmap? = null
    private var shieldBitmap: Bitmap? = null
    private var doubleShotBitmap: Bitmap? = null
    private var invincibilityBitmap: Bitmap? = null
    private var cachedHealBitmap: Bitmap? = null
    private var cachedShieldBitmap: Bitmap? = null
    private var cachedDoubleShotBitmap: Bitmap? = null
    private var cachedInvincibilityBitmap: Bitmap? = null
    private var powerUpSize: Int = 0

    // Trạng thái đã spawn boss chưa
    private var bossSpawned = false
    // Số enemy đã tiêu diệt
    var enemiesKilled = 0
        private set
    // Thời gian spawn enemy lần cuối
    private var lastEnemySpawn: Long = 0

    // Bitmap gốc và cache cho các thực thể
    private var playerBitmap: Bitmap? = null
    private var enemyBitmap: Bitmap? = null
    private var bossBitmap: Bitmap? = null
    private var bulletBitmap: Bitmap? = null
    private var backgroundBitmap: Bitmap? = null
    private var scaledBackground: Bitmap? = null

    private var cachedPlayerBitmap: Bitmap? = null
    private var cachedEnemyBitmap: Bitmap? = null
    private var cachedBossBitmap: Bitmap? = null
    private var cachedBulletBitmapPlayer: Bitmap? = null
    private var cachedBulletBitmapEnemy: Bitmap? = null

    private var fallingBitmap: Bitmap? = null
    private var cachedFallingBitmap: Bitmap? = null
    private var fallingObjectSize: Int = 0

    private var playerBulletSize: Int = 0
    private var enemyBulletSize: Int = 0

    // Trạng thái khởi tạo tài nguyên
    private var resourcesInitialized = false
    // Trạng thái kéo thả player
    private var isDraggingPlayer = false

    // Thiết lập background, scale theo kích thước màn hình
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

    // Khởi tạo tài nguyên (bitmap, pool) cho game
    fun initResources(screenW: Int, screenH: Int) {
        val res = gameView.resources
        // Load và cắt sprite sheet cho hiệu ứng nổ
        val explosionSheet = BitmapFactory.decodeResource(res, R.drawable.explosion)
        val frameCount = 6
        val frameW = explosionSheet.width / frameCount
        val frameH = explosionSheet.height

        explosionFrames = (0 until frameCount).map { i ->
            Bitmap.createBitmap(explosionSheet, i * frameW, 0, frameW, frameH)
        }

        initExplosionPool(20)

        // Load bitmap cho power-up
        healBitmap = BitmapFactory.decodeResource(res, R.drawable.health_pack)
        shieldBitmap = BitmapFactory.decodeResource(res, R.drawable.shield_pack)
        doubleShotBitmap = BitmapFactory.decodeResource(res, R.drawable.double_shot)
        invincibilityBitmap = BitmapFactory.decodeResource(res, R.drawable.invincibility_pack)
        powerUpSize = (screenW * 0.06f * GameView.SCALE_FACTOR).toInt()
        cachedHealBitmap = Bitmap.createScaledBitmap(healBitmap!!, powerUpSize, powerUpSize, false)
        cachedShieldBitmap = Bitmap.createScaledBitmap(shieldBitmap!!, powerUpSize, powerUpSize, false)
        cachedDoubleShotBitmap = Bitmap.createScaledBitmap(doubleShotBitmap!!, powerUpSize, powerUpSize, false)
        cachedInvincibilityBitmap = Bitmap.createScaledBitmap(invincibilityBitmap!!, powerUpSize, powerUpSize, false)

        initPowerUpPool(10)

        // Load và scale bitmap cho coin
        coinBitmap = BitmapFactory.decodeResource(res, R.drawable.coin)
        coinSize = (screenW * 0.06f * GameView.SCALE_FACTOR).toInt()
        cachedCoinBitmap = Bitmap.createScaledBitmap(coinBitmap!!, coinSize, coinSize, false)

        // Khởi tạo pool cho coin
        if (coinPool.size < 20) {
            repeat(20 - coinPool.size) {
                coinPool.add(Coin(0f, 0f, coinSize, speed = 10f, bitmap = cachedCoinBitmap, active = false))
            }
        } else {
            coinPool.forEach { it.active = false }
        }

        // Load bitmap cho player, enemy, boss, bullet, falling object
        playerBitmap = BitmapFactory.decodeResource(res, R.drawable.player_mars)
        enemyBitmap = BitmapFactory.decodeResource(res, R.drawable.enemyred)
        bossBitmap = BitmapFactory.decodeResource(res, R.drawable.boss_end)
        bulletBitmap = BitmapFactory.decodeResource(res, R.drawable.bullet5)
        fallingBitmap = BitmapFactory.decodeResource(res, R.drawable.rock)

        // Thiết lập kích thước và vị trí ban đầu cho player
        val playerSize = (screenW * 0.14f * GameView.SCALE_FACTOR).toInt()
        gameView.player.size = playerSize
        cachedPlayerBitmap = Bitmap.createScaledBitmap(playerBitmap!!, playerSize, playerSize, false)
        gameView.player.bitmap = cachedPlayerBitmap
        gameView.player.x = screenW / 2f - playerSize / 2
        gameView.player.y = screenH * 0.7f

        initEnemyPool(20)

        // Thiết lập bitmap cho đạn player và enemy
        playerBulletSize = (screenW * 0.05f * GameView.SCALE_FACTOR).toInt()
        cachedBulletBitmapPlayer = Bitmap.createScaledBitmap(bulletBitmap!!, playerBulletSize, playerBulletSize, false).let {
            val matrix = Matrix().apply { postRotate(0f) } // Đạn player hướng lên (giả sử bullet5.png đã đúng hướng)
            Bitmap.createBitmap(it, 0, 0, it.width, it.height, matrix, true)
        }
        enemyBulletSize = (screenW * 0.04f * GameView.SCALE_FACTOR).toInt()
        cachedBulletBitmapEnemy = Bitmap.createScaledBitmap(bulletBitmap!!, enemyBulletSize, enemyBulletSize, false).let {
            val matrix = Matrix().apply { postRotate(180f) } // Đạn enemy hướng xuống (giả sử bullet5.png đã đúng hướng)
            Bitmap.createBitmap(it, 0, 0, it.width, it.height, matrix, true)
        }

        initBulletPool(50)
        initFallingObjectPool(15)

        // Thiết lập vị trí ban đầu của background
        bgY1 = 0f
        bgY2 = -screenH.toFloat()
        bossSpawned = false
        enemiesKilled = 0
        resourcesInitialized = true
    }

    // Khởi tạo pool cho power-up
    private fun initPowerUpPool(size: Int) {
        if (powerUpPool.size < size) {
            repeat(size - powerUpPool.size) {
                powerUpPool.add(PowerUp(active = false))
            }
        } else {
            powerUpPool.forEach { it.active = false }
        }
    }

    // Khởi tạo pool cho hiệu ứng nổ
    private fun initExplosionPool(size: Int) {
        if (explosionPool.size < size) {
            repeat(size - explosionPool.size) {
                explosionPool.add(Explosion(0f, 0f, active = false))
            }
        } else {
            explosionPool.forEach { it.active = false }
        }
    }

    // Khởi tạo pool cho đạn (player và enemy)
    private fun initBulletPool(size: Int) {
        if (bulletPool.size < size) {
            repeat(size - bulletPool.size) {
                bulletPool.add(Bullet(0f, 0f, size = 0f, speed = 0f, angle = 0f, bitmap = null, active = false))
            }
        } else {
            bulletPool.forEach {
                it.active = false
                it.speed = 0f
                it.angle = 0f // Reset angle để tránh lỗi tái sử dụng
            }
        }
        if (enemyBulletPool.size < size) {
            repeat(size - enemyBulletPool.size) {
                enemyBulletPool.add(Bullet(0f, 0f, size = 0f, speed = 0f, angle = 0f, bitmap = null, active = false))
            }
        } else {
            enemyBulletPool.forEach {
                it.active = false
                it.speed = 0f
                it.angle = 0f // Reset angle cho đạn enemy
            }
        }
    }

    // Khởi tạo pool cho enemy và boss
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

    // Khởi tạo pool cho falling object
    private fun initFallingObjectPool(size: Int) {
        val screenW = gameView.screenW
        fallingObjectSize = (screenW * 0.08f * GameView.SCALE_FACTOR).toInt()
        cachedFallingBitmap = Bitmap.createScaledBitmap(fallingBitmap!!, fallingObjectSize, fallingObjectSize, false)

        if (fallingObjectPool.size < size) {
            repeat(size - fallingObjectPool.size) {
                fallingObjectPool.add(
                    FallingObject(0f, 0f, fallingObjectSize, speed = 12f, bitmap = cachedFallingBitmap, active = false) // Tốc độ rơi của vật phẩm
                )
            }
        } else {
            fallingObjectPool.forEach { obj ->
                obj.size = fallingObjectSize
                obj.bitmap = cachedFallingBitmap
                obj.active = false
                obj.speed = 12f // Reset tốc độ rơi
            }
        }
    }

    // Lấy đạn từ pool
    private fun getBulletFromPool(): Bullet? = bulletPool.find { !it.active }?.also { it.active = true }
    private fun getEnemyBulletFromPool(): Bullet? = enemyBulletPool.find { !it.active }?.also { it.active = true }
    private fun getEnemyFromPool(): Enemy? = enemyPool.find { !it.active }?.also { it.active = true }
    private fun getBossFromPool(): Enemy? = bossPool.find { !it.active }?.also { it.active = true }
    private fun getExplosionFromPool(): Explosion? = explosionPool.find { !it.active }?.also { it.active = true }

    // Spawn power-up ngẫu nhiên
    fun spawnPowerUp() {
        val types = PowerUpType.values()
        val randomType = types.random()
        powerUpPool.find { !it.active }?.apply {
            x = (0..gameView.screenW - size).random().toFloat()
            y = -size.toFloat() // Spawn ngoài đỉnh màn hình
            this.size = powerUpSize
            speed = (6..8).random().toFloat() // Tốc độ rơi của power-up
            type = randomType
            bitmap = when (randomType) {
                PowerUpType.HEAL -> cachedHealBitmap
                PowerUpType.SHIELD -> cachedShieldBitmap
                PowerUpType.DOUBLE_SHOT -> cachedDoubleShotBitmap
                PowerUpType.INVINCIBILITY -> cachedInvincibilityBitmap
            }
            active = true
            activePowerUps.add(this)
        }
    }

    // Spawn đạn player
    fun spawnBullet(x: Float, y: Float) {
        if (bulletPool.none { !it.active }) {
            bulletPool.add(Bullet(0f, 0f, size = 0f, speed = 0f, angle = 0f, bitmap = null, active = false))
            Log.d("EntityManager", "Expanded bulletPool due to depletion")
        }
        bulletPool.find { !it.active }?.apply {
            this.x = x
            this.y = y
            this.size = playerBulletSize.toFloat()
            this.speed = 10f // Tốc độ đạn player
            this.angle = 0f
            this.bitmap = cachedBulletBitmapPlayer
            this.active = true
            if (!activeBullets.contains(this)) {
                activeBullets.add(this)
            }
            SoundManager.playShoot()
            Log.d("EntityManager", "Spawned player bullet at x=$x, y=$y, speed=$speed")
        }
    }

    // Spawn coin
    fun spawnCoin(x: Float, y: Float) {
        coinPool.find { !it.active }?.apply {
            this.x = x
            this.y = y
            this.size = coinSize
            speed = (10..14).random().toFloat() // Tốc độ rơi của coin
            bitmap = cachedCoinBitmap
            active = true
            value = 1
            if (!activeCoins.contains(this)) {
                activeCoins.add(this)
            }
        }
    }

    // Spawn hiệu ứng nổ
    fun spawnExplosion(x: Float, y: Float, size: Int) {
        SoundManager.playExplosion()
        getExplosionFromPool()?.let { exp ->
            exp.x = x
            exp.y = y
            exp.setFrames(explosionFrames!!, size.toFloat())
            activeExplosions.add(exp)
        }
    }

    // Spawn enemy ngẫu nhiên
    private fun spawnEnemy() {
        val screenW = gameView.screenW
        getEnemyFromPool()?.apply {
            x = (50..screenW - size - 50).random().toFloat()
            y = (50..300).random().toFloat()
            hp = DifficultyManager.EnemyDefaults.hp
            maxHp = hp
            speed = DifficultyManager.EnemyDefaults.speed.toFloat()
            shotDelay = DifficultyManager.EnemyDefaults.shotDelay
            directionX = if ((0..1).random() == 0) -1 else 1
            activeEnemies.add(this)
        }
    }

    // Spawn boss khi đủ số enemy bị tiêu diệt
    private fun spawnBoss() {
        if (!bossSpawned) {
            getBossFromPool()?.apply {
                x = gameView.screenW / 2f - size / 2
                y = 100f
                hp = DifficultyManager.BossDefaults.hp
                maxHp = hp
                speed = DifficultyManager.BossDefaults.speed.toFloat()
                shotDelay = DifficultyManager.BossDefaults.shotDelay
                active = true
                activeEnemies.add(this)
            }
            bossSpawned = true
        }
    }

    // Cập nhật trạng thái game mỗi frame
    fun update(deltaTime: Float) {
        if (!resourcesInitialized) return
        if (gameView.gameState == GameView.GameState.WIN ||
            gameView.gameState == GameView.GameState.GAME_OVER) {
            return
        }

        val screenH = gameView.screenH
        val screenW = gameView.screenW

        // Cập nhật vị trí background để cuộn
        bgY1 += backgroundSpeed * deltaTime * 60f
        bgY2 += backgroundSpeed * deltaTime * 60f
        if (bgY1 >= screenH) bgY1 = bgY2 - screenH
        if (bgY2 >= screenH) bgY2 = bgY1 - screenH

        // Kiểm tra game over khi player hết HP
        if (gameView.gameState == GameView.GameState.RUNNING && gameView.player.hp <= 0) {
            Log.d("EntityManager", "Player HP <= 0 -> GAME_OVER")
            gameView.gameState = GameView.GameState.GAME_OVER
        }

        val now = System.currentTimeMillis()

        // Xử lý bắn đạn của player
        val player = gameView.player
        if (System.currentTimeMillis() - player.lastShot > player.shotDelay) {
            player.lastShot = System.currentTimeMillis()
            val bulletX = player.x + player.size / 2f - playerBulletSize / 2f
            val bulletY = player.y - playerBulletSize.toFloat()
            val offset = player.size * 0.3f
            Log.d("EntityManager", "Player position: x=${player.x}, y=${player.y}, bulletX=$bulletX, bulletY=$bulletY")

            if (player.doubleShotActive && System.currentTimeMillis() < player.doubleShotEndTime) {
                spawnBullet(bulletX - offset, bulletY)
                spawnBullet(bulletX + offset, bulletY)
                Log.d("EntityManager", "Double shot active, spawning two bullets")
            } else {
                spawnBullet(bulletX, bulletY)
            }

            if (player.doubleShotActive && System.currentTimeMillis() >= player.doubleShotEndTime) {
                player.doubleShotActive = false
                Log.d("EntityManager", "Double shot expired")
            }
        }

        // Spawn enemy định kỳ
        if (now - lastEnemySpawn > 1500) {
            spawnEnemy()
            lastEnemySpawn = now
        }

        // Cập nhật enemy
        val iteratorEnemy = activeEnemies.iterator()
        while (iteratorEnemy.hasNext()) {
            val e = iteratorEnemy.next()
            if (e.active) {
                e.y += e.speed * deltaTime * 60f
                e.x += 2f * deltaTime * 60f * e.directionX
                if (e.x < 0) {
                    e.x = 0f
                    e.directionX *= -1
                    SoundManager.playEnemyBounce()
                } else if (e.x + e.size > screenW) {
                    e.x = (screenW - e.size).toFloat()
                    e.directionX *= -1
                    SoundManager.playEnemyBounce()
                }
                val maxY = (screenH * 0.66f).toInt()
                if (e.y < 0) {
                    e.y = 0f
                    e.speed = -e.speed
                } else if (e.y + e.size > maxY) {
                    e.y = (maxY - e.size).toFloat()
                    e.speed = -e.speed
                }

                // Enemy bắn đạn
                if (now - e.lastShot > e.shotDelay) {
                    getEnemyBulletFromPool()?.let { bullet ->
                        bullet.x = e.x + e.size / 2 - enemyBulletSize / 2
                        bullet.y = e.y + e.size
                        bullet.size = enemyBulletSize.toFloat()
                        bullet.speed = 15f // Tốc độ đạn enemy
                        bullet.angle = 0f // Không dùng angle, nhưng set để nhất quán
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

        // Spawn boss khi đủ số enemy bị tiêu diệt
        if (!bossSpawned && enemiesKilled >= goal) spawnBoss()

        // Cập nhật đạn player (di chuyển lên trên)
        activeBullets.removeAll { bullet ->
            bullet.y -= bullet.speed * deltaTime * 60f
            bullet.y < -bullet.size
        }

        // Cập nhật đạn enemy (di chuyển xuống dưới)
        activeEnemyBullets.removeAll { bullet ->
            bullet.y += bullet.speed * deltaTime * 60f
            bullet.y > screenH + bullet.size
        }

        // Cập nhật coin (rơi từ trên xuống)
        activeCoins.forEach { coin ->
            if (coin.active) {
                coin.y += coin.speed * deltaTime * 60f
                if (coin.y > gameView.screenH) {
                    coin.active = false
                }
            }
        }
        activeCoins.removeAll { !it.active }

        // Spawn falling object ngẫu nhiên
        if ((0..1000).random() < 5) { // Xác suất ~0.5%, ~1 falling object mỗi 3.34 giây
            spawnFallingObject()
            SoundManager.playFallingHit()
        }

        // Cập nhật falling object (rơi từ trên xuống)
        activeFallingObjects.removeAll { obj ->
            if (obj.active) {
                obj.y += obj.speed * deltaTime * 60f
                if (obj.y > screenH) {
                    obj.active = false
                    true
                } else false
            } else {
                true
            }
        }

        // Spawn power-up ngẫu nhiên
        if ((0..200).random() < 1) { // Xác suất ~0.5%, ~1 power-up mỗi 3.36 giây
            spawnPowerUp()
        }

        // Cập nhật power-up (rơi từ trên xuống)
        activePowerUps.removeAll { pu ->
            if (pu.active) {
                pu.y += pu.speed * deltaTime * 60f
                if (pu.y > gameView.screenH) {
                    pu.active = false
                    true
                } else false
            } else true
        }

        // Cập nhật hiệu ứng nổ
        activeExplosions.removeAll { exp ->
            if (exp.active) {
                exp.update(deltaTime)
                false
            } else {
                true
            }
        }
    }

    // Spawn falling object
    private fun spawnFallingObject() {
        val screenW = gameView.screenW
        fallingObjectPool.find { !it.active }?.apply {
            x = (0..(screenW - size)).random().toFloat()
            y = -size.toFloat() // Spawn ngoài đỉnh màn hình
            speed = (10..16).random().toFloat() // Tốc độ rơi ngẫu nhiên
            bitmap = cachedFallingBitmap
            active = true
            activeFallingObjects.add(this)
        }
    }

    // Xử lý thao tác chạm để di chuyển player
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
            }
        }
        return true
    }

    // Lấy background và tọa độ
    fun getBackground(): Bitmap? = scaledBackground
    fun getBgY1() = bgY1
    fun getBgY2() = bgY2

    // Tăng số enemy bị tiêu diệt
    fun increaseEnemiesKilled() {
        enemiesKilled++
    }

    // Tính toán sample size để scale bitmap
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

    // Reset trạng thái game
    fun reset(screenW: Int, screenH: Int) {
        activeBullets.clear()
        activeEnemyBullets.clear()
        activeEnemies.clear()
        activeFallingObjects.clear()
        activePowerUps.clear()
        powerUpPool.forEach { it.active = false }
        activeExplosions.clear()
        explosionPool.forEach { it.active = false }
        bulletPool.forEach { it.active = false; it.speed = 0f; it.angle = 0f }
        enemyBulletPool.forEach { it.active = false; it.speed = 0f; it.angle = 0f }
        enemyPool.forEach { it.active = false }
        bossPool.forEach { it.active = false }
        fallingObjectPool.forEach { it.active = false; it.speed = 12f }
        bossSpawned = false
        enemiesKilled = 0
        lastEnemySpawn = 0
        bgY1 = 0f
        bgY2 = -screenH.toFloat()
        gameView.player.reset(screenW, screenH)
    }
}