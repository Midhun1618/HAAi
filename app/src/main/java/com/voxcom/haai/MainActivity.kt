package com.voxcom.haai

import android.animation.ObjectAnimator
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.firebase.auth.FirebaseAuth
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import kotlin.random.Random
import androidx.core.view.isVisible
import com.google.android.material.progressindicator.LinearProgressIndicator

class MainActivity : AppCompatActivity() {
    private lateinit var greet : TextView
    private lateinit var mailTv : TextView
    private lateinit var ageTv : TextView
    private lateinit var textView: TextView
    private lateinit var button: ImageView
    private lateinit var logout: ImageView
    private lateinit var genderIcon : ImageView
    private lateinit var texts: Array<String>
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { view, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())

            view.setPadding(
                view.paddingLeft,
                systemBars.top,
                view.paddingRight,
                systemBars.bottom
            )

            insets
        }

        val SymptomsBtn = findViewById<CardView>(R.id.card1)
        val EmergencyBtn = findViewById<CardView>(R.id.card4)
        val ReportsBtn = findViewById<CardView>(R.id.card3)
        val govtWebBtn = findViewById<CardView>(R.id.card2)

        greet = findViewById(R.id.greetingTv)
        mailTv = findViewById(R.id.emailTv)
        ageTv = findViewById(R.id.ageTvm)
        genderIcon = findViewById(R.id.gender_icon)

        textView = findViewById(R.id.tipTv)
        button = findViewById(R.id.buttonGenerate)
        logout = findViewById(R.id.btnLogout)

        texts = resources.getStringArray(R.array.health_tips)


        showRandomText()

        ReminderScheduler.scheduleAll(this)

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
            ageTv.text="$age years"
            mailTv.text =email
            when (it.gender.trim().lowercase()) {
                "female" -> genderIcon.setImageResource(R.drawable.icon_female)
                "male" -> genderIcon.setImageResource(R.drawable.icon_male)
            }
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
        govtWebBtn.setOnClickListener {
            openHealthWebsiteWithConfirmation()
        }
        logout.setOnClickListener {
            logoutWithConfirmation()
        }

        val openPillTracker = View.OnClickListener {
            startActivity(Intent(this, PillTrackerActivity::class.java))
        }
        findViewById<View>(R.id.cardPillTracker).setOnClickListener(openPillTracker)
        findViewById<View>(R.id.btnAddMedicineHome).setOnClickListener {
            startActivity(Intent(this, AddMedicineActivity::class.java))
        }

    }
    override fun onResume() {
        super.onResume()
        updatePillCard()
    }
    private fun logoutWithConfirmation() {
        AlertDialog.Builder(this)
            .setTitle("Logout")
            .setMessage("Are you sure you want to log out?")
            .setPositiveButton("Yes") { _, _ ->
                ReminderScheduler.cancelAll(this)
                FirebaseAuth.getInstance().signOut()
                UserManager.clearUser(this)
                val intent = Intent(this, LoginActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
                finish()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
    private fun openHealthWebsiteWithConfirmation() {
        AlertDialog.Builder(this)
            .setTitle("Open Website")
            .setMessage("This will open the official health website in your browser.")
            .setPositiveButton("Open") { _, _ ->
                openHealthWebsite()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
    private fun openHealthWebsite() {
        val url = "https://www.mohfw.gov.in/"
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))

        if (intent.resolveActivity(packageManager) != null) {
            startActivity(intent)
        } else {
            Toast.makeText(this, "No browser found to open the link", Toast.LENGTH_SHORT).show()
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
    private fun updatePillCard() {
        val nextGroup = findViewById<View>(R.id.pillNextGroup)
        val emptyGroup = findViewById<View>(R.id.pillEmptyGroup)

        // State B: no medicines yet -> empty state with the Add button
        if (PillStore.getMedicines(this).isEmpty()) {
            nextGroup.isVisible = false
            emptyGroup.isVisible = true
            findViewById<TextView>(R.id.pillSubtitleTv).setText(R.string.pill_tracker_subtitle_empty)
            return
        }

        // State A: hero panel + progress
        emptyGroup.isVisible = false
        nextGroup.isVisible = true

        val timeTv = findViewById<TextView>(R.id.pillTimeTv)
        val nameTv = findViewById<TextView>(R.id.pillTitleTv)
        val doseTv = findViewById<TextView>(R.id.pillDoseTv)
        val countdownTv = findViewById<TextView>(R.id.pillCountdownTv)
        val markTakenBtn = findViewById<View>(R.id.btnMarkTaken)
        val progressTv = findViewById<TextView>(R.id.pillProgressTv)
        val progressBar = findViewById<LinearProgressIndicator>(R.id.pillProgress)

        val now = System.currentTimeMillis()
        val doses = DoseSchedule.dosesForDay(this, now, now)
        val next = doses.firstOrNull { it.state == DoseState.DUE || it.state == DoseState.UPCOMING }
        val taken = doses.count { it.state == DoseState.TAKEN }
        val missed = doses.count { it.state == DoseState.MISSED }

        if (next != null) {
            timeTv.text = DoseSchedule.display(this, next.time)
            nameTv.text = next.medicine.name

            val dose = next.medicine.dose
            doseTv.text = dose
            doseTv.isVisible = dose.isNotBlank()

            countdownTv.isVisible = true
            countdownTv.text = countdownText(next.scheduledAt - now)

            // Only offer "Mark as taken" when the dose can actually be logged right now
            markTakenBtn.isVisible = DoseSchedule.canLog(next, now)
            markTakenBtn.setOnClickListener {
                PillStore.logDose(this, next.medicine.id, next.scheduledAt, DoseStatus.TAKEN)
                NotificationHelper.cancel(this, next.medicine.id, next.scheduledAt)
                updatePillCard()   // refresh: moves on to the following dose + updates progress
            }
        } else {
            // Nothing left to take: reuse the same panel as a calm "all done" state
            timeTv.text = if (doses.isEmpty()) "Free day" else "All done"
            nameTv.text = if (doses.isEmpty()) "Nothing scheduled today" else "Today's doses are complete"
            doseTv.isVisible = false
            countdownTv.isVisible = false
            markTakenBtn.isVisible = false
        }

        // Progress row
        progressTv.text = buildString {
            append(if (doses.isEmpty()) "No doses" else "$taken of ${doses.size} taken")
            if (missed > 0) append(" · $missed missed")
        }
        progressBar.setProgressCompat(
            if (doses.isEmpty()) 0 else taken * 100 / doses.size,
            true
        )
    }
    private fun countdownText(deltaMs: Long): String {
        val mins = deltaMs / 60_000L
        return when {
            mins <= 0L -> "Due now"
            mins < 60L -> "in ${mins}m"
            else -> {
                val h = mins / 60
                val m = mins % 60
                if (m == 0L) "in ${h}h" else "in ${h}h ${m}m"
            }
        }
    }
}