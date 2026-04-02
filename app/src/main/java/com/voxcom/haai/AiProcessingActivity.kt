package com.voxcom.haai

import android.content.Intent
import android.os.Bundle
import android.view.animation.AnimationUtils
import android.widget.Button
import android.widget.ImageView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class AiProcessingActivity : AppCompatActivity() {
    private lateinit var loaderImg: ImageView
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_ai_processing)
        loaderImg = findViewById(R.id.loaderImageView)
        val rotationAnim = AnimationUtils.loadAnimation(this,R.anim.rotate)

        loaderImg.startAnimation(rotationAnim)
        loaderImg.setOnClickListener {
            startActivity(Intent(this, ResultActivity::class.java))
            finish()
        }
    }
}
