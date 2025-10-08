package com.example.game2d.managers

import android.content.Context
import android.graphics.RectF
import com.example.game2d.core.GameView
import com.example.game2d.entities.Enemy
import com.example.game2d.entities.FallingObject
import com.example.game2d.entities.PowerUp
import com.example.game2d.entities.PowerUpType

open class CollisionManager(private val gameView: GameView) {

    // Reusable rects to avoid per-check allocations
    private val bulletRect = RectF()
    private val enemyRect = RectF()
    private val playerRect = RectF()
    private val fallingRect = RectF()

    private val powerUpRect = RectF()

    open fun checkCollisions() {
        val entityManager = gameView.entityManager

        // Coin → Player
        val coinIterator = entityManager.activeCoins.iterator()
        while (coinIterator.hasNext()) {
            val coin = coinIterator.next()
            if (coin.active) {
                playerRect.set(
                    gameView.player.x, gameView.player.y,
                    gameView.player.x + gameView.player.size,
                    gameView.player.y + gameView.player.size
                )
                val coinRect = RectF(coin.x, coin.y, coin.x + coin.size, coin.y + coin.size)

                if (RectF.intersects(playerRect, coinRect)) {
                    coinIterator.remove() // ✅ remove ngay bằng iterator
                    gameView.player.coins += coin.value
                    gameView.tvCoin?.post {
                        gameView.tvCoin?.text = gameView.player.coins.toString()
                    }
                }
            }
        }

        // Bullet → Enemy
        val bulletIterator = entityManager.activeBullets.iterator()
        while (bulletIterator.hasNext()) {
            val b = bulletIterator.next()
            bulletRect.set(b.x, b.y, b.x + b.size, b.y + b.size)
            var hit = false

            val enemyIterator = entityManager.activeEnemies.iterator()
            while (enemyIterator.hasNext()) {
                val e = enemyIterator.next()
                enemyRect.set(e.x, e.y, e.x + e.size, e.y + e.size)

                if (RectF.intersects(enemyRect, bulletRect)) {
                    e.hp--
                    if (e.hp <= 0 && e.active) {
                        e.active = false
                        if (e.isBoss) {
                            entityManager.spawnExplosion(e.x + e.size / 2f, e.y + e.size / 2f, e.size)
                            if (gameView.gameState == GameView.GameState.RUNNING) {
                                gameView.gameState = GameView.GameState.WIN
                            }
                        } else {
                            entityManager.increaseEnemiesKilled()
                            entityManager.spawnExplosion(e.x + e.size / 2f, e.y + e.size / 2f, e.size)
                            entityManager.spawnCoin(
                                e.x + e.size / 2f - entityManager.coinSize / 2f,
                                e.y + e.size / 2f - entityManager.coinSize / 2f
                            )
                        }
                        enemyIterator.remove() // ✅ remove an toàn
                    }
                    hit = true
                }
            }
            if (hit) {
                bulletIterator.remove() // ✅ remove bullet
            }
        }

        // Enemy bullet → Player
        val enemyBulletIterator = entityManager.activeEnemyBullets.iterator()
        while (enemyBulletIterator.hasNext()) {
            val b = enemyBulletIterator.next()
            playerRect.set(
                gameView.player.x, gameView.player.y,
                gameView.player.x + gameView.player.size,
                gameView.player.y + gameView.player.size
            )
            bulletRect.set(b.x, b.y, b.x + b.size, b.y + b.size)

            if (RectF.intersects(playerRect, bulletRect)) {
                if (!gameView.player.isInvincible) {
                    gameView.onPlayerHit(1)
                }
                enemyBulletIterator.remove()
            }
        }

        // FallingObject → Player
        val fallIterator = entityManager.activeFallingObjects.iterator()
        while (fallIterator.hasNext()) {
            val f = fallIterator.next()
            playerRect.set(
                gameView.player.x, gameView.player.y,
                gameView.player.x + gameView.player.size,
                gameView.player.y + gameView.player.size
            )
            fallingRect.set(f.x, f.y, f.x + f.size, f.y + f.size)

            if (RectF.intersects(playerRect, fallingRect)) {
                if (!gameView.player.isInvincible) {
                    gameView.onPlayerHit(1)
                }
                fallIterator.remove()
            }
        }

        // PowerUp → Player
        val powerUpIterator = entityManager.activePowerUps.iterator()
        while (powerUpIterator.hasNext()) {
            val pu = powerUpIterator.next()
            playerRect.set(
                gameView.player.x, gameView.player.y,
                gameView.player.x + gameView.player.size,
                gameView.player.y + gameView.player.size
            )
            powerUpRect.set(pu.x, pu.y, pu.x + pu.size, pu.y + pu.size)

            if (RectF.intersects(playerRect, powerUpRect)) {
                powerUpIterator.remove()
                applyPowerUpEffect(pu.type)
            }
        }
    }


    private fun applyPowerUpEffect(type: PowerUpType) {
        val player = gameView.player
        when (type) {
            PowerUpType.HEAL -> {
                player.hp = minOf(player.hp + 2, player.maxHp)  // Hồi 2 HP, không vượt max
            }
            PowerUpType.SHIELD -> {
                player.shield = 3  // Lá chắn chịu 3 damage
            }
            PowerUpType.DOUBLE_SHOT -> {
                player.doubleShotActive = true
                player.doubleShotEndTime = System.currentTimeMillis() + 10000  // 10 giây
            }
            PowerUpType.INVINCIBILITY -> {
                player.isInvincible = true
                player.invincibilityEndTime = System.currentTimeMillis() + 2000
            }
        }
    }
}