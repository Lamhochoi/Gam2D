package com.example.game2d.managers

import android.content.Context
import android.util.Log

object PlayerDataManager {
    private const val TAG = "PlayerDataManager"
    private const val PREF_NAME = "game_data"
    private const val KEY_COINS = "total_coins"
    private const val KEY_ENERGY = "total_energy"
    private const val KEY_GEMS = "total_gems"

    private fun getPrefs(context: Context) =
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)

    // ================== COIN ==================
    fun getCoins(context: Context): Int {
        val v = getPrefs(context).getInt(KEY_COINS, 0)
        Log.d(TAG, "getCoins() -> $v")
        return v
    }

    fun addCoins(context: Context, amount: Int) {
        if (amount <= 0) {
            Log.d(TAG, "addCoins() ignored (amount=$amount)")
            return
        }
        val prefs = getPrefs(context)
        val old = prefs.getInt(KEY_COINS, 0)
        val total = old + amount
        val ok = prefs.edit().putInt(KEY_COINS, total).commit() // commit để chắc chắn
        Log.d(TAG, "addCoins(): old=$old amount=$amount => total=$total commit=$ok")
    }

    // ================== ENERGY ==================
    fun getEnergy(context: Context): Int {
        val v = getPrefs(context).getInt(KEY_ENERGY, 30)
        Log.d(TAG, "getEnergy() -> $v")
        return v
    }

    fun setEnergy(context: Context, value: Int) {
        getPrefs(context).edit().putInt(KEY_ENERGY, value).apply()
        Log.d(TAG, "setEnergy($value)")
    }

    fun useEnergy(context: Context, cost: Int): Boolean {
        val current = getEnergy(context)
        return if (current >= cost) {
            setEnergy(context, current - cost)
            Log.d(TAG, "useEnergy($cost) -> success, now ${current - cost}")
            true
        } else {
            Log.d(TAG, "useEnergy($cost) -> failed, current=$current")
            false
        }
    }

    // ================== GEM ==================
    fun getGems(context: Context): Int {
        val v = getPrefs(context).getInt(KEY_GEMS, 10)
        Log.d(TAG, "getGems() -> $v")
        return v
    }

    fun setGems(context: Context, value: Int) {
        getPrefs(context).edit().putInt(KEY_GEMS, value).apply()
        Log.d(TAG, "setGems($value)")
    }

    fun addGems(context: Context, amount: Int) {
        if (amount <= 0) {
            Log.d(TAG, "addGems() ignored (amount=$amount)")
            return
        }
        val prefs = getPrefs(context)
        val old = prefs.getInt(KEY_GEMS, 10)
        val total = old + amount
        prefs.edit().putInt(KEY_GEMS, total).apply()
        Log.d(TAG, "addGems(): old=$old amount=$amount => total=$total")
    }

    // ----- Debug helper -----
    fun clearAllForDebug(context: Context) {
        getPrefs(context).edit().clear().commit()
        Log.w(TAG, "clearAllForDebug(): preferences cleared")
    }
}
