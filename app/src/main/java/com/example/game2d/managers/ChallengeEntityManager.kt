package com.example.game2d.managers

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.RectF
import android.view.MotionEvent
import com.example.game2d.R
import com.example.game2d.core.ChallengeGameView
import com.example.game2d.core.GameView
import com.example.game2d.entities.*

class ChallengeEntityManager(private val challengeGameView: ChallengeGameView) : EntityManager(challengeGameView) {

    private var marsEnemyBitmap: Bitmap? = null
    private var mercuryEnemyBitmap: Bitmap? = null
    private var saturnEnemyBitmap: Bitmap? = null
    private var marsBossBitmap: Bitmap? = null
    private var mercuryBossBitmap: Bitmap? = null
    private var saturnBossBitmap: Bitmap? = null
    private var marsBulletBitmap: Bitmap? = null
    private var mercuryBulletBitmap: Bitmap? = null
    private var saturnBulletBitmap: Bitmap? = null

    override fun initResources(screenW: Int, screenH: Int) {
        super.initResources(screenW, screenH)
        val res = challengeGameView.resources
        marsEnemyBitmap = BitmapFactory.decodeResource(res, R.drawable.enemyred)
        mercuryEnemyBitmap = BitmapFactory.decodeResource(res, R.drawable.enemy10)
        saturnEnemyBitmap = BitmapFactory.decodeResource(res, R.drawable.enemy11)
        marsBossBitmap = BitmapFactory.decodeResource(res, R.drawable.boss_end)
        mercuryBossBitmap = BitmapFactory.decodeResource(res, R.drawable.boss_mer)
        saturnBossBitmap = BitmapFactory.decodeResource(res, R.drawable.boss_saturn)
        marsBulletBitmap = BitmapFactory.decodeResource(res, R.drawable.bullet5)
        mercuryBulletBitmap = BitmapFactory.decodeResource(res, R.drawable.bullet5)
        saturnBulletBitmap = BitmapFactory.decodeResource(res, R.drawable.bullet5)
        updateForLevel()
    }

    fun updateForLevel() {
        val level = challengeGameView.levels[challengeGameView.currentLevelIndex]
        when (level) {
            "MARS" -> {
                setBackground(challengeGameView.context, R.drawable.bgr_marss, challengeGameView.screenW, challengeGameView.screenH)
                enemyBitmap = marsEnemyBitmap
                bossBitmap = marsBossBitmap
                cachedEnemyBitmap = marsEnemyBitmap?.let { Bitmap.createScaledBitmap(it, enemySize, enemySize, true) }
                cachedBossBitmap = marsBossBitmap?.let { Bitmap.createScaledBitmap(it, bossSize, bossSize, true) }
                cachedBulletBitmapEnemy = marsBulletBitmap?.let { Bitmap.createScaledBitmap(it, enemyBulletSize, enemyBulletSize, true) }
            }
            "MERCURY" -> {
                setBackground(challengeGameView.context, R.drawable.bgr_mer, challengeGameView.screenW, challengeGameView.screenH)
                enemyBitmap = mercuryEnemyBitmap
                bossBitmap = mercuryBossBitmap
                cachedEnemyBitmap = mercuryEnemyBitmap?.let { Bitmap.createScaledBitmap(it, enemySize, enemySize, true) }
                cachedBossBitmap = mercuryBossBitmap?.let { Bitmap.createScaledBitmap(it, bossSize, bossSize, true) }
                cachedBulletBitmapEnemy = mercuryBulletBitmap?.let { Bitmap.createScaledBitmap(it, enemyBulletSize, enemyBulletSize, true) }
            }
            "SATURN" -> {
                setBackground(challengeGameView.context, R.drawable.bgr_saturn, challengeGameView.screenW, challengeGameView.screenH)
                enemyBitmap = saturnEnemyBitmap
                bossBitmap = saturnBossBitmap
                cachedEnemyBitmap = saturnEnemyBitmap?.let { Bitmap.createScaledBitmap(it, enemySize, enemySize, true) }
                cachedBossBitmap = saturnBossBitmap?.let { Bitmap.createScaledBitmap(it, bossSize, bossSize, true) }
                cachedBulletBitmapEnemy = saturnBulletBitmap?.let { Bitmap.createScaledBitmap(it, enemyBulletSize, enemyBulletSize, true) }
            }
        }
        enemyPool.forEach { it.bitmap = cachedEnemyBitmap }
        bossPool.forEach { it.bitmap = cachedBossBitmap }
        enemyBulletPool.forEach { it.bitmap = cachedBulletBitmapEnemy }
    }

    override fun reset(screenW: Int, screenH: Int) {
        val savedHp = challengeGameView.player.hp
        val savedShield = challengeGameView.player.shield
        val savedDoubleShot = challengeGameView.player.doubleShotActive
        val savedDoubleEnd = challengeGameView.player.doubleShotEndTime
        val savedInvincible = challengeGameView.player.isInvincible
        val savedInvEnd = challengeGameView.player.invincibilityEndTime

        super.reset(screenW, screenH)

        challengeGameView.player.hp = savedHp
        challengeGameView.player.shield = savedShield
        challengeGameView.player.doubleShotActive = savedDoubleShot
        challengeGameView.player.doubleShotEndTime = savedDoubleEnd
        challengeGameView.player.isInvincible = savedInvincible
        challengeGameView.player.invincibilityEndTime = savedInvEnd

        updateForLevel()
    }

    override fun update(deltaTime: Float) {
        super.update(deltaTime)
        if (challengeGameView.gameState == GameView.GameState.RUNNING) {
            val level = challengeGameView.levels[challengeGameView.currentLevelIndex]
            if (level == "SATURN" && (0..100).random() < 5) { // Tăng spawn để feature rõ rệt
                spawnFallingObject()
            }
        }
    }
}