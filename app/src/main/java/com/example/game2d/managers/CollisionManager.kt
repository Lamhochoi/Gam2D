package com.example.game2d.managers

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

        // Bullet → Enemy
        entityManager.activeBullets.removeAll { b ->
            var hit = false
            bulletRect.set(b.x, b.y, b.x + b.size, b.y + b.size)
            val deadEnemies = mutableListOf<Enemy>()

            entityManager.activeEnemies.forEach { e ->
                enemyRect.set(e.x, e.y, e.x + e.size, e.y + e.size)
                if (RectF.intersects(enemyRect, bulletRect)) {
                    e.hp--
                    if (e.hp <= 0) {
                        deadEnemies.add(e)
                        if (e.isBoss) {
                            // Boss chết → để EntityManager/GameView xử lý WIN
                            e.active = false
                            if (gameView.gameState == GameView.GameState.RUNNING) {
                                gameView.gameState = GameView.GameState.WIN
                            }
                        } else {
                            // Enemy thường chết → tăng kill ngay lập tức
                            entityManager.increaseEnemiesKilled()
                            e.active = false
                            // GỌI NỔ
                            entityManager.spawnExplosion(e.x + e.size/2f, e.y + e.size/2f, e.size)
                        }
                    }
                    hit = true
                }
            }

            entityManager.activeEnemies.removeAll(deadEnemies)
            if (hit) b.active = false
            hit
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
                gameView.onPlayerHit(1) // trừ 1 máu, có hiệu ứng flash & rung
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
                    gameView.onPlayerHit(1) // chỉ trừ 1 máu
                }
            }
        }
    }
}
