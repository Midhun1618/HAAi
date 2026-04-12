package com.voxcom.haai

import android.content.Intent
import android.os.Bundle
import android.widget.*
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.*

class ResultActivity : AppCompatActivity() {

    private lateinit var nameTv: TextView
    private lateinit var ageTv: TextView
    private lateinit var durationTv: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_result)

        val diseaseTv = findViewById<TextView>(R.id.diseaseTv)
        val confidenceTv = findViewById<TextView>(R.id.confidenceTv)
        val dateTv = findViewById<TextView>(R.id.dateTv)

        nameTv = findViewById(R.id.nameTv)
        ageTv = findViewById(R.id.ageTv)
        durationTv = findViewById(R.id.durationTv)

        val causeContainer = findViewById<LinearLayout>(R.id.causeLv)
        val actionContainer = findViewById<LinearLayout>(R.id.actionsLv)
        val symptomsContainer = findViewById<LinearLayout>(R.id.symptomsLv)

        val checkAgainBtn = findViewById<Button>(R.id.checkAgainBtn)
        val saveReportBtn = findViewById<Button>(R.id.saveReportBtn)

        // 📅 Date
        val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        dateTv.text = sdf.format(Date())

        // 📦 Intent Data
        val resultText = intent.getStringExtra("RESULT")
        val name = intent.getStringExtra("NAME") ?: "User"
        val ageValue = intent.getStringExtra("AGE") ?: ""
        val durationValue = intent.getStringExtra("DURATION") ?: ""
        val symptoms = intent.getStringArrayListExtra("SYMPTOMS") ?: arrayListOf()

        // UI Set
        nameTv.text = name
        ageTv.text = ageValue
        durationTv.text = "$durationValue days"

        var disease = ""
        var confidence = ""

        val causesList = mutableListOf<String>()
        val actionsList = mutableListOf<String>()

        try {
            if (!resultText.isNullOrEmpty()) {

                val jsonObject = JSONObject(resultText)

                disease = jsonObject.optString("disease", "Unknown")
                confidence = jsonObject.optString("confidence", "--")

                val causesArray = jsonObject.optJSONArray("causes") ?: JSONArray()
                val actionsArray = jsonObject.optJSONArray("actions") ?: JSONArray()

                diseaseTv.text = disease
                confidenceTv.text = "Confidence: $confidence"

                causeContainer.removeAllViews()
                actionContainer.removeAllViews()
                symptomsContainer.removeAllViews()

                // 🔹 Causes
                for (i in 0 until causesArray.length()) {
                    val text = causesArray.getString(i)
                    causesList.add(text)

                    val tv = TextView(this)
                    tv.text = "• $text"
                    tv.textSize = 18f
                    tv.setPadding(8, 8, 8, 8)
                    causeContainer.addView(tv)
                }

                // 🔹 Symptoms
                for (symptom in symptoms) {
                    val tv = TextView(this)
                    tv.text = "• $symptom"
                    tv.textSize = 18f
                    tv.setPadding(8, 8, 8, 8)
                    symptomsContainer.addView(tv)
                }

                // 🔹 Actions
                for (i in 0 until actionsArray.length()) {
                    val text = actionsArray.getString(i)
                    actionsList.add(text)

                    val tv = TextView(this)
                    tv.text = "• $text"
                    tv.textSize = 18f
                    tv.setPadding(8, 8, 8, 8)
                    actionContainer.addView(tv)
                }
            }

        } catch (e: Exception) {
            Toast.makeText(this, "Error parsing result", Toast.LENGTH_SHORT).show()
        }

        // 🔁 Check again
        checkAgainBtn.setOnClickListener {
            startActivity(Intent(this, SymptomsActivity::class.java))
            finish()
        }

        // 💾 Save Report (FIXED JSON STORAGE)
        saveReportBtn.setOnClickListener {

            val report = JSONObject()

            // ✅ Convert to JSONArray (IMPORTANT FIX)
            val symptomsArray = JSONArray()
            symptoms.forEach { symptomsArray.put(it) }

            val causesArray = JSONArray()
            causesList.forEach { causesArray.put(it) }

            val actionsArray = JSONArray()
            actionsList.forEach { actionsArray.put(it) }

            report.put("date", dateTv.text.toString())
            report.put("disease", disease)
            report.put("confidence", confidence)
            report.put("age", ageValue)
            report.put("duration", durationValue)

            report.put("symptoms", symptomsArray)
            report.put("causes", causesArray)
            report.put("actions", actionsArray)

            ReportManager.saveReport(this, report)

            Toast.makeText(this, "Report Saved Successfully ✅", Toast.LENGTH_SHORT).show()

            // 🔒 Disable button
            saveReportBtn.isEnabled = false
            saveReportBtn.text = "Saved"
        }
    }
}