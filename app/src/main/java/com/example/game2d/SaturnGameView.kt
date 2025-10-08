package com.example.game2d

import android.content.Context
import android.util.AttributeSet
import android.view.SurfaceHolder
import com.example.game2d.core.GameView
import com.example.game2d.managers.DifficultyManager

class SaturnGameView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : GameView(context, attrs, defStyleAttr) {

    override fun surfaceCreated(holder: SurfaceHolder) {
        super.surfaceCreated(holder)

        DifficultyManager.apply("SATURN", player, this)
        entityManager.setBackground(context, R.drawable.bgr_saturn, screenW, screenH)

        // Set bitmap cho enemy và boss
        entityManager.setEnemyBitmap(context, R.drawable.enemy11)
        entityManager.setBossBitmap(context, R.drawable.boss_saturn)
    }
}