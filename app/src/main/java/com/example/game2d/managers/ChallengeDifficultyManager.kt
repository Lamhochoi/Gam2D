package com.example.game2d.managers

import com.example.game2d.core.ChallengeGameView
import com.example.game2d.entities.Player
import com.example.game2d.R

object ChallengeDifficultyManager {
    object EnemyDefaults {
        var speed: Int = 3
        var shotDelay: Long = 800
        var hp: Int = 5
    }

    object BossDefaults {
        var hp: Int = 30
        var speed: Int = 3
        var shotDelay: Long = 800
    }

    fun apply(level: String, gameView: ChallengeGameView, keepPlayerState: Boolean = false) {
        apply(level, gameView.player, gameView, keepPlayerState)
    }

    fun apply(level: String, player: Player, gameView: ChallengeGameView, keepPlayerState: Boolean) {
        val entityManager = gameView.entityManager

        when (level) {
            "MARS" -> {
                if (!keepPlayerState) {
                    player.maxHp = 100
                }
                player.hp = 100
                EnemyDefaults.speed = 1; EnemyDefaults.shotDelay = 1800; EnemyDefaults.hp = 2
                BossDefaults.hp = 10; BossDefaults.speed = 1; BossDefaults.shotDelay = 1500
                entityManager.goal = 10

                entityManager.setBackground(gameView.context, R.drawable.bgr_marss, gameView.screenW, gameView.screenH)
            }
            "MERCURY" -> {
                if (!keepPlayerState) {
                    player.maxHp = 100
                }
                EnemyDefaults.speed = 2; EnemyDefaults.shotDelay = 1500; EnemyDefaults.hp = 2
                BossDefaults.hp = 15; BossDefaults.speed = 2; BossDefaults.shotDelay = 1300
                entityManager.goal = 12
                entityManager.setBackground(gameView.context, R.drawable.bgr_mer, gameView.screenW, gameView.screenH)
            }
            "SATURN" -> {
                if (!keepPlayerState) {
                    player.maxHp = 100
                }
                EnemyDefaults.speed = 3; EnemyDefaults.shotDelay = 1000; EnemyDefaults.hp = 2
                BossDefaults.hp = 20; BossDefaults.speed = 3; BossDefaults.shotDelay = 1000
                entityManager.goal = 15
                entityManager.setBackground(gameView.context, R.drawable.bgr_saturn, gameView.screenW, gameView.screenH)
            }
        }
    }
}