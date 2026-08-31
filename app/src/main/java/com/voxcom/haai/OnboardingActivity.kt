package com.voxcom.haai

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.viewpager2.widget.ViewPager2

class OnboardingActivity : AppCompatActivity() {

    private lateinit var viewPager: ViewPager2

    private lateinit var nextBtn: Button

    private lateinit var skipBtn: View

    private lateinit var indicators: List<View>

    private lateinit var adapter: OnboardingAdapter


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_onboarding)


        viewPager = findViewById(R.id.viewPager)

        nextBtn = findViewById(R.id.nextBtn)

        skipBtn = findViewById(R.id.skipBtn)


        indicators = listOf(

            findViewById(R.id.indicator1),

            findViewById(R.id.indicator2),

            findViewById(R.id.indicator3)

        )


        val list = listOf(

            OnboardingData(

                icon = R.drawable.ic_main,

                title = "Understand Your Symptoms",

                description =
                    "Select the symptoms you're experiencing and get structured health awareness insights to better understand what your body may be telling you.",

                banner = R.drawable.banner1

            ),


            OnboardingData(

                icon = R.drawable.ic_main,

                title = "AI-Powered Health Insights",

                description =
                    "HAAi analyzes your selected symptoms and provides possible health insights, helpful guidance, and suggested next steps.",

                banner = R.drawable.banner2

            ),


            OnboardingData(

                icon = R.drawable.ic_main,

                title = "Your Health, Used Responsibly",

                description =
                    "HAAi provides general health awareness information and is not a replacement for professional medical advice, diagnosis, or emergency care.",

                banner = R.drawable.banner3,

                isDisclaimer = true

            )

        )


        adapter = OnboardingAdapter(list) { accepted ->

            updateDisclaimerButton(accepted)

        }


        viewPager.adapter = adapter


        updateIndicators(0)


        // Next Button

        nextBtn.setOnClickListener {

            val currentPosition = viewPager.currentItem


            if (currentPosition < list.size - 1) {

                viewPager.currentItem = currentPosition + 1

            } else {

                if (!adapter.isDisclaimerAccepted) {

                    Toast.makeText(
                        this,
                        "Please accept the disclaimer to continue",
                        Toast.LENGTH_SHORT
                    ).show()

                    return@setOnClickListener
                }


                completeOnboarding()
            }

        }


        // Skip Button

        skipBtn.setOnClickListener {

            viewPager.currentItem = list.size - 1

        }


        // Page Change

        viewPager.registerOnPageChangeCallback(

            object : ViewPager2.OnPageChangeCallback() {

                override fun onPageSelected(position: Int) {

                    updateIndicators(position)


                    if (position == list.size - 1) {

                        nextBtn.text = "Get Started"

                        skipBtn.visibility = View.GONE

                        updateDisclaimerButton(
                            adapter.isDisclaimerAccepted
                        )

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


    private fun updateIndicators(position: Int) {

        indicators.forEachIndexed { index, indicator ->

            if (index == position) {

                indicator.setBackgroundResource(
                    R.drawable.indicator_active
                )

                indicator.layoutParams.width =
                    dpToPx(22)

                indicator.layoutParams.height =
                    dpToPx(8)

            } else {

                indicator.setBackgroundResource(
                    R.drawable.indicator_inactive
                )

                indicator.layoutParams.width =
                    dpToPx(8)

                indicator.layoutParams.height =
                    dpToPx(8)
            }

            indicator.requestLayout()
        }
    }


    private fun updateDisclaimerButton(
        accepted: Boolean
    ) {

        if (viewPager.currentItem == 2) {

            nextBtn.isEnabled = accepted

            nextBtn.alpha =
                if (accepted) 1f else 0.5f
        }
    }


    private fun completeOnboarding() {

        val preferences = getSharedPreferences(
            "HAAiPrefs",
            MODE_PRIVATE
        )


        preferences.edit()
            .putBoolean(
                "onboarding_completed",
                true
            )
            .apply()


        startActivity(

            Intent(
                this,
                MainActivity::class.java
            )

        )


        finish()
    }


    private fun dpToPx(dp: Int): Int {

        return (
                dp * resources.displayMetrics.density
                ).toInt()
    }
}