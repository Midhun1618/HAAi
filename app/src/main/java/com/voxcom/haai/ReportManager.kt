package com.voxcom.haai

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

object ReportManager {

    private const val PREF_NAME = "reports_pref"
    private const val KEY_REPORTS = "reports_list"

    fun saveReport(context: Context, report: JSONObject) {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)

        val existing = prefs.getString(KEY_REPORTS, "[]")
        val jsonArray = JSONArray(existing)

        jsonArray.put(report)

        prefs.edit().putString(KEY_REPORTS, jsonArray.toString()).apply()
    }

    fun getReports(context: Context): JSONArray {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        val data = prefs.getString(KEY_REPORTS, "[]")
        return JSONArray(data)
    }
}