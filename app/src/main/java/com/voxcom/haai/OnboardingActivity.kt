package com.voxcom.haai

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.animation.DecelerateInterpolator
import android.view.animation.OvershootInterpolator
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.viewpager2.widget.ViewPager2
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

class OnboardingActivity : AppCompatActivity() {

    private lateinit var viewPager: ViewPager2
    private lateinit var nextBtn: Button
    private lateinit var skipBtn: View
    private lateinit var greetTv: TextView
    private lateinit var indicators: List<View>
    private lateinit var adapter: OnboardingAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_onboarding)

        viewPager = findViewById(R.id.viewPager)
        nextBtn = findViewById(R.id.nextBtn)
        skipBtn = findViewById(R.id.skipBtn)
        greetTv = findViewById(R.id.greetTv)

        indicators = listOf(
            findViewById(R.id.indicator1),
            findViewById(R.id.indicator2),
            findViewById(R.id.indicator3)
        )

        setGreeting()

        val list = listOf(
            OnboardingData(
                icon = R.drawable.ic_main,
                title = "Understand Your Symptoms",
                description = "Select the symptoms you're experiencing and get structured health awareness insights to better understand what your body may be telling you.",
                banner = R.drawable.illus1_onboard
            ),
            OnboardingData(
                icon = R.drawable.ic_main,
                title = "AI-Powered Health Insights",
                description = "HAAi analyzes your selected symptoms and provides possible health insights, helpful guidance, and suggested next steps.",
                banner = R.drawable.illus2_onboard
            ),
            OnboardingData(
                icon = R.drawable.ic_main,
                title = "Your Health, Used Responsibly",
                description = "HAAi provides general health awareness information and is not a replacement for professional medical advice, diagnosis, or emergency care.",
                banner = R.drawable.illus3_onboard,
                isDisclaimer = true
            )
        )

        adapter = OnboardingAdapter(list) { accepted ->
            updateDisclaimerButton(accepted)
        }

        viewPager.adapter = adapter

        // Smooth slide-fade transition between pages
        viewPager.setPageTransformer(SlideFadePageTransformer())

        updateIndicators(0)
        animateEntrance()

        nextBtn.setOnClickListener {
            val currentPosition = viewPager.currentItem
            if (currentPosition < list.size - 1) {
                viewPager.currentItem = currentPosition + 1
            } else {
                if (!adapter.isDisclaimerAccepted) {
                    Toast.makeText(this, "Please accept the disclaimer to continue", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
                completeOnboarding()
            }
        }

        skipBtn.setOnClickListener {
            viewPager.currentItem = list.size - 1
        }

        viewPager.registerOnPageChangeCallback(
            object : ViewPager2.OnPageChangeCallback() {
                override fun onPageSelected(position: Int) {
                    updateIndicators(position)

                    if (position == list.size - 1) {
                        nextBtn.text = "Get Started"
                        skipBtn.visibility = View.GONE
                        updateDisclaimerButton(adapter.isDisclaimerAccepted)
                    } else {
                        nextBtn.text = "Continue"
                        nextBtn.isEnabled = true
                        nextBtn.alpha = 1f
                        skipBtn.visibility = View.VISIBLE
                    }
                }
            }
        )
    }

    /** Fetches the current user and sets the greeting text, with a birthday check. */


    /** Entrance animation: greeting fades/slides down first, then the ViewPager slides in from the side. */
    private fun animateEntrance() {
        // Initial state
        greetTv.alpha = 0f
        greetTv.translationY = -dpToPx(24).toFloat()

        viewPager.alpha = 0f
        viewPager.translationX = dpToPx(80).toFloat()

        greetTv.animate()
            .alpha(1f)
            .translationY(0f)
            .setDuration(450)
            .setInterpolator(DecelerateInterpolator())
            .start()

        viewPager.animate()
            .alpha(1f)
            .translationX(0f)
            .setStartDelay(200)
            .setDuration(500)
            .setInterpolator(OvershootInterpolator(0.9f))
            .start()
    }

    private fun updateIndicators(position: Int) {
        indicators.forEachIndexed { index, indicator ->
            if (index == position) {
                indicator.setBackgroundResource(R.drawable.indicator_active)
                indicator.layoutParams.width = dpToPx(22)
                indicator.layoutParams.height = dpToPx(8)
            } else {
                indicator.setBackgroundResource(R.drawable.indicator_inactive)
                indicator.layoutParams.width = dpToPx(8)
                indicator.layoutParams.height = dpToPx(8)
            }
            indicator.requestLayout()
        }
    }

    private fun updateDisclaimerButton(accepted: Boolean) {
        if (viewPager.currentItem == 2) {
            nextBtn.isEnabled = accepted
            nextBtn.alpha = if (accepted) 1f else 0.5f
        }
    }

    private fun completeOnboarding() {
        val preferences = getSharedPreferences("HAAiPrefs", MODE_PRIVATE)
        preferences.edit().putBoolean("onboarding_completed", true).apply()
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }

    private fun dpToPx(dp: Int): Int {
        return (dp * resources.displayMetrics.density).toInt()
    }

    /** Fetches the current user and sets the greeting text, with a birthday check. */
    private fun setGreeting() {
        val user = UserManager.getUser(this)

        user?.let {
            val name = it.name
            val dobDate = parseDob(it.dob)

            val isBirthday = dobDate?.let { d ->
                val today = LocalDate.now()
                today.monthValue == d.monthValue && today.dayOfMonth == d.dayOfMonth
            } ?: false

            greetTv.text = if (isBirthday) {
                "Happy Birthday, $name 🎉"
            } else {
                "Hi, $name"
            }

            // val age = dobDate?.let { d -> ChronoUnit.YEARS.between(d, LocalDate.now()) }
            // age is available here if you later add an ageTv to this layout

        } ?: run {
            greetTv.text = "Hi there"
            Toast.makeText(this, "User data not found", Toast.LENGTH_SHORT).show()
        }
    }

    /** Parses a dob string ("yyyy-MM-dd") into a LocalDate, or null if invalid/missing. */
    private fun parseDob(dob: String?): LocalDate? {
        if (dob.isNullOrBlank()) return null
        return try {
            LocalDate.parse(dob, DateTimeFormatter.ISO_LOCAL_DATE)
        } catch (e: Exception) {
            null
        }
    }
}