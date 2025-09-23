package com.example.game2d.managers

import com.example.game2d.core.GameView
import com.example.game2d.entities.Player

object DifficultyManager {
    object EnemyDefaults {
        var speed: Int = 2
        var shotDelay: Long = 1000
        var hp: Int = 5
    }

    object BossDefaults {
        var hp: Int = 30
        var speed: Int = 2
        var shotDelay: Long = 1000
    }
    fun apply(level: String, gameView: GameView) {
        apply(level, gameView.player, gameView)
    }
    fun apply(level: String, player: Player, gameView: GameView) {
        val entityManager = gameView.entityManager

        when (level) {
            "MARS" -> {
                player.hp = 50; player.maxHp = 50
                EnemyDefaults.speed = 1; EnemyDefaults.shotDelay = 1500; EnemyDefaults.hp = 2
                BossDefaults.hp = 20; BossDefaults.speed = 1; BossDefaults.shotDelay = 1500
                entityManager.goal = 10
            }
            "MERCURY" -> {
                player.hp = 20; player.maxHp = 20
                EnemyDefaults.speed = 1; EnemyDefaults.shotDelay = 900; EnemyDefaults.hp = 2
                BossDefaults.hp = 40; BossDefaults.speed = 2; BossDefaults.shotDelay = 1000
                entityManager.goal = 20
            }
            "SATURN" -> {
                player.hp = 20; player.maxHp = 20
                EnemyDefaults.speed = 1; EnemyDefaults.shotDelay = 600; EnemyDefaults.hp = 2
                BossDefaults.hp = 60; BossDefaults.speed = 3; BossDefaults.shotDelay = 700
                entityManager.goal = 25
            }
        }
    }
}
