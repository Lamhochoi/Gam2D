package com.example.game2d.managers

import android.content.Context
import android.util.Log

object PlayerDataManager {
    private const val TAG = "PlayerDataManager"
    private const val PREF_NAME = "game_data"
    private const val KEY_COINS = "total_coins"
    private const val KEY_ENERGY = "total_energy"
    private const val KEY_GEMS = "total_gems"
    private const val KEY_LAST_ENERGY_UPDATE = "last_energy_update"
    private const val ENERGY_MAX = 30
    private const val ENERGY_RECHARGE_SECONDS = 120L

    private fun getPrefs(context: Context) =
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)

    fun getCoins(context: Context): Int {
        val prefs = getPrefs(context)
        val value = prefs.getInt(KEY_COINS, 0)
        Log.d(TAG, "getCoins(): value=$value")
        return value
    }

    fun addCoins(context: Context, amount: Int) {
        if (amount <= 0) {
            Log.d(TAG, "addCoins(): ignored (amount=$amount)")
            return
        }
        try {
            val prefs = getPrefs(context)
            val old = prefs.getInt(KEY_COINS, 0)
            val total = old + amount
            val ok = prefs.edit().putInt(KEY_COINS, total).commit()
            Log.d(TAG, "addCoins(): old=$old, amount=$amount, total=$total, commit=$ok")
        } catch (e: Exception) {
            Log.e(TAG, "addCoins failed: ${e.message}", e)
        }
    }

    fun getEnergy(context: Context): Int {
        val prefs = getPrefs(context)
        val currentEnergy = prefs.getInt(KEY_ENERGY, 30)
        val lastUpdate = prefs.getLong(KEY_LAST_ENERGY_UPDATE, 0)
        val currentTime = System.currentTimeMillis() / 1000

        if (lastUpdate > 0 && currentTime > lastUpdate) {
            val secondsElapsed = currentTime - lastUpdate
            val energyToAdd = (secondsElapsed / ENERGY_RECHARGE_SECONDS).toInt()
            if (energyToAdd > 0) {
                val newEnergy = minOf(currentEnergy + energyToAdd, ENERGY_MAX)
                prefs.edit()
                    .putInt(KEY_ENERGY, newEnergy)
                    .putLong(KEY_LAST_ENERGY_UPDATE, currentTime)
                    .commit()
                Log.d(TAG, "getEnergy(): added $energyToAdd, newEnergy=$newEnergy, lastUpdate=$currentTime")
                return newEnergy
            }
        }
        Log.d(TAG, "getEnergy(): value=$currentEnergy")
        return currentEnergy
    }

    fun setEnergy(context: Context, value: Int) {
        val newValue = value.coerceIn(0, ENERGY_MAX)
        getPrefs(context).edit()
            .putInt(KEY_ENERGY, newValue)
            .putLong(KEY_LAST_ENERGY_UPDATE, System.currentTimeMillis() / 1000)
            .commit()
        Log.d(TAG, "setEnergy($newValue)")
    }

    fun useEnergy(context: Context, cost: Int): Boolean {
        val current = getEnergy(context)
        return if (current >= cost) {
            setEnergy(context, current - cost)
            Log.d(TAG, "useEnergy($cost): success, now ${current - cost}")
            true
        } else {
            Log.d(TAG, "useEnergy($cost): failed, current=$current")
            false
        }
    }

    fun addEnergy(context: Context, amount: Int) {
        if (amount <= 0) {
            Log.d(TAG, "addEnergy(): ignored (amount=$amount)")
            return
        }
        val current = getEnergy(context)
        val newEnergy = minOf(current + amount, ENERGY_MAX)
        setEnergy(context, newEnergy)
        Log.d(TAG, "addEnergy(): current=$current, amount=$amount, newEnergy=$newEnergy")
    }

    fun getGems(context: Context): Int {
        val value = getPrefs(context).getInt(KEY_GEMS, 10)
        Log.d(TAG, "getGems(): value=$value")
        return value
    }

    fun setGems(context: Context, value: Int) {
        getPrefs(context).edit().putInt(KEY_GEMS, value).apply()
        Log.d(TAG, "setGems($value)")
    }

    fun addGems(context: Context, amount: Int) {
        if (amount <= 0) {
            Log.d(TAG, "addGems(): ignored (amount=$amount)")
            return
        }
        val prefs = getPrefs(context)
        val old = prefs.getInt(KEY_GEMS, 10)
        val total = old + amount
        prefs.edit().putInt(KEY_GEMS, total).apply()
        Log.d(TAG, "addGems(): old=$old, amount=$amount, total=$total")
    }

    fun buyEnergy(context: Context, energyAmount: Int, gemCost: Int): Boolean {
        val currentGems = getGems(context)
        val currentEnergy = getEnergy(context)
        if (currentGems >= gemCost) {
            val newEnergy = minOf(currentEnergy + energyAmount, ENERGY_MAX)
            val newGems = currentGems - gemCost
            getPrefs(context).edit()
                .putInt(KEY_ENERGY, newEnergy)
                .putInt(KEY_GEMS, newGems)
                .putLong(KEY_LAST_ENERGY_UPDATE, System.currentTimeMillis() / 1000)
                .commit()
            Log.d(TAG, "buyEnergy(): success, added $energyAmount energy, spent $gemCost gems, newEnergy=$newEnergy, newGems=$newGems")
            return true
        } else {
            Log.d(TAG, "buyEnergy(): failed, need $gemCost gems, have $currentGems")
            return false
        }
    }

    fun buyEnergyWithCoins(context: Context, energyAmount: Int, coinCost: Int): Boolean {
        val currentCoins = getCoins(context)
        val currentEnergy = getEnergy(context)
        if (currentCoins >= coinCost) {
            val newEnergy = minOf(currentEnergy + energyAmount, ENERGY_MAX)
            val newCoins = currentCoins - coinCost
            getPrefs(context).edit()
                .putInt(KEY_ENERGY, newEnergy)
                .putInt(KEY_COINS, newCoins)
                .putLong(KEY_LAST_ENERGY_UPDATE, System.currentTimeMillis() / 1000)
                .commit()
            Log.d(TAG, "buyEnergyWithCoins(): success, added $energyAmount energy, spent $coinCost coins, newEnergy=$newEnergy, newCoins=$newCoins")
            return true
        } else {
            Log.d(TAG, "buyEnergyWithCoins(): failed, need $coinCost coins, have $currentCoins")
            return false
        }
    }

    fun buyGemsWithCoins(context: Context, gemAmount: Int, coinCost: Int): Boolean {
        val currentCoins = getCoins(context)
        if (currentCoins >= coinCost) {
            val newGems = getGems(context) + gemAmount
            val newCoins = currentCoins - coinCost
            getPrefs(context).edit()
                .putInt(KEY_GEMS, newGems)
                .putInt(KEY_COINS, newCoins)
                .commit()
            Log.d(TAG, "buyGemsWithCoins(): success, added $gemAmount gems, spent $coinCost coins")
            return true
        } else {
            Log.d(TAG, "buyGemsWithCoins(): failed, need $coinCost coins, have $currentCoins")
            return false
        }
    }

    fun clearAllForDebug(context: Context) {
        getPrefs(context).edit().clear().commit()
        Log.w(TAG, "clearAllForDebug(): preferences cleared")
    }

    fun debugPrefs(context: Context) {
        val prefs = getPrefs(context).all
        Log.d(TAG, "debugPrefs(): $prefs")
    }
}