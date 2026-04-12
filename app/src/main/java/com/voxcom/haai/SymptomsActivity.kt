package com.voxcom.haai

import android.content.Intent
import android.os.Bundle
import android.widget.*
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class SymptomsActivity : AppCompatActivity() {

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

        val additionalInput = findViewById<EditText>(R.id.additionalInput)
        val durationEt = findViewById<EditText>(R.id.durationEt)
        val ageEt = findViewById<EditText>(R.id.ageEt)

        val user = UserManager.getUser(this)

        user?.let {
            val age = getAgeFromDob(it.dob)
            ageEt.setText(age.toString())
        } ?: run {
            Toast.makeText(this, "User data not found", Toast.LENGTH_SHORT).show()
        }

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

            val selectedSymptoms = mutableListOf<String>()

            buttons.forEachIndexed { index, button ->
                if (states[index]) {
                    selectedSymptoms.add(button.text.toString())
                }
            }

            if (selectedSymptoms.isEmpty()) {
                Toast.makeText(this, "Select at least one symptom", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val age = ageEt.text.toString()
            val duration = durationEt.text.toString()
            val extraInfo = additionalInput.text.toString()
            val gender = user?.gender

            if (age.isEmpty()) {
                Toast.makeText(this, "Enter age", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (duration.isEmpty()) {
                Toast.makeText(this, "Enter duration", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val prompt = """
                Act as a basic medical assistant.

                Patient Age: $age years
                Patient Gender : $gender
                Symptoms Duration: $duration days

                Symptoms: ${selectedSymptoms.joinToString(", ")}
                Additional Info: $extraInfo

                Analyze and return ONLY JSON:

                {
                  "disease": "",
                  "confidence": "",
                  "causes": [],
                  "actions": []
                }

                No markdown. No explanation.
            """.trimIndent()

            val intent = Intent(this, AiProcessingActivity::class.java)
            intent.putExtra("PROMPT", prompt)
            startActivity(intent)
            finish()
        }
    }
    fun getAgeFromDob(dob: String): Int {
        return try {
            val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
            val birthDate = sdf.parse(dob) ?: return 0

            val dobCal = Calendar.getInstance()
            dobCal.time = birthDate

            val today = Calendar.getInstance()

            var age = today.get(Calendar.YEAR) - dobCal.get(Calendar.YEAR)

            // adjust if birthday hasn't occurred yet this year
            if (today.get(Calendar.DAY_OF_YEAR) < dobCal.get(Calendar.DAY_OF_YEAR)) {
                age--
            }

            age
        } catch (e: Exception) {
            0 // fallback if parsing fails
        }
    }
}