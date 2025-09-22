package com.example.game2d

import android.content.Context
import android.util.AttributeSet
import android.view.SurfaceHolder
import com.example.game2d.core.GameView
import com.example.game2d.managers.DifficultyManager

class MarsGameView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : GameView(context, attrs, defStyleAttr) {

    override fun surfaceCreated(holder: SurfaceHolder) {
        super.surfaceCreated(holder)

        // Áp dụng độ khó cho map Mars
        DifficultyManager.apply("MARS", player, this)

        // Set background sau khi có screenW, screenH
        entityManager.setBackground(context, R.drawable.mars_bgr, screenW, screenH)
    }
}