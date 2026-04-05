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
        val causeLv = findViewById<ListView>(R.id.causeLv)
        val actionsLv = findViewById<ListView>(R.id.actionsLv)
        val checkAgainBtn = findViewById<Button>(R.id.checkAgainBtn)
        val saveReportBtn = findViewById<Button>(R.id.saveReportBtn)

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

            diseaseTv.text = disease
            confidenceTv.text = "Confidence: $confidence"

            val causesList = mutableListOf<String>()
            for (i in 0 until causesArray.length()) {
                causesList.add(causesArray.getString(i))
            }

            val actionsList = mutableListOf<String>()
            for (i in 0 until actionsArray.length()) {
                actionsList.add(actionsArray.getString(i))
            }

            causeLv.adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, causesList)
            actionsLv.adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, actionsList)

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