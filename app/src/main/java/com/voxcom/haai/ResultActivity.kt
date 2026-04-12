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

        // date
        val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        dateTv.text = sdf.format(Date())

        val resultText = intent.getStringExtra("RESULT")
        val ageValue = intent.getStringExtra("AGE") ?: ""
        val durationValue = intent.getStringExtra("DURATION") ?: ""
        val symptoms = intent.getStringArrayListExtra("SYMPTOMS") ?: arrayListOf()

        ageTv.text = ageValue
        durationTv.text = "$durationValue days"

        var disease = ""
        var confidence = ""
        val causesList = mutableListOf<String>()
        val actionsList = mutableListOf<String>()

        try {
            val jsonObject = JSONObject(resultText!!)

            disease = jsonObject.getString("disease")
            confidence = jsonObject.getString("confidence")

            val causesArray = jsonObject.getJSONArray("causes")
            val actionsArray = jsonObject.getJSONArray("actions")

            diseaseTv.text = disease
            confidenceTv.text = "Confidence: $confidence"

            causeContainer.removeAllViews()
            actionContainer.removeAllViews()
            symptomsContainer.removeAllViews()

            for (i in 0 until causesArray.length()) {
                val text = causesArray.getString(i)
                causesList.add(text)

                val tv = TextView(this)
                tv.text = "• $text"
                tv.textSize = 18f
                tv.setPadding(8, 8, 8, 8)
                causeContainer.addView(tv)
            }

            for (symptom in symptoms) {
                val tv = TextView(this)
                tv.text = "• $symptom"
                tv.textSize = 18f
                tv.setPadding(8, 8, 8, 8)
                symptomsContainer.addView(tv)
            }

            for (i in 0 until actionsArray.length()) {
                val text = actionsArray.getString(i)
                actionsList.add(text)

                val tv = TextView(this)
                tv.text = "• $text"
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

            val report = JSONObject()

            report.put("date", dateTv.text.toString())
            report.put("disease", disease)
            report.put("confidence", confidence)
            report.put("age", ageValue)
            report.put("duration", durationValue)
            report.put("symptoms", symptoms)
            report.put("causes", causesList)
            report.put("actions", actionsList)

            ReportManager.saveReport(this, report)

            Toast.makeText(this, "Report Saved Successfully ✅", Toast.LENGTH_SHORT).show()

            saveReportBtn.isEnabled = false
            saveReportBtn.text = "Saved"
        }
    }
}