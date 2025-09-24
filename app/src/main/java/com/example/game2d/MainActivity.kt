package com.example.game2d

import android.content.Intent
import android.content.pm.ActivityInfo
import android.os.Bundle
import android.os.Handler
import android.os.Looper
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
    private val handler = Handler(Looper.getMainLooper())
    private val energyRechargeRunnable = object : Runnable {
        override fun run() {
            PlayerDataManager.addEnergy(this@MainActivity, 1)
            loadPlayerData() // Cập nhật UI
            Log.d("MainActivity", "Energy recharged, scheduling next in 2 minutes")
            handler.postDelayed(this, 120_000) // 120 giây = 2 phút
        }
    }

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
        startEnergyRechargeTimer()
    }

    override fun onResume() {
        super.onResume()
        loadPlayerData()
        Log.d("MainActivity", "onResume: reloaded player data")
        PlayerDataManager.debugPrefs(this)
        startEnergyRechargeTimer()
    }

    override fun onPause() {
        super.onPause()
        handler.removeCallbacks(energyRechargeRunnable) // Dừng timer khi pause
        Log.d("MainActivity", "onPause: stopped energy recharge timer")
    }

    private fun startEnergyRechargeTimer() {
        handler.removeCallbacks(energyRechargeRunnable) // Xóa timer cũ
        val currentEnergy = PlayerDataManager.getEnergy(this)
        if (currentEnergy < 30) { // Chỉ chạy timer nếu chưa đầy
            handler.postDelayed(energyRechargeRunnable, 120_000)
            Log.d("MainActivity", "Started energy recharge timer, currentEnergy=$currentEnergy")
        } else {
            Log.d("MainActivity", "Energy full ($currentEnergy), no timer needed")
        }
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