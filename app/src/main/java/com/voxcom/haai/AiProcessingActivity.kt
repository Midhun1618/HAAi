package com.voxcom.haai

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.animation.AnimationUtils
import android.widget.ImageView
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_ai_processing)

        loaderImg = findViewById(R.id.loaderImageView)

        val rotationAnim = AnimationUtils.loadAnimation(this, R.anim.rotate)
        loaderImg.startAnimation(rotationAnim)

        val prompt = intent.getStringExtra("PROMPT") ?: ""
        val age = intent.getStringExtra("AGE") ?: ""
        val duration = intent.getStringExtra("DURATION") ?: ""
        val symptoms = intent.getStringArrayListExtra("SYMPTOMS") ?: arrayListOf()
        val extraInfo = intent.getStringExtra("EXTRA") ?: ""

        Log.d("AI_DEBUG", "Prompt received: $prompt")

        if (prompt.isEmpty()) {
            Toast.makeText(this, "No input received", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        callAI(prompt, age, duration, symptoms, extraInfo)
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

        val part = JSONObject()
        part.put("text", prompt)

        val partsArray = JSONArray()
        partsArray.put(part)

        val content = JSONObject()
        content.put("parts", partsArray)

        val contentsArray = JSONArray()
        contentsArray.put(content)

        jsonObject.put("contents", contentsArray)

        val request = Request.Builder()
            .url("https://generativelanguage.googleapis.com/v1beta/models/${Info.model}:generateContent")
            .addHeader("Content-Type", "application/json")
            .addHeader("X-goog-api-key", Info.api)
            .post(jsonObject.toString().toRequestBody("application/json".toMediaType()))
            .build()

        Log.d("AI_DEBUG", "Sending request...")

        client.newCall(request).enqueue(object : Callback {

            override fun onFailure(call: Call, e: IOException) {
                Log.e("AI_ERROR", "Request failed", e)

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

                Log.d("AI_RESPONSE", body)

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

                    Log.d("AI_CLEAN", cleanText)

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
                    Log.e("AI_PARSE_ERROR", "Parsing failed", e)

                    runOnUiThread {
                        loaderImg.clearAnimation()
                        Toast.makeText(this@AiProcessingActivity, "Parse Error", Toast.LENGTH_LONG).show()
                    }
                }
            }
        })
    }
}