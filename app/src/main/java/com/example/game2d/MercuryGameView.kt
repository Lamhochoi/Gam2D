package com.example.game2d

import android.content.Context
import android.util.AttributeSet
import android.view.SurfaceHolder
import com.example.game2d.core.GameView
import com.example.game2d.managers.DifficultyManager

class MercuryGameView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : GameView(context, attrs, defStyleAttr) {

    override fun surfaceCreated(holder: SurfaceHolder) {
        super.surfaceCreated(holder)

        DifficultyManager.apply("MERCURY", player, this)
        entityManager.setBackground(context, R.drawable.bgr_mer, screenW, screenH)
    }
}