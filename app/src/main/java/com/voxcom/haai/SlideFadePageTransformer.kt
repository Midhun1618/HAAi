package com.voxcom.haai

import android.view.View
import androidx.viewpager2.widget.ViewPager2
import kotlin.math.abs

class SlideFadePageTransformer : ViewPager2.PageTransformer {

    override fun transformPage(page: View, position: Float) {
        page.apply {
            when {
                position < -1f -> {
                    alpha = 0f
                }
                position <= 1f -> {
                    alpha = 1f - abs(position) * 0.6f

                    translationX = -position * width * 0.35f

                    val scale = 0.88f + (1f - abs(position)) * 0.12f
                    scaleX = scale
                    scaleY = scale

                    // Keep the page closest to center on top
                    translationZ = 1f - abs(position)
                }
                else -> {
                    alpha = 0f
                }
            }
        }
    }
}