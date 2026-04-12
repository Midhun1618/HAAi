package com.voxcom.haai

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.animation.AlphaAnimation
import android.view.animation.AnimationUtils
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import okhttp3.Call
import okhttp3.Callback
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit

class AiProcessingActivity : AppCompatActivity() {

    private lateinit var loaderImg: ImageView
    private lateinit var loaderTxt: TextView

    private val sentences = listOf(
        "Analysing your Symptoms",
        "This might take a few seconds",
        "Almost there",
        "Getting your Report ready"
    )

    private var index = 0
    private val handler = Handler(Looper.getMainLooper())
    private val delay: Long = 3000

    private val runnable = object : Runnable {
        override fun run() {
            changeTextWithFade()
            handler.postDelayed(this, delay)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_ai_processing)

        loaderImg = findViewById(R.id.loaderImageView)
        loaderTxt = findViewById(R.id.loaderTv)

        val rotationAnim = AnimationUtils.loadAnimation(this, R.anim.rotate)
        loaderImg.startAnimation(rotationAnim)

        val prompt = intent.getStringExtra("PROMPT") ?: ""
        val age = intent.getStringExtra("AGE") ?: ""
        val duration = intent.getStringExtra("DURATION") ?: ""
        val symptoms = intent.getStringArrayListExtra("SYMPTOMS") ?: arrayListOf()
        val extraInfo = intent.getStringExtra("EXTRA") ?: ""

        if (prompt.isEmpty()) {
            Toast.makeText(this, "No input received", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        callAI(prompt, age, duration, symptoms, extraInfo)

        handler.post(runnable)
    }

    private fun changeTextWithFade() {
        val fadeOut = AlphaAnimation(1f, 0f).apply {
            duration = 500
        }

        val fadeIn = AlphaAnimation(0f, 1f).apply {
            duration = 500
        }

        fadeOut.setAnimationListener(object : android.view.animation.Animation.AnimationListener {
            override fun onAnimationStart(animation: android.view.animation.Animation?) {}

            override fun onAnimationEnd(animation: android.view.animation.Animation?) {
                index = (index + 1) % sentences.size
                loaderTxt.text = sentences[index]
                loaderTxt.startAnimation(fadeIn)
            }

            override fun onAnimationRepeat(animation: android.view.animation.Animation?) {}
        })

        loaderTxt.startAnimation(fadeOut)
    }

    private fun callAI(
        prompt: String,
        age: String,
        duration: String,
        selectedSymptoms: ArrayList<String>,
        extraInfo: String
    ) {

        val client = OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .build()

        val jsonObject = JSONObject()

        val part = JSONObject().apply {
            put("text", prompt)
        }

        val partsArray = JSONArray().apply {
            put(part)
        }

        val content = JSONObject().apply {
            put("parts", partsArray)
        }

        val contentsArray = JSONArray().apply {
            put(content)
        }

        jsonObject.put("contents", contentsArray)

        val request = Request.Builder()
            .url("https://generativelanguage.googleapis.com/v1beta/models/${Info.model}:generateContent")
            .addHeader("Content-Type", "application/json")
            .addHeader("X-goog-api-key", Info.api)
            .post(jsonObject.toString().toRequestBody("application/json".toMediaType()))
            .build()

        client.newCall(request).enqueue(object : Callback {

            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread {
                    loaderImg.clearAnimation()
                    Toast.makeText(this@AiProcessingActivity, "API Failed", Toast.LENGTH_LONG).show()
                }
            }

            override fun onResponse(call: Call, response: Response) {

                if (!response.isSuccessful) {
                    runOnUiThread {
                        loaderImg.clearAnimation()
                        Toast.makeText(
                            this@AiProcessingActivity,
                            "Server Error ${response.code}",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                    return
                }

                val body = response.body?.string() ?: ""

                if (body.isEmpty()) {
                    runOnUiThread {
                        loaderImg.clearAnimation()
                        Toast.makeText(this@AiProcessingActivity, "Empty response", Toast.LENGTH_LONG).show()
                    }
                    return
                }

                try {
                    val json = JSONObject(body)

                    if (json.has("error")) {
                        val msg = json.getJSONObject("error").getString("message")

                        runOnUiThread {
                            loaderImg.clearAnimation()
                            Toast.makeText(this@AiProcessingActivity, msg, Toast.LENGTH_LONG).show()
                        }
                        return
                    }

                    val text = json
                        .getJSONArray("candidates")
                        .getJSONObject(0)
                        .getJSONObject("content")
                        .getJSONArray("parts")
                        .getJSONObject(0)
                        .getString("text")

                    val cleanText = text
                        .replace("```json", "")
                        .replace("```", "")
                        .trim()

                    runOnUiThread {
                        loaderImg.clearAnimation()

                        val intent = Intent(this@AiProcessingActivity, ResultActivity::class.java)
                        intent.putExtra("RESULT", cleanText)
                        intent.putExtra("AGE", age)
                        intent.putExtra("DURATION", duration)
                        intent.putStringArrayListExtra("SYMPTOMS", ArrayList(selectedSymptoms))
                        intent.putExtra("EXTRA", extraInfo)

                        startActivity(intent)
                        finish()
                    }

                } catch (e: Exception) {
                    runOnUiThread {
                        loaderImg.clearAnimation()
                        Toast.makeText(this@AiProcessingActivity, "Parse Error", Toast.LENGTH_LONG).show()
                    }
                }
            }
        })
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacks(runnable)
    }
}