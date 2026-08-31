package com.voxcom.haai

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class SymptomsActivity : AppCompatActivity() {

    private lateinit var selectedCountTv: TextView

    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)

        enableEdgeToEdge()

        setContentView(R.layout.activity_symptoms)


        // Symptom Buttons

        val buttons = listOf(

            findViewById<TextView>(R.id.symp1),
            findViewById<TextView>(R.id.symp2),
            findViewById<TextView>(R.id.symp3),
            findViewById<TextView>(R.id.symp4),
            findViewById<TextView>(R.id.symp5),
            findViewById<TextView>(R.id.symp6),
            findViewById<TextView>(R.id.symp7),
            findViewById<TextView>(R.id.symp8),
            findViewById<TextView>(R.id.symp9),
            findViewById<TextView>(R.id.symp10)

        )


        val states = MutableList(buttons.size) { false }


        // Views

        selectedCountTv =
            findViewById(R.id.selectedCountTv)

        val additionalInput =
            findViewById<EditText>(R.id.additionalInput)

        val durationEt =
            findViewById<EditText>(R.id.durationEt)

        val ageEt =
            findViewById<EditText>(R.id.ageEt)

        val checkNow =
            findViewById<Button>(R.id.checkNow)


        val user = UserManager.getUser(this)


        user?.let {

            val age = getAgeFromDob(it.dob)

            if (age > 0) {
                ageEt.setText(age.toString())
            }

        }


        buttons.forEachIndexed { index, button ->

            button.setOnClickListener {

                states[index] = !states[index]


                if (states[index]) {

                    button.setBackgroundResource(
                        R.drawable.selected
                    )

                    button.setTextColor(
                        getColor(R.color.text_white)
                    )

                } else {

                    button.setBackgroundResource(
                        R.drawable.unselected
                    )

                    button.setTextColor(
                        getColor(R.color.text_secondary)
                    )
                }


                updateSelectedCount(states.count { it })
            }
        }


        checkNow.setOnClickListener {

            val selectedSymptoms =
                mutableListOf<String>()


            buttons.forEachIndexed { index, button ->

                if (states[index]) {

                    selectedSymptoms.add(
                        button.text.toString()
                    )
                }
            }


            if (selectedSymptoms.isEmpty()) {

                Toast.makeText(
                    this,
                    "Please select at least one symptom",
                    Toast.LENGTH_SHORT
                ).show()

                return@setOnClickListener
            }


            val age =
                ageEt.text.toString().trim()

            val duration =
                durationEt.text.toString().trim()

            val extraInfo =
                additionalInput.text.toString().trim()

            val gender =
                user?.gender ?: "Not specified"

            val name =
                user?.name ?: "User"



            if (age.isEmpty()) {

                ageEt.requestFocus()

                Toast.makeText(
                    this,
                    "Please enter your age",
                    Toast.LENGTH_SHORT
                ).show()

                return@setOnClickListener
            }


            if (duration.isEmpty()) {

                durationEt.requestFocus()

                Toast.makeText(
                    this,
                    "Please enter symptom duration",
                    Toast.LENGTH_SHORT
                ).show()

                return@setOnClickListener
            }


            val ageValue = age.toIntOrNull()

            val durationValue = duration.toIntOrNull()


            if (ageValue == null || ageValue <= 0 || ageValue > 120) {

                Toast.makeText(
                    this,
                    "Please enter a valid age",
                    Toast.LENGTH_SHORT
                ).show()

                return@setOnClickListener
            }


            if (durationValue == null || durationValue <= 0) {

                Toast.makeText(
                    this,
                    "Please enter a valid duration",
                    Toast.LENGTH_SHORT
                ).show()

                return@setOnClickListener
            }


            val prompt = """

                Act as a health awareness assistant.

                Important:
                - Provide general health awareness only.
                - Do NOT claim to provide a medical diagnosis.
                - Do NOT present any condition as certain.
                - Consider the listed symptoms, age, gender and duration.
                - If serious symptoms such as chest pain could indicate an emergency,
                  clearly recommend immediate professional medical attention.

                Patient Information:
                Age: $age years
                Gender: $gender
                Symptoms Duration: $duration days

                Selected Symptoms:
                ${selectedSymptoms.joinToString(", ")}

                Additional Information:
                ${if (extraInfo.isEmpty()) "None provided" else extraInfo}

                Analyze the information and return ONLY valid JSON.

                Use this exact structure:

                {
                  "disease": "",
                  "confidence": "",
                  "causes": [],
                  "actions": []
                }

                The "disease" field should describe a possible health concern,
                not a confirmed diagnosis.

                No markdown.
                No explanation outside JSON.

            """.trimIndent()


            // Open AI Processing

            val intent = Intent(
                this,
                AiProcessingActivity::class.java
            )


            intent.putExtra(
                "PROMPT",
                prompt
            )

            intent.putExtra(
                "NAME",
                name
            )

            intent.putExtra(
                "AGE",
                age
            )

            intent.putExtra(
                "DURATION",
                duration
            )

            intent.putStringArrayListExtra(
                "SYMPTOMS",
                ArrayList(selectedSymptoms)
            )

            intent.putExtra(
                "EXTRA",
                extraInfo
            )


            startActivity(intent)
        }
    }


    private fun updateSelectedCount(count: Int) {

        selectedCountTv.text =
            when (count) {

                0 -> "0 selected"

                1 -> "1 selected"

                else -> "$count selected"
            }
    }


    private fun getAgeFromDob(
        dob: String
    ): Int {

        return try {

            val sdf = SimpleDateFormat(
                "dd/MM/yyyy",
                Locale.getDefault()
            )

            val birthDate =
                sdf.parse(dob) ?: return 0


            val dobCalendar =
                Calendar.getInstance()

            dobCalendar.time =
                birthDate


            val today =
                Calendar.getInstance()


            var age =
                today.get(Calendar.YEAR) -
                        dobCalendar.get(Calendar.YEAR)


            if (
                today.get(Calendar.DAY_OF_YEAR) <
                dobCalendar.get(Calendar.DAY_OF_YEAR)
            ) {
                age--
            }


            age

        } catch (e: Exception) {

            0
        }
    }
}