package com.voxcom.haai

import android.content.Intent
import android.os.Bundle
import android.widget.*
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.*

class ResultActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_result)

        // UI Elements
        val diseaseTv = findViewById<TextView>(R.id.diseaseTv)
        val confidenceTv = findViewById<TextView>(R.id.confidenceTv)
        val dateTv = findViewById<TextView>(R.id.dateTv)

        val causeContainer = findViewById<LinearLayout>(R.id.causeLv)
        val actionContainer = findViewById<LinearLayout>(R.id.actionsLv)

        val checkAgainBtn = findViewById<Button>(R.id.checkAgainBtn)
        val saveReportBtn = findViewById<Button>(R.id.saveReportBtn)

        // Date
        val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        dateTv.text = sdf.format(Date())

        val resultText = intent.getStringExtra("RESULT")

        try {
            val cleanJson = resultText
                ?.replace("```json", "")
                ?.replace("```", "")
                ?.trim()

            val jsonObject = JSONObject(cleanJson!!)

            val disease = jsonObject.getString("disease")
            val confidence = jsonObject.getString("confidence")

            val causesArray = jsonObject.getJSONArray("causes")
            val actionsArray = jsonObject.getJSONArray("actions")

            // Set main text
            diseaseTv.text = disease
            confidenceTv.text = "Confidence: $confidence"

            // 🔥 CLEAR OLD VIEWS (important if reused)
            causeContainer.removeAllViews()
            actionContainer.removeAllViews()

            // 🔥 ADD CAUSES
            for (i in 0 until causesArray.length()) {
                val tv = TextView(this)
                tv.text = "• " + causesArray.getString(i)
                tv.textSize = 18f
                tv.setPadding(8, 8, 8, 8)
                causeContainer.addView(tv)
            }

            // 🔥 ADD ACTIONS
            for (i in 0 until actionsArray.length()) {
                val tv = TextView(this)
                tv.text = "• " + actionsArray.getString(i)
                tv.textSize = 18f
                tv.setPadding(8, 8, 8, 8)
                actionContainer.addView(tv)
            }

        } catch (e: Exception) {
            Toast.makeText(this, "Error parsing result", Toast.LENGTH_SHORT).show()
        }

        checkAgainBtn.setOnClickListener {
            startActivity(Intent(this, SymptomsActivity::class.java))
            finish()
        }

        saveReportBtn.setOnClickListener {
            Toast.makeText(this, "Report Saved (feature coming 😄)", Toast.LENGTH_SHORT).show()
        }
    }
}