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
                challengeGameView.entityManager.activeFallingObjects.forEach { f: FallingObject ->
                    if (f.active) {
                        val playerRect = RectF(
                            challengeGameView.player.x,
                            challengeGameView.player.y,
                            challengeGameView.player.x + challengeGameView.player.size,
                            challengeGameView.player.y + challengeGameView.player.size
                        )
                        val fallingRect = RectF(
                            f.x,
                            f.y,
                            f.x + f.size,
                            f.y + f.size
                        )
                        if (RectF.intersects(playerRect, fallingRect)) {
                            challengeGameView.onPlayerHit(2)
                            f.active = false
                        }
                    }
                }
            }
        }
    }
}