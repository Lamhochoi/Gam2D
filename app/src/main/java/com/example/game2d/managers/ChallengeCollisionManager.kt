package com.example.game2d.managers

import android.graphics.RectF
import com.example.game2d.core.ChallengeGameView
import com.example.game2d.core.GameView
import com.example.game2d.entities.FallingObject

class ChallengeCollisionManager(private val challengeGameView: ChallengeGameView) : CollisionManager(challengeGameView) {
    override fun checkCollisions() {
        super.checkCollisions()
        if (challengeGameView.gameState == GameView.GameState.RUNNING) {
            val level = challengeGameView.levels[challengeGameView.currentLevelIndex]
            if (level == "SATURN") {
                // Truy cập activeFallingObjects từ entityManager của ChallengeGameView
                challengeGameView.entityManager.activeFallingObjects.forEach { f: FallingObject ->
                    if (f.active) { // Kiểm tra thuộc tính active
                        // Tính toán RectF cho người chơi
                        val playerRect = RectF(
                            challengeGameView.player.x,
                            challengeGameView.player.y,
                            challengeGameView.player.x + challengeGameView.player.size,
                            challengeGameView.player.y + challengeGameView.player.size
                        )
                        // Tính toán RectF cho thiên thạch
                        val fallingRect = RectF(
                            f.x,
                            f.y,
                            f.x + f.size,
                            f.y + f.size
                        )
                        // Kiểm tra va chạm với xác suất 5%
                        if (RectF.intersects(playerRect, fallingRect) && (0..100).random() < 5) {
                            challengeGameView.onPlayerHit(2) // Gấp đôi sát thương ở Saturn
                        }
                    }
                }
            }
        }
    }
}