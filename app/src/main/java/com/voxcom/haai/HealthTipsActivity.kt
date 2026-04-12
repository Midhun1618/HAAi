package com.voxcom.haai

import android.os.Bundle
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import kotlin.random.Random

class HealthTipsActivity : AppCompatActivity() {

    private lateinit var textView: TextView
    private lateinit var button: ImageView
    private lateinit var texts: Array<String>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_health_tips)

        textView = findViewById(R.id.tipTv)
        button = findViewById(R.id.buttonGenerate)

        // Load texts from strings.xml
        texts = resources.getStringArray(R.array.health_tips)

        // Show one random text initially
        showRandomText()

        button.setOnClickListener {
            showRandomText()
        }
    }

    private fun showRandomText() {
        val randomIndex = Random.nextInt(texts.size)
        textView.text = texts[randomIndex]
    }
}