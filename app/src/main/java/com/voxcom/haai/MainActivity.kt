package com.voxcom.haai

import android.animation.ObjectAnimator
import android.content.Intent
import android.health.connect.datatypes.units.Temperature
import android.os.Bundle
import android.widget.Button
import android.widget.ImageView
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
import kotlin.random.Random

class MainActivity : AppCompatActivity() {
    private lateinit var greet : TextView
    private lateinit var mailTv : TextView
    private lateinit var ageTv : TextView
    private lateinit var usernameTv : TextView
    private lateinit var textView: TextView
    private lateinit var button: ImageView
    private lateinit var texts: Array<String>
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        val SymptomsBtn = findViewById<CardView>(R.id.card1)
        val EmergencyBtn = findViewById<CardView>(R.id.card4)
        val ReportsBtn = findViewById<CardView>(R.id.card3)

        greet = findViewById(R.id.greetingTv)
        mailTv = findViewById(R.id.emailTv)
        ageTv = findViewById(R.id.ageTvm)
        usernameTv = findViewById(R.id.usernameTv)

        textView = findViewById(R.id.tipTv)
        button = findViewById(R.id.buttonGenerate)

        texts = resources.getStringArray(R.array.health_tips)


        showRandomText()

        button.setOnClickListener {
            val rotate = ObjectAnimator.ofFloat(button, "rotation", 0f, 360f)
            rotate.duration = 400
            rotate.start()

            showRandomText()
        }

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
            usernameTv.text = name
            ageTv.text="$age years"
            mailTv.text =email
        } ?: run {
            Toast.makeText(this, "User data not found", Toast.LENGTH_SHORT).show()
        }


        SymptomsBtn.setOnClickListener {
            startActivity(Intent(this, SymptomsActivity::class.java))
        }
        EmergencyBtn.setOnClickListener {
            startActivity(Intent(this, EmergencyActivity::class.java))
        }
        ReportsBtn.setOnClickListener {
            startActivity(Intent(this, ReportsActivity::class.java))
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

            if (today.get(Calendar.DAY_OF_YEAR) < dobCal.get(Calendar.DAY_OF_YEAR)) {
                age--
            }

            age
        } catch (e: Exception) {
            0
        }
    }
    private fun showRandomText() {

        val randomIndex = Random.nextInt(texts.size)

        textView.animate()
            .alpha(0f)
            .translationY(20f)
            .setDuration(150)
            .withEndAction {
                textView.text = texts[randomIndex]

                textView.translationY = -20f

                textView.animate()
                    .alpha(1f)
                    .translationY(0f)
                    .setDuration(300)
                    .start()
            }
            .start()
    }
}