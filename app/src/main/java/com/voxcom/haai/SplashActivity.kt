package com.voxcom.haai

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.animation.AlphaAnimation
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth

class SplashActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        val logo = findViewById<ImageView>(R.id.logo)
        val fadeIn = AlphaAnimation(0f, 1f)

        fadeIn.duration = 1500
        logo.startAnimation(fadeIn)

        Handler(Looper.getMainLooper()).postDelayed({

            val firebaseUser = FirebaseAuth.getInstance().currentUser
            val localUser = UserManager.getUser(this)

            when {
                firebaseUser == null -> {
                    startActivity(Intent(this, LoginActivity::class.java))
                }

                localUser == null -> {
                    startActivity(Intent(this, LoginActivity::class.java))
                }

                else -> {
                    startActivity(Intent(this, MainActivity::class.java))
                }
            }

            finish()

        }, 2500)
    }
}