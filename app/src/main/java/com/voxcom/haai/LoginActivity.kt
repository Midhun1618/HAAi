package com.voxcom.haai

import android.app.DatePickerDialog
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import android.view.View
import androidx.lifecycle.lifecycleScope

import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest

import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.database.FirebaseDatabase
import kotlinx.coroutines.launch
import java.util.Calendar

class LoginActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth

    private lateinit var googleBtn: TextView
    private lateinit var loginDetailLL: LinearLayout
    private lateinit var emailTv: TextView
    private lateinit var nameEt: EditText
    private lateinit var dobEt: EditText
    private lateinit var loginBtn: Button

    private var selectedGender = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        auth = FirebaseAuth.getInstance()

        googleBtn = findViewById(R.id.loginWgooleBtn)
        loginDetailLL = findViewById(R.id.loginDetailLL)
        emailTv = findViewById(R.id.emailTv)
        nameEt = findViewById(R.id.nameEt)
        dobEt = findViewById(R.id.additionalInput)
        loginBtn = findViewById(R.id.nextpage)

        val maleBtn = findViewById<TextView>(R.id.maleBtn)
        val femaleBtn = findViewById<TextView>(R.id.femaleBtn)

        // 🔐 Google Login
        googleBtn.setOnClickListener {
            signInWithGoogle()
        }

        // 🎯 Gender selection
        maleBtn.setOnClickListener {
            selectedGender = "Male"
            Toast.makeText(this, "Male selected", Toast.LENGTH_SHORT).show()
        }

        femaleBtn.setOnClickListener {
            selectedGender = "Female"
            Toast.makeText(this, "Female selected", Toast.LENGTH_SHORT).show()
        }

        // 👉 Save profile
        loginBtn.setOnClickListener {
            val name = nameEt.text.toString().trim()
            val dob = dobEt.text.toString().trim()
            val gender = selectedGender

            if (name.isEmpty() || dob.isEmpty()|| gender.isEmpty()) {
                Toast.makeText(this, "Fill all details 😅", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }else{
                val user = User(
                    name = name,
                    email = auth.currentUser?.email ?: "",
                    dob = dob,
                    gender = gender
                )
                UserManager.saveUser(this, user)
                saveUserToFirebase(name, dob, gender)
                startActivity(Intent(this, OnboardingActivity::class.java))
                finish()
            }


        }

        dobEt.setOnClickListener {

            val calendar = Calendar.getInstance()

            val year = calendar.get(Calendar.YEAR)
            val month = calendar.get(Calendar.MONTH)
            val day = calendar.get(Calendar.DAY_OF_MONTH)

            val datePicker = DatePickerDialog(
                this,
                { _, selectedYear, selectedMonth, selectedDay ->

                    val formattedDate =
                        "$selectedDay/${selectedMonth + 1}/$selectedYear"

                    dobEt.setText(formattedDate)
                },
                year,
                month,
                day
            )

            datePicker.datePicker.maxDate = System.currentTimeMillis()

            datePicker.show()
        }
    }

    // 🔐 Google Sign-In
    private fun signInWithGoogle() {

        val credentialManager = CredentialManager.create(this)

        val googleIdOption = GetGoogleIdOption.Builder()
            .setFilterByAuthorizedAccounts(false)
            .setServerClientId(getString(R.string.default_web_client_id))
            .build()

        val request = GetCredentialRequest.Builder()
            .addCredentialOption(googleIdOption)
            .build()

        lifecycleScope.launch {
            try {
                val result = credentialManager.getCredential(this@LoginActivity, request)

                val credential = result.credential

                if (credential is CustomCredential &&
                    credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
                ) {

                    val googleCredential =
                        GoogleIdTokenCredential.createFrom(credential.data)

                    firebaseAuthWithGoogle(googleCredential)

                }

            } catch (e: Exception) {
                Toast.makeText(this@LoginActivity, "Login cancelled", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // 🔥 Firebase Auth
    private fun firebaseAuthWithGoogle(credential: GoogleIdTokenCredential) {

        val firebaseCredential =
            GoogleAuthProvider.getCredential(credential.idToken, null)

        auth.signInWithCredential(firebaseCredential)
            .addOnSuccessListener {

                val user = auth.currentUser

                // Show UI after login
                googleBtn.visibility = View.GONE
                loginDetailLL.visibility = View.VISIBLE

                emailTv.text = user?.email ?: "No Email"

                Toast.makeText(this, "Google Login Success ✅", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener {
                Toast.makeText(this, "Auth Failed ❌", Toast.LENGTH_SHORT).show()
            }
    }

    // 💾 Save user data
    private fun saveUserToFirebase(name: String, dob: String, gender: String) {

        val uid = auth.currentUser?.uid ?: return

        val ref = FirebaseDatabase.getInstance()
            .getReference("users")
            .child(uid)

        val userMap = mapOf(
            "name" to name,
            "email" to auth.currentUser?.email,
            "dob" to dob,
            "gender" to gender
        )

        ref.setValue(userMap)
            .addOnSuccessListener {
                Toast.makeText(this, "Profile Saved ✅", Toast.LENGTH_SHORT).show()

                startActivity(Intent(this, OnboardingActivity::class.java))
                finish()
            }
            .addOnFailureListener {
                Toast.makeText(this, "Failed to save ❌", Toast.LENGTH_SHORT).show()
            }
    }
}