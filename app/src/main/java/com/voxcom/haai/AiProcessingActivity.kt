package com.voxcom.haai

import android.content.Intent
import android.os.Bundle
import android.view.animation.AnimationUtils
import android.widget.ImageView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import okhttp3.*
import org.json.JSONObject
import java.io.IOException
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Callback
import okhttp3.Response
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody

class AiProcessingActivity : AppCompatActivity() {

    private lateinit var loaderImg: ImageView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_ai_processing)

        loaderImg = findViewById(R.id.loaderImageView)

        val rotationAnim = AnimationUtils.loadAnimation(this, R.anim.rotate)
        loaderImg.startAnimation(rotationAnim)

        val prompt = intent.getStringExtra("PROMPT") ?: ""

        callAI(prompt)
    }

    private fun callAI(prompt: String) {
        val client = OkHttpClient()

        val json = """
        {
          "contents": [{
            "parts": [{
              "text": "$prompt"
            }]
          }]
        }
        """

        val request = Request.Builder()
            .url("https://generativelanguage.googleapis.com/v1beta/models/gemini-pro:generateContent?key=AIzaSyDnyAr37mBGS586ny4nsQC8j7rcQumxZCs")
            .post(json.toRequestBody("application/json".toMediaType()))
            .build()

        client.newCall(request).enqueue(object : Callback {

            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread {
                    loaderImg.clearAnimation()
                    finish() // go back if failed
                    println("the requested result failed")
                }
            }

            override fun onResponse(call: Call, response: Response) {
                val body = response.body?.string()

                try {
                    val jsonObject = JSONObject(body!!)
                    val text = jsonObject
                        .getJSONArray("candidates")
                        .getJSONObject(0)
                        .getJSONObject("content")
                        .getJSONArray("parts")
                        .getJSONObject(0)
                        .getString("text")

                    runOnUiThread {
                        loaderImg.clearAnimation()

                        val intent = Intent(this@AiProcessingActivity, ResultActivity::class.java)
                        intent.putExtra("RESULT", text)
                        startActivity(intent)
                        finish()
                    }

                } catch (e: Exception) {
                    runOnUiThread {
                        loaderImg.clearAnimation()

                        finish()
                    }
                }
            }
        })
    }
}