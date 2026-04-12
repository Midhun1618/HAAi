package com.voxcom.haai

import android.content.Intent
import android.health.connect.datatypes.units.Temperature
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class MainActivity : AppCompatActivity() {
    private lateinit var greet : TextView
    private lateinit var mailTv : TextView
    private lateinit var ageTv : TextView
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        val SymptomsBtn = findViewById<CardView>(R.id.card1)
        val TipsBtn = findViewById<CardView>(R.id.card2)
        val EmergencyBtn = findViewById<CardView>(R.id.card4)

        greet = findViewById(R.id.greetingTv)
        mailTv = findViewById(R.id.emailTv)
        ageTv = findViewById(R.id.ageTvm)

        val today = Calendar.getInstance()


        val user = UserManager.getUser(this)

        user?.let {
            val name = it.name
            val email = it.email
            val age = getAgeFromDob(it.dob)
            if (today.equals(user?.dob)){
                greet.text = "Happy Birthday, $name"
            }else{
                greet.text = "Hi, $name"
            }
            ageTv.text=age.toString()
            mailTv.text =email
        } ?: run {
            Toast.makeText(this, "User data not found", Toast.LENGTH_SHORT).show()
        }


        SymptomsBtn.setOnClickListener {
            startActivity(Intent(this, SymptomsActivity::class.java))
        }
        TipsBtn.setOnClickListener {
            startActivity(Intent(this, HealthTipsActivity::class.java))
        }
        EmergencyBtn.setOnClickListener {
            startActivity(Intent(this, EmergencyActivity::class.java))
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