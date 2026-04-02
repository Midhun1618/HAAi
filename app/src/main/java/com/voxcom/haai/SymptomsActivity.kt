package com.voxcom.haai

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class SymptomsActivity : AppCompatActivity() {
    private lateinit var sym01 : Button
    private lateinit var sym02 : Button
    private lateinit var sym03 : Button
    private lateinit var sym04 : Button
    private lateinit var sym05 : Button
    private lateinit var sym06 : Button
    private lateinit var sym07 : Button
    private lateinit var sym08 : Button
    private lateinit var sym09 : Button
    private lateinit var sym10 : Button

    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_symptoms)
        val buttons = listOf(
            findViewById<Button>(R.id.symp1),
            findViewById<Button>(R.id.symp2),
            findViewById<Button>(R.id.symp3),
            findViewById<Button>(R.id.symp4),
            findViewById<Button>(R.id.symp5),
            findViewById<Button>(R.id.symp6),
            findViewById<Button>(R.id.symp7),
            findViewById<Button>(R.id.symp8),
            findViewById<Button>(R.id.symp9),
            findViewById<Button>(R.id.symp10)
        )
        val states = MutableList(10) { false }

        buttons.forEachIndexed { index, button ->
            button.setOnClickListener {
                states[index] = !states[index]

                if (states[index]) {
                    button.setBackgroundColor(getColor(R.color.gradient_start))
                } else {
                    button.setBackgroundColor(getColor(R.color.text_secondary))
                }
            }
        }

        val checkNow = findViewById<Button>(R.id.checkNow)
        checkNow.setOnClickListener {
            startActivity(Intent(this, AiProcessingActivity::class.java))
            finish()
        }
    }
}