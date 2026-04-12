package com.voxcom.haai

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.viewpager2.widget.ViewPager2

class OnboardingActivity : AppCompatActivity() {

    private lateinit var viewPager: ViewPager2
    private lateinit var nextBtn: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_onboarding)

        viewPager = findViewById(R.id.viewPager)
        nextBtn = findViewById(R.id.nextBtn)

        val list = listOf(
            OnboardingData(
                R.drawable.ic_main,
                "Understand Your Symptoms",
                "This app helps you better understand the symptoms you experience in your daily life. " +
                        "You can explore common health conditions based on what you feel. " +
                        "It provides simple and easy-to-understand explanations for various symptoms. " +
                        "Instead of guessing, you get structured information that guides your awareness. " +
                        "The goal is to make health knowledge accessible to everyone. " +
                        "You can learn how symptoms may be connected to lifestyle or habits. " +
                        "This helps you stay more aware of your body and its signals. " +
                        "Early awareness can help you make better decisions about your health. " +
                        "It empowers you with knowledge before taking further action."
            ),

            OnboardingData(
                R.drawable.ic_main,
                "AI Powered Health Insights",
                "Our app uses artificial intelligence to analyze the information you provide. " +
                        "It gives you smart and relevant health insights based on your symptoms. " +
                        "The AI is designed to simplify complex medical information into easy guidance. " +
                        "You receive suggestions that can help you understand possible causes. " +
                        "It continuously improves to provide better and more accurate responses. " +
                        "The system focuses on awareness, not diagnosis. " +
                        "It helps you think more clearly about your health situation. " +
                        "You can use these insights to prepare before consulting a professional. " +
                        "This makes your healthcare journey more informed and efficient."
            ),

            OnboardingData(
                R.drawable.ic_main,
                "Disclaimer",
                "This application is designed only for general health awareness and educational purposes. " +
                        "It does not provide medical advice, diagnosis, or treatment. " +
                        "The information shown is generated using AI and may not always be accurate. " +
                        "You should not rely on this app as a substitute for professional medical consultation. " +
                        "Always consult a qualified doctor or healthcare provider for serious concerns. " +
                        "In case of emergency, seek immediate medical help. " +
                        "Using this app means you understand its limitations. " +
                        "The developers are not responsible for any decisions made based on this information. " +
                        "Please use this tool responsibly and prioritize professional healthcare."
            )
        )

        val adapter = OnboardingAdapter(list)
        viewPager.adapter = adapter

        nextBtn.setOnClickListener {
            if (viewPager.currentItem < list.size - 1) {
                viewPager.currentItem += 1
            } else {

                if (!adapter.isDisclaimerAccepted) {
                    Toast.makeText(this, "Please accept the disclaimer to continue", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }

                startActivity(Intent(this, MainActivity::class.java))
                finish()
            }
        }

        viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                if (position == list.size - 1) {
                    nextBtn.text = "Continue"
                } else {
                    nextBtn.text = "Next"
                }
            }
        })
    }
}