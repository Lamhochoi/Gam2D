package com.example.game2d.managers

import com.example.game2d.core.GameView
import com.example.game2d.entities.Player

object DifficultyManager {
    object EnemyDefaults {
        var speed: Int = 3 // Tăng từ 2 lên 3
        var shotDelay: Long = 800 // Giảm từ 1000ms xuống 800ms
        var hp: Int = 5
    }

    object BossDefaults {
        var hp: Int = 30
        var speed: Int = 3 // Tăng từ 2 lên 3
        var shotDelay: Long = 800 // Giảm từ 1000ms xuống 800ms
    }

    fun apply(level: String, gameView: GameView) {
        apply(level, gameView.player, gameView)
    }

    fun apply(level: String, player: Player, gameView: GameView) {
        val entityManager = gameView.entityManager

        when (level) {
            "MARS" -> {
                player.hp = 50; player.maxHp = 50
                EnemyDefaults.speed = 1; EnemyDefaults.shotDelay = 1200; EnemyDefaults.hp = 2
                BossDefaults.hp = 20; BossDefaults.speed = 1; BossDefaults.shotDelay = 1200
                entityManager.goal = 10
            }
            "MERCURY" -> {
                player.hp = 20; player.maxHp = 20
                EnemyDefaults.speed = 4; EnemyDefaults.shotDelay = 700; EnemyDefaults.hp = 2
                BossDefaults.hp = 40; BossDefaults.speed = 3; BossDefaults.shotDelay = 800
                entityManager.goal = 20
            }
            "SATURN" -> {
                player.hp = 20; player.maxHp = 20
                EnemyDefaults.speed = 5; EnemyDefaults.shotDelay = 500; EnemyDefaults.hp = 2
                BossDefaults.hp = 60; BossDefaults.speed = 4; BossDefaults.shotDelay = 600
                entityManager.goal = 25
            }
        }
    }
}