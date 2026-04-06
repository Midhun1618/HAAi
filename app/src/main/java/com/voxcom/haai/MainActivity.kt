package com.voxcom.haai

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        val SymptomsBtn = findViewById<CardView>(R.id.card1)
        val TipsBtn = findViewById<CardView>(R.id.card2)
        SymptomsBtn.setOnClickListener {
            startActivity(Intent(this, SymptomsActivity::class.java))
        }
        TipsBtn.setOnClickListener {
            startActivity(Intent(this, HealthTipsActivity::class.java))
        }
    }
}