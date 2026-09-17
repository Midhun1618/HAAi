package com.voxcom.haai

import android.app.DatePickerDialog
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.animation.DecelerateInterpolator
import android.widget.Button
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope

import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest

import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.launch
import java.util.Calendar

class LoginActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth

    private lateinit var headerLL: LinearLayout
    private lateinit var cardContainer: androidx.constraintlayout.widget.ConstraintLayout

    private lateinit var googleBtnWrap: FrameLayout
    private lateinit var googleBtnContent: LinearLayout
    private lateinit var googleLoadingPb: ProgressBar

    private lateinit var loginDetailLL: LinearLayout
    private lateinit var emailTv: TextView
    private lateinit var nameEt: EditText
    private lateinit var dobEt: TextView
    private lateinit var maleBtn: TextView
    private lateinit var femaleBtn: TextView
    private lateinit var loginBtn: Button
    private lateinit var loginLoadingPb: ProgressBar

    private var selectedGender = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()

        setContentView(R.layout.activity_login)

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

        auth = FirebaseAuth.getInstance()

        headerLL = findViewById(R.id.headerLL)
        cardContainer = findViewById(R.id.cardContainer)

        googleBtnWrap = findViewById(R.id.googleBtnWrap)
        googleBtnContent = findViewById(R.id.googleBtnContent)
        googleLoadingPb = findViewById(R.id.googleLoadingPb)

        loginDetailLL = findViewById(R.id.loginDetailLL)
        emailTv = findViewById(R.id.emailTv)
        nameEt = findViewById(R.id.nameEt)
        dobEt = findViewById(R.id.dobInput)
        maleBtn = findViewById(R.id.maleBtn)
        femaleBtn = findViewById(R.id.femaleBtn)
        loginBtn = findViewById(R.id.nextpage)
        loginLoadingPb = findViewById(R.id.loginLoadingPb)

        animateEntrance()

        googleBtnWrap.setOnClickListener {
            signInWithGoogle()
        }

        maleBtn.setOnClickListener { selectGender("Male") }
        femaleBtn.setOnClickListener { selectGender("Female") }

        loginBtn.setOnClickListener {
            val name = nameEt.text.toString().trim()
            val dob = dobEt.text.toString().trim()
            val gender = selectedGender

            if (name.isEmpty() || dob.isEmpty() || gender.isEmpty()) {
                Toast.makeText(this, "Fill all details", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (!isUserAdult(dob)) {
                Toast.makeText(this, "You must be at least 18 years old", Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }

            val user = User(
                name = name,
                email = auth.currentUser?.email ?: "",
                dob = dob,
                gender = gender
            )
            UserManager.saveUser(this, user)

            setLoginLoading(true)
            saveUserToFirebase(name, dob, gender)
        }

        dobEt.setOnClickListener {
            val calendar = Calendar.getInstance()
            val year = calendar.get(Calendar.YEAR)
            val month = calendar.get(Calendar.MONTH)
            val day = calendar.get(Calendar.DAY_OF_MONTH)

            val datePicker = DatePickerDialog(
                this,
                { _, selectedYear, selectedMonth, selectedDay ->
                    val formattedDate = "$selectedDay/${selectedMonth + 1}/$selectedYear"
                    dobEt.text = formattedDate
                },
                year, month, day
            )

            datePicker.datePicker.maxDate = System.currentTimeMillis()
            datePicker.show()
        }
    }

    /** Fade + slide up the header and card on screen open. */
    private fun animateEntrance() {
        headerLL.animate()
            .alpha(1f)
            .setDuration(400)
            .setInterpolator(DecelerateInterpolator())
            .start()

        cardContainer.animate()
            .alpha(1f)
            .translationY(0f)
            .setStartDelay(150)
            .setDuration(450)
            .setInterpolator(DecelerateInterpolator())
            .start()
    }

    private fun selectGender(gender: String) {
        selectedGender = gender

        val (activeBtn, inactiveBtn) = if (gender == "Male") maleBtn to femaleBtn else femaleBtn to maleBtn

        activeBtn.setTextColor(getColor(R.color.text_white))
        activeBtn.setBackgroundResource(R.drawable.blue_bubble)
        activeBtn.animate().scaleX(1f).scaleY(1f).setDuration(150).start()

        inactiveBtn.setTextColor(getColor(R.color.text_secondary))
        inactiveBtn.setBackgroundResource(R.drawable.curver_outliner)

        // small bounce feedback on the tapped chip
        activeBtn.scaleX = 0.9f
        activeBtn.scaleY = 0.9f
        activeBtn.animate().scaleX(1f).scaleY(1f).setDuration(150).start()
    }

    /** Toggles the Google button between its normal state and a spinner. */
    private fun setGoogleLoading(loading: Boolean) {
        googleBtnWrap.isEnabled = !loading
        googleBtnContent.visibility = if (loading) View.INVISIBLE else View.VISIBLE
        googleLoadingPb.visibility = if (loading) View.VISIBLE else View.GONE
    }

    /** Toggles the LOGIN button between its normal state and a spinner. */
    private fun setLoginLoading(loading: Boolean) {
        loginBtn.isEnabled = !loading
        loginBtn.text = if (loading) "" else "LOGIN"
        loginLoadingPb.visibility = if (loading) View.VISIBLE else View.GONE
        nameEt.isEnabled = !loading
        dobEt.isEnabled = !loading
        maleBtn.isEnabled = !loading
        femaleBtn.isEnabled = !loading
    }

    /** Crossfades from the Google button into the detail form. */
    private fun revealLoginDetails() {
        googleBtnWrap.animate()
            .alpha(0f)
            .setDuration(200)
            .withEndAction {
                googleBtnWrap.visibility = View.GONE

                loginDetailLL.visibility = View.VISIBLE
                loginDetailLL.alpha = 0f
                loginDetailLL.translationY = 30f

                loginDetailLL.animate()
                    .alpha(1f)
                    .translationY(0f)
                    .setDuration(350)
                    .setInterpolator(DecelerateInterpolator())
                    .start()
            }
            .start()
    }

    private fun signInWithGoogle() {
        setGoogleLoading(true)

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
                    val googleCredential = GoogleIdTokenCredential.createFrom(credential.data)
                    firebaseAuthWithGoogle(googleCredential)
                } else {
                    setGoogleLoading(false)
                }

            } catch (e: Exception) {
                setGoogleLoading(false)
                Toast.makeText(this@LoginActivity, "Login cancelled", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun firebaseAuthWithGoogle(credential: GoogleIdTokenCredential) {
        val firebaseCredential = GoogleAuthProvider.getCredential(credential.idToken, null)

        auth.signInWithCredential(firebaseCredential)
            .addOnSuccessListener {
                checkExistingUserAndProceed()
            }
            .addOnFailureListener {
                setGoogleLoading(false)
                Toast.makeText(this, "Auth Failed", Toast.LENGTH_SHORT).show()
            }
    }

    /**
     * After Google sign-in succeeds, look the user up in Firebase by uid.
     * - If a profile already exists: load it into UserManager and skip
     *   straight into the app (no need to re-enter name/dob/gender).
     * - If it doesn't exist: this is a first-time login, so show the
     *   detail form to collect name/dob/gender.
     */
    private fun checkExistingUserAndProceed() {
        val uid = auth.currentUser?.uid
        if (uid == null) {
            setGoogleLoading(false)
            Toast.makeText(this, "Not signed in", Toast.LENGTH_SHORT).show()
            return
        }

        val ref = FirebaseDatabase.getInstance()
            .getReference("users")
            .child(uid)

        ref.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                setGoogleLoading(false)

                if (snapshot.exists()) {
                    // Returning user — pull saved details and go straight into the app.
                    val name = snapshot.child("name").getValue(String::class.java) ?: ""
                    val email = snapshot.child("email").getValue(String::class.java)
                        ?: (auth.currentUser?.email ?: "")
                    val dob = snapshot.child("dob").getValue(String::class.java) ?: ""
                    val gender = snapshot.child("gender").getValue(String::class.java) ?: ""

                    val user = User(name = name, email = email, dob = dob, gender = gender)
                    UserManager.saveUser(this@LoginActivity, user)

                    // TODO: replace MainActivity with whatever your post-login/home screen is.
                    startActivity(Intent(this@LoginActivity, MainActivity::class.java))
                    finish()
                } else {
                    // First-time user — show the form to collect their details.
                    emailTv.text = auth.currentUser?.email ?: "No Email"
                    revealLoginDetails()
                }
            }

            override fun onCancelled(error: DatabaseError) {
                setGoogleLoading(false)
                Toast.makeText(
                    this@LoginActivity,
                    "Couldn't check your profile: ${error.message}",
                    Toast.LENGTH_SHORT
                ).show()
                // Fall back to showing the form rather than leaving the user stuck.
                emailTv.text = auth.currentUser?.email ?: "No Email"
                revealLoginDetails()
            }
        })
    }

    private fun saveUserToFirebase(name: String, dob: String, gender: String) {
        val uid = auth.currentUser?.uid

        if (uid == null) {
            setLoginLoading(false)
            Toast.makeText(this, "Not signed in", Toast.LENGTH_SHORT).show()
            return
        }

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
                startActivity(Intent(this, OnboardingActivity::class.java))
                finish()
            }
            .addOnFailureListener {
                setLoginLoading(false)
                Toast.makeText(this, "Failed to save", Toast.LENGTH_SHORT).show()
            }
    }

    private fun isUserAdult(dob: String): Boolean {
        return try {
            val parts = dob.split("/")
            val day = parts[0].toInt()
            val month = parts[1].toInt() - 1
            val year = parts[2].toInt()

            val dobCalendar = Calendar.getInstance()
            dobCalendar.set(year, month, day)

            val today = Calendar.getInstance()
            var age = today.get(Calendar.YEAR) - dobCalendar.get(Calendar.YEAR)

            if (today.get(Calendar.DAY_OF_YEAR) < dobCalendar.get(Calendar.DAY_OF_YEAR)) {
                age--
            }

            age >= 18
        } catch (e: Exception) {
            false
        }
    }
}