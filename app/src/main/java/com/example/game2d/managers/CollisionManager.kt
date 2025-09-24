package com.example.game2d.managers

import android.content.Context
import android.graphics.RectF
import com.example.game2d.core.GameView
import com.example.game2d.entities.Enemy
import com.example.game2d.entities.FallingObject

class CollisionManager(private val gameView: GameView) {

    // Reusable rects to avoid per-check allocations
    private val bulletRect = RectF()
    private val enemyRect = RectF()
    private val playerRect = RectF()
    private val fallingRect = RectF()

    fun checkCollisions() {
        val entityManager = gameView.entityManager

        // Coin → Player
        entityManager.activeCoins.forEach { coin ->
            if (coin.active) {
                playerRect.set(
                    gameView.player.x, gameView.player.y,
                    gameView.player.x + gameView.player.size,
                    gameView.player.y + gameView.player.size
                )
                val coinRect = RectF(coin.x, coin.y, coin.x + coin.size, coin.y + coin.size)

                if (RectF.intersects(playerRect, coinRect)) {
                    coin.active = false
                    gameView.player.coins += coin.value
                    // ✅ Chỉ cập nhật coin tạm thời và UI
                    gameView.tvCoin?.post {
                        gameView.tvCoin?.text = gameView.player.coins.toString()
                    }
                    //SoundManager.playCoin()
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
                            // Boss chết → WIN
                            entityManager.spawnExplosion(e.x + e.size / 2f, e.y + e.size / 2f, e.size)
                            if (gameView.gameState == GameView.GameState.RUNNING) {
                                gameView.gameState = GameView.GameState.WIN
                            }
                        } else {
                            // Enemy thường chết
                            entityManager.increaseEnemiesKilled()
                            entityManager.spawnExplosion(e.x + e.size / 2f, e.y + e.size / 2f, e.size)

                            // ✅ Spawn Coin tại đây
                            entityManager.spawnCoin(
                                e.x + e.size / 2f - entityManager.coinSize / 2f,
                                e.y + e.size / 2f - entityManager.coinSize / 2f
                            )
                        }
                        enemyIterator.remove() // ✅ xoá ngay enemy chết
                    }
                    hit = true
                }
            }

            if (hit) {
                b.active = false
                bulletIterator.remove() // xoá bullet đã va chạm
            }
        }

        // Enemy bullet → Player
        entityManager.activeEnemyBullets.removeAll { b ->
            playerRect.set(
                gameView.player.x, gameView.player.y,
                gameView.player.x + gameView.player.size,
                gameView.player.y + gameView.player.size
            )
            bulletRect.set(b.x, b.y, b.x + b.size, b.y + b.size)

            if (RectF.intersects(playerRect, bulletRect)) {
                gameView.onPlayerHit(1)
                b.active = false
                true
            } else false
        }

        // FallingObject → Player
        entityManager.activeFallingObjects.forEach { f: FallingObject ->
            if (f.active) {
                playerRect.set(
                    gameView.player.x, gameView.player.y,
                    gameView.player.x + gameView.player.size,
                    gameView.player.y + gameView.player.size
                )
                fallingRect.set(f.x, f.y, f.x + f.size, f.y + f.size)

                if (RectF.intersects(playerRect, fallingRect)) {
                    f.active = false
                    gameView.onPlayerHit(1)
                }
            }
        }
    }
}