package com.voxcom.haai

import org.json.JSONArray
import org.json.JSONObject

/**
 * times: "HH:mm" in 24h format. days: Calendar.DAY_OF_WEEK values (1 = Sunday ... 7 = Saturday).
 */
data class Medicine(
    val id: String,
    val name: String,
    val dose: String,
    val times: List<String>,
    val days: Set<Int>,
    val createdAt: Long
) {
    fun toJson(): JSONObject = JSONObject().apply {
        put("id", id)
        put("name", name)
        put("dose", dose)
        put("times", JSONArray(times))
        put("days", JSONArray(days.toList()))
        put("createdAt", createdAt)
    }

    companion object {
        fun fromJson(o: JSONObject): Medicine {
            val t = o.getJSONArray("times")
            val d = o.getJSONArray("days")
            return Medicine(
                id = o.getString("id"),
                name = o.getString("name"),
                dose = o.optString("dose", ""),
                times = List(t.length()) { t.getString(it) },
                days = (0 until d.length()).map { d.getInt(it) }.toSet(),
                createdAt = o.optLong("createdAt", 0L)
            )
        }
    }
}

enum class DoseStatus { TAKEN, SKIPPED }

data class DoseLog(
    val medicineId: String,
    val scheduledAt: Long,
    val status: DoseStatus,
    val loggedAt: Long
) {
    fun toJson(): JSONObject = JSONObject().apply {
        put("medicineId", medicineId)
        put("scheduledAt", scheduledAt)
        put("status", status.name)
        put("loggedAt", loggedAt)
    }

    companion object {
        fun fromJson(o: JSONObject) = DoseLog(
            medicineId = o.getString("medicineId"),
            scheduledAt = o.getLong("scheduledAt"),
            status = DoseStatus.valueOf(o.getString("status")),
            loggedAt = o.optLong("loggedAt", 0L)
        )
    }
}
