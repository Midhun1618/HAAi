package com.voxcom.haai

import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import org.json.JSONArray
import org.json.JSONObject

class ReportsActivity : AppCompatActivity() {

    // 🔥 Track last opened item
    private var lastExpanded: LinearLayout? = null
    private var lastImage: ImageView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_reports)

        val container = findViewById<LinearLayout>(R.id.reportsContainer)
        val reports = ReportManager.getReports(this)

        container.removeAllViews()

        if (reports.length() == 0) {
            val emptyTv = TextView(this)
            emptyTv.text = "No reports yet"
            emptyTv.textSize = 18f
            emptyTv.setPadding(20, 40, 20, 20)
            container.addView(emptyTv)
            return
        }

        for (i in reports.length() - 1 downTo 0) {

            val report = reports.getJSONObject(i)
            val item = layoutInflater.inflate(R.layout.item_report, container, false)

            // 🔹 Top views
            val diseaseTv = item.findViewById<TextView>(R.id.itemDisease)
            val dateTv = item.findViewById<TextView>(R.id.itemDate)
            val confidenceTv = item.findViewById<TextView>(R.id.itemConfidence)
            val reportImage = item.findViewById<ImageView>(R.id.reportImage)

            // 🔹 Expand layout
            val expandLayout = item.findViewById<LinearLayout>(R.id.expandLayout)

            // 🔹 Detail views
            val ageTv = item.findViewById<TextView>(R.id.ageTv)
            val durationTv = item.findViewById<TextView>(R.id.durationTv)

            val symptomsLv = item.findViewById<LinearLayout>(R.id.symptomsLv)
            val causeLv = item.findViewById<LinearLayout>(R.id.causeLv)
            val actionsLv = item.findViewById<LinearLayout>(R.id.actionsLv)

            // 🔥 SET BASIC DATA (safe)
            diseaseTv.text = report.optString("disease", "Unknown")
            dateTv.text = report.optString("date", "--")
            confidenceTv.text = "Confidence: ${report.optString("confidence", "--")}"

            ageTv.text = "Age: ${report.optString("age", "--")}"
            durationTv.text = "Duration: ${report.optString("duration", "--")} days"

            // 🔥 CLEAR OLD VIEWS
            symptomsLv.removeAllViews()
            causeLv.removeAllViews()
            actionsLv.removeAllViews()

            // 🔥 SAFE ARRAY FETCH FUNCTION
            fun getSafeArray(obj: JSONObject, key: String): JSONArray {
                return try {
                    val data = obj.get(key)
                    when (data) {
                        is JSONArray -> data
                        is String -> JSONArray(data)
                        else -> JSONArray()
                    }
                } catch (e: Exception) {
                    JSONArray()
                }
            }

            // 🔹 Symptoms
            val symArr = getSafeArray(report, "symptoms")
            for (j in 0 until symArr.length()) {
                val tv = TextView(this)
                tv.text = "• ${symArr.getString(j)}"
                tv.setPadding(4, 4, 4, 4)
                symptomsLv.addView(tv)
            }

            // 🔹 Causes
            val causeArr = getSafeArray(report, "causes")
            for (j in 0 until causeArr.length()) {
                val tv = TextView(this)
                tv.text = "• ${causeArr.getString(j)}"
                tv.setPadding(4, 4, 4, 4)
                causeLv.addView(tv)
            }

            // 🔹 Actions
            val actArr = getSafeArray(report, "actions")
            for (j in 0 until actArr.length()) {
                val tv = TextView(this)
                tv.text = "• ${actArr.getString(j)}"
                tv.setPadding(4, 4, 4, 4)
                actionsLv.addView(tv)
            }

            // 🔥 ACCORDION CLICK LOGIC (clean + stable)
            item.setOnClickListener {

                // 👉 Same item clicked
                if (expandLayout == lastExpanded) {

                    if (expandLayout.visibility == View.VISIBLE) {
                        expandLayout.visibility = View.GONE
                        reportImage.visibility = View.VISIBLE
                        lastExpanded = null
                        lastImage = null
                    } else {
                        expandLayout.visibility = View.VISIBLE
                        reportImage.visibility = View.GONE
                    }

                    return@setOnClickListener
                }

                // 👉 Close previous
                lastExpanded?.visibility = View.GONE
                lastImage?.visibility = View.VISIBLE

                // 👉 Open new
                expandLayout.visibility = View.VISIBLE
                reportImage.visibility = View.GONE

                // 👉 Update tracker
                lastExpanded = expandLayout
                lastImage = reportImage
            }

            container.addView(item)
        }
    }
}