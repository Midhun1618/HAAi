package com.voxcom.haai

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat.startActivity

class LoginActivity : AppCompatActivity() {

    private lateinit var googleBtn: TextView
    private lateinit var loginDetailLL: LinearLayout
    private lateinit var emailTv: TextView
    private lateinit var nameEt: EditText
    private lateinit var dobEt: EditText
    private lateinit var loginBtn: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)


        googleBtn = findViewById(R.id.loginWgooleBtn)
        loginDetailLL = findViewById(R.id.loginDetailLL)
        emailTv = findViewById(R.id.emailTv)
        nameEt = findViewById(R.id.nameEt)
        dobEt = findViewById(R.id.additionalInput)
        loginBtn = findViewById(R.id.nextpage)

        emailTv.text = "user@gmail.com"


        googleBtn.setOnClickListener {
            googleBtn.visibility = TextView.GONE
            loginDetailLL.visibility = LinearLayout.VISIBLE

            Toast.makeText(this, "Google Login Success ✅", Toast.LENGTH_SHORT).show()
        }

        loginBtn.setOnClickListener {
            val name = nameEt.text.toString()
            val dob = dobEt.text.toString()

            if (name.isEmpty() || dob.isEmpty()) {
                Toast.makeText(this, "Fill all details bro 😅", Toast.LENGTH_SHORT).show()
            } else {
                startActivity(Intent(this, OnboardingActivity::class.java))
                finish()
            }
        }
    }
}