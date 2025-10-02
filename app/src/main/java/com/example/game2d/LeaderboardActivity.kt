package com.example.game2d

import android.content.Context
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.res.ResourcesCompat

class LeaderboardActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_leaderboard)

        val recyclerView = findViewById<RecyclerView>(R.id.leaderboard_recycler_view)
        recyclerView.layoutManager = LinearLayoutManager(this)

        val sharedPrefs = getSharedPreferences("leaderboard", Context.MODE_PRIVATE)
        val topTimes = sharedPrefs.getString("top_times", "")?.split(",")?.mapNotNull { it.toLongOrNull() } ?: emptyList()

        recyclerView.adapter = LeaderboardAdapter(topTimes)
    }
}

class LeaderboardAdapter(private val topTimes: List<Long>) : RecyclerView.Adapter<LeaderboardAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val rankText: TextView = view.findViewById(android.R.id.text1)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(android.R.layout.simple_list_item_1, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val rank = position + 1
        val time = topTimes.getOrNull(position) ?: 0L
        val minutes = time / 60000
        val seconds = (time % 60000) / 1000
        val milliseconds = time % 1000
        holder.rankText.text = "Top $rank: %02d:%02d.%03d".format(minutes, seconds, milliseconds)
        holder.rankText.textSize = 20f
        holder.rankText.setTextColor(0xFFFFFF00.toInt()) // Màu vàng
        holder.rankText.typeface = ResourcesCompat.getFont(holder.itemView.context, R.font.robotomono_bold)
    }

    override fun getItemCount(): Int = topTimes.size
}