package com.example.game2d

import android.content.Intent
import android.content.pm.ActivityInfo
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.example.game2d.managers.PlayerDataManager

class MainActivity : AppCompatActivity() {

    private lateinit var tvCoin: TextView
    private lateinit var tvEnergy: TextView
    private lateinit var tvGem: TextView

    private val gameLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                loadPlayerData()
                Log.d("MainActivity", "Game returned, reloaded player data")
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        setContentView(R.layout.activity_main)

        // Đảm bảo không reset
        // PlayerDataManager.clearAllForDebug(this)

        tvCoin = findViewById(R.id.tvCoin)
        tvEnergy = findViewById(R.id.tvEnergy)
        tvGem = findViewById(R.id.tvGem)

        val btnMars = findViewById<Button>(R.id.btnEasy)
        val btnMercury = findViewById<Button>(R.id.btnMedium)
        val btnSaturn = findViewById<Button>(R.id.btnHard)

        btnMars.setOnClickListener { tryStartGame("MARS") }
        btnMercury.setOnClickListener { tryStartGame("MERCURY") }
        btnSaturn.setOnClickListener { tryStartGame("SATURN") }

        loadPlayerData()
        PlayerDataManager.debugPrefs(this)
    }

    override fun onResume() {
        super.onResume()
        loadPlayerData()
        Log.d("MainActivity", "onResume: reloaded player data")
        PlayerDataManager.debugPrefs(this)
    }

    private fun loadPlayerData() {
        val coins = PlayerDataManager.getCoins(this)
        val energy = PlayerDataManager.getEnergy(this)
        val gems = PlayerDataManager.getGems(this)
        Log.d("MainActivity", "loadPlayerData: coins=$coins, energy=$energy, gems=$gems")
        tvCoin.text = coins.toString()
        tvEnergy.text = "$energy/30"
        tvGem.text = gems.toString()
    }

    private fun tryStartGame(planet: String) {
        if (PlayerDataManager.useEnergy(this, 1)) {
            startGame(planet)
        } else {
            Toast.makeText(this, "Không đủ Energy!", Toast.LENGTH_SHORT).show()
        }
    }

    private fun startGame(planet: String) {
        val intent = Intent(this, GameActivity::class.java)
        intent.putExtra("LEVEL", planet)
        gameLauncher.launch(intent)
    }
}