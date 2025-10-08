package com.example.game2d

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class ChallengeLeaderboardFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_leaderboard, container, false)
        val recyclerView = view.findViewById<RecyclerView>(R.id.leaderboard_recycler_view)

        recyclerView.layoutManager = LinearLayoutManager(context)
        val sharedPrefs = requireContext().getSharedPreferences("challenge_leaderboard", Context.MODE_PRIVATE)
        val topTimes = sharedPrefs.getString("top_times", "")?.split(",")?.mapNotNull { it.toLongOrNull() } ?: emptyList()

        recyclerView.adapter = ChallengeLeaderboardAdapter(topTimes)
        return view
    }
}