package com.example.game2d

import android.content.Intent
import android.content.pm.ActivityInfo
import android.os.Bundle
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

    // ✅ launcher để nhận result từ GameActivity
    private val gameLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                // Chỉ cần load lại toàn bộ dữ liệu mỗi khi quay về
                loadPlayerData()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Ép dọc màn hình
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        setContentView(R.layout.activity_main)

        // ⚠️ DEBUG: reset toàn bộ dữ liệu khi build lại app
        // 👉 BỎ DÒNG NÀY KHI RELEASE
        // PlayerDataManager.clearAllForDebug(this)

        // Ánh xạ view
        tvCoin = findViewById(R.id.tvCoin)
        tvEnergy = findViewById(R.id.tvEnergy)
        tvGem = findViewById(R.id.tvGem)

        val btnMars = findViewById<Button>(R.id.btnEasy)      // Sao Hỏa
        val btnMercury = findViewById<Button>(R.id.btnMedium) // Sao Thủy
        val btnSaturn = findViewById<Button>(R.id.btnHard)    // Sao Thổ

        btnMars.setOnClickListener { tryStartGame("MARS") }
        btnMercury.setOnClickListener { tryStartGame("MERCURY") }
        btnSaturn.setOnClickListener { tryStartGame("SATURN") }

        // ✅ Gọi load dữ liệu ngay khi mở app (UI ban đầu hiển thị đúng)
        loadPlayerData()
    }

    override fun onResume() {
        super.onResume()
        loadPlayerData() // luôn load lại khi về MainActivity
    }

    /**
     * ✅ Đọc dữ liệu từ PlayerDataManager và cập nhật UI
     */
    private fun loadPlayerData() {
        val coins = PlayerDataManager.getCoins(this)
        val energy = PlayerDataManager.getEnergy(this)
        val gems = PlayerDataManager.getGems(this)

        tvCoin.text = coins.toString()
        tvEnergy.text = "$energy/30"
        tvGem.text = gems.toString()
    }

    /**
     * ✅ Kiểm tra năng lượng trước khi mở màn chơi mới
     */
    private fun tryStartGame(planet: String) {
        if (PlayerDataManager.useEnergy(this, 1)) {
            startGame(planet)
        } else {
            Toast.makeText(this, "Không đủ Energy!", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * ✅ Mở màn chơi mới (chỉ khi còn Energy)
     */
    private fun startGame(planet: String) {
        val intent = Intent(this, GameActivity::class.java)
        intent.putExtra("LEVEL", planet)
        gameLauncher.launch(intent) // dùng launcher thay vì startActivity
    }
}
