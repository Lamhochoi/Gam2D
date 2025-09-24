package com.example.game2d

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import android.view.animation.AnimationUtils

data class ShopItem(
    val name: String,
    val amount: Int, // Đổi từ energyAmount thành amount để tổng quát (Energy hoặc Gems)
    val gemCost: Int,
    val coinCost: Int,
    val iconResId: Int,
    val currencyType: String // "GEMS" hoặc "COINS"
)

class ShopItemAdapter(
    private val items: List<ShopItem>,
    private val onBuyClick: (ShopItem) -> Unit
) : RecyclerView.Adapter<ShopItemAdapter.ShopItemViewHolder>() {

    inner class ShopItemViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val ivIcon: ImageView = itemView.findViewById(R.id.ivItemIcon)
        val tvName: TextView = itemView.findViewById(R.id.tvItemName)
        val tvPrice: TextView = itemView.findViewById(R.id.tvItemPrice)
        val btnBuy: Button = itemView.findViewById(R.id.btnBuyItem)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ShopItemViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.shop_item_layout, parent, false)
        return ShopItemViewHolder(view)
    }

    override fun onBindViewHolder(holder: ShopItemViewHolder, position: Int) {
        val item = items[position]
        Log.d("ShopItemAdapter", "Binding item: ${item.name}, amount=${item.amount}, gemCost=${item.gemCost}, coinCost=${item.coinCost}, currency=${item.currencyType}")
        holder.ivIcon.setImageResource(item.iconResId)
        holder.tvName.text = item.name
        holder.tvPrice.text = if (item.currencyType == "GEMS") "${item.gemCost} Gems" else "${item.coinCost} Coins"
        holder.btnBuy.setOnClickListener {
            Log.d("ShopItemAdapter", "Buy clicked for ${item.name}")
            // Áp dụng animation khi nhấn
            val animation = AnimationUtils.loadAnimation(holder.itemView.context, R.anim.button_scale)
            holder.btnBuy.startAnimation(animation)
            onBuyClick(item)
        }
    }

    override fun getItemCount(): Int = items.size
}