package com.example.game2d

import android.content.Intent
import android.content.pm.ActivityInfo
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.widget.Button
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.game2d.managers.PlayerDataManager

class MainActivity : AppCompatActivity() {

    private lateinit var tvCoin: TextView
    private lateinit var tvEnergy: TextView
    private lateinit var tvGem: TextView
    private lateinit var btnShop: ImageButton
    private val handler = Handler(Looper.getMainLooper())
    private val energyRechargeRunnable = object : Runnable {
        override fun run() {
            PlayerDataManager.addEnergy(this@MainActivity, 1)
            loadPlayerData()
            Log.d("MainActivity", "Energy recharged, scheduling next in 2 minutes")
            handler.postDelayed(this, 120_000)
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

        tvCoin = findViewById(R.id.tvCoin)
        tvEnergy = findViewById(R.id.tvEnergy)
        tvGem = findViewById(R.id.tvGem)
        btnShop = findViewById(R.id.btnShop)

        val btnMars = findViewById<Button>(R.id.btnEasy)
        val btnMercury = findViewById<Button>(R.id.btnMedium)
        val btnSaturn = findViewById<Button>(R.id.btnHard)
        val btnChallenge = findViewById<Button>(R.id.btnChallenge)
        val btnAddEnergy = findViewById<ImageView>(R.id.btnAddEnergy)
        val btnAddCoin = findViewById<ImageView>(R.id.btnAddCoin)
        val btnAddGem = findViewById<ImageView>(R.id.btnAddGem)

        btnMars.setOnClickListener { tryStartGame("MARS") }
        btnMercury.setOnClickListener { tryStartGame("MERCURY") }
        btnSaturn.setOnClickListener { tryStartGame("SATURN") }
        btnChallenge.setOnClickListener { tryStartChallenge() }
        btnShop.setOnClickListener {
            Log.d("MainActivity", "Shop button clicked")
            showShopDialog()
        }
        btnAddEnergy.setOnClickListener { Toast.makeText(this, "Dùng nút Shop để mua Energy!", Toast.LENGTH_SHORT).show() }
        btnAddCoin.setOnClickListener { Toast.makeText(this, "Chưa hỗ trợ mua Coin!", Toast.LENGTH_SHORT).show() }
        btnAddGem.setOnClickListener { Toast.makeText(this, "Chưa hỗ trợ mua Gem!", Toast.LENGTH_SHORT).show() }

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
        handler.removeCallbacks(energyRechargeRunnable)
        Log.d("MainActivity", "onPause: stopped energy recharge timer")
    }

    private fun startEnergyRechargeTimer() {
        handler.removeCallbacks(energyRechargeRunnable)
        val currentEnergy = PlayerDataManager.getEnergy(this)
        if (currentEnergy < 30) {
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
            tvEnergy.animate().scaleX(1.2f).scaleY(1.2f).setDuration(200).withEndAction {
                tvEnergy.animate().scaleX(1f).scaleY(1f).setDuration(200).start()
            }.start()
            Toast.makeText(this, "Bắt đầu chiến đấu trên $planet!", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "Không đủ Energy!", Toast.LENGTH_SHORT).show()
        }
    }

    private fun tryStartChallenge() {
        if (PlayerDataManager.useEnergy(this, 5)) { // Challenge mode costs 5 energy
            startGame("CHALLENGE")
            tvEnergy.animate().scaleX(1.2f).scaleY(1.2f).setDuration(200).withEndAction {
                tvEnergy.animate().scaleX(1f).scaleY(1f).setDuration(200).start()
            }.start()
            Toast.makeText(this, "Bắt đầu Khiêu Chiến Vũ Trụ!", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "Cần 5 Energy để chơi Khiêu Chiến!", Toast.LENGTH_SHORT).show()
        }
    }

    private fun startGame(mode: String) {
        val intent = Intent(this, GameActivity::class.java)
        intent.putExtra("MODE", mode)
        gameLauncher.launch(intent)
    }

    private fun showShopDialog() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.shop_dialog, null)
        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .setCancelable(false)
            .create()

        val tvShopGems = dialogView.findViewById<TextView>(R.id.tvShopGems)
        val tvShopCoins = dialogView.findViewById<TextView>(R.id.tvShopCoins)
        val rvShopItems = dialogView.findViewById<RecyclerView>(R.id.rvShopItems)
        val btnCloseShop = dialogView.findViewById<Button>(R.id.btnCloseShop)

        val currentGems = PlayerDataManager.getGems(this)
        val currentCoins = PlayerDataManager.getCoins(this)
        tvShopGems.text = "Số Gems: $currentGems"
        tvShopCoins.text = "Số Coins: $currentCoins"
        Log.d("MainActivity", "Showing shop dialog, gems=$currentGems, coins=$currentCoins")

        val shopItems = listOf(
            ShopItem("10 Energy", 10, 5, 50, R.drawable.energy, "GEMS"),
            ShopItem("30 Energy", 30, 12, 120, R.drawable.energy, "GEMS"),
            ShopItem("10 Energy (Coins)", 10, 0, 50, R.drawable.energy, "COINS"),
            ShopItem("30 Energy (Coins)", 30, 0, 120, R.drawable.energy, "COINS"),
            ShopItem("50 Gems", 50, 0, 500, R.drawable.gem, "COINS")
        )

        rvShopItems.layoutManager = LinearLayoutManager(this)
        rvShopItems.addItemDecoration(DividerItemDecoration(this, LinearLayoutManager.VERTICAL))
        rvShopItems.adapter = ShopItemAdapter(shopItems) { item ->
            Log.d("MainActivity", "Attempting to buy ${item.name}")
            if (item.currencyType == "GEMS") {
                buyItemWithGems(item.amount, item.gemCost, item.name)
            } else {
                buyItemWithCoins(item.amount, item.coinCost, item.name)
            }
            tvShopGems.text = "Số Gems: ${PlayerDataManager.getGems(this)}"
            tvShopCoins.text = "Số Coins: ${PlayerDataManager.getCoins(this)}"
            loadPlayerData()
            dialog.dismiss()
        }

        btnCloseShop.setOnClickListener {
            Log.d("MainActivity", "Shop dialog closed")
            dialog.dismiss()
        }

        dialog.window?.setBackgroundDrawableResource(R.drawable.shop_dialog_bg)
        dialog.show()
    }

    private fun buyItemWithGems(amount: Int, gemCost: Int, itemName: String) {
        Log.d("MainActivity", "buyItemWithGems: item=$itemName, amount=$amount, gemCost=$gemCost")
        if (PlayerDataManager.buyEnergy(this, amount, gemCost)) {
            loadPlayerData()
            Toast.makeText(this, "Mua thành công: +$amount Energy", Toast.LENGTH_SHORT).show()
            tvEnergy.animate().scaleX(1.2f).scaleY(1.2f).setDuration(200).withEndAction {
                tvEnergy.animate().scaleX(1f).scaleY(1f).setDuration(200).start()
            }.start()
        } else {
            Toast.makeText(this, "Không đủ Gems! Cần $gemCost Gems", Toast.LENGTH_SHORT).show()
        }
    }

    private fun buyItemWithCoins(amount: Int, coinCost: Int, itemName: String) {
        Log.d("MainActivity", "buyItemWithCoins: item=$itemName, amount=$amount, coinCost=$coinCost")
        if (itemName.contains("Gems")) {
            if (PlayerDataManager.buyGemsWithCoins(this, amount, coinCost)) {
                loadPlayerData()
                Toast.makeText(this, "Mua thành công: +$amount Gems", Toast.LENGTH_SHORT).show()
                tvGem.animate().scaleX(1.2f).scaleY(1.2f).setDuration(200).withEndAction {
                    tvGem.animate().scaleX(1f).scaleY(1f).setDuration(200).start()
                }.start()
            } else {
                Toast.makeText(this, "Không đủ Coins! Cần $coinCost Coins", Toast.LENGTH_SHORT).show()
            }
        } else {
            if (PlayerDataManager.buyEnergyWithCoins(this, amount, coinCost)) {
                loadPlayerData()
                Toast.makeText(this, "Mua thành công: +$amount Energy", Toast.LENGTH_SHORT).show()
                tvEnergy.animate().scaleX(1.2f).scaleY(1.2f).setDuration(200).withEndAction {
                    tvEnergy.animate().scaleX(1f).scaleY(1f).setDuration(200).start()
                }.start()
            } else {
                Toast.makeText(this, "Không đủ Coins! Cần $coinCost Coins", Toast.LENGTH_SHORT).show()
            }
        }
    }
}