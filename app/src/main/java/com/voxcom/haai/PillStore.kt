package com.voxcom.haai

import android.content.Context
import android.content.SharedPreferences
import org.json.JSONArray

object PillStore {
    private const val PREFS = "pill_tracker"
    private const val KEY_MEDS = "medicines"
    private const val KEY_LOGS = "dose_logs"
    private const val MAX_LOGS = 1000
    private val lock = Any()

    private fun prefs(c: Context): SharedPreferences =
        c.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    private fun readMeds(c: Context): List<Medicine> {
        val raw = prefs(c).getString(KEY_MEDS, null) ?: return emptyList()
        return try {
            val arr = JSONArray(raw)
            List(arr.length()) { Medicine.fromJson(arr.getJSONObject(it)) }
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun writeMeds(c: Context, list: List<Medicine>) {
        prefs(c).edit().putString(KEY_MEDS, JSONArray(list.map { it.toJson() }).toString()).commit()
    }

    private fun readLogs(c: Context): List<DoseLog> {
        val raw = prefs(c).getString(KEY_LOGS, null) ?: return emptyList()
        return try {
            val arr = JSONArray(raw)
            List(arr.length()) { DoseLog.fromJson(arr.getJSONObject(it)) }
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun writeLogs(c: Context, list: List<DoseLog>) {
        prefs(c).edit().putString(KEY_LOGS, JSONArray(list.map { it.toJson() }).toString()).commit()
    }

    fun getMedicines(c: Context): List<Medicine> = synchronized(lock) { readMeds(c) }

    fun getMedicine(c: Context, id: String): Medicine? = getMedicines(c).firstOrNull { it.id == id }

    fun upsert(c: Context, m: Medicine) = synchronized(lock) {
        val list = readMeds(c).toMutableList()
        val i = list.indexOfFirst { it.id == m.id }
        if (i >= 0) list[i] = m else list.add(m)
        writeMeds(c, list)
    }

    fun delete(c: Context, id: String) = synchronized(lock) {
        writeMeds(c, readMeds(c).filter { it.id != id })
        writeLogs(c, readLogs(c).filter { it.medicineId != id })
    }

    fun getLogs(c: Context): List<DoseLog> = synchronized(lock) { readLogs(c) }

    fun isLogged(c: Context, medicineId: String, scheduledAt: Long): Boolean =
        getLogs(c).any { it.medicineId == medicineId && it.scheduledAt == scheduledAt }

    fun logDose(c: Context, medicineId: String, scheduledAt: Long, status: DoseStatus) = synchronized(lock) {
        val list = readLogs(c)
            .filterNot { it.medicineId == medicineId && it.scheduledAt == scheduledAt }
            .toMutableList()
        list.add(DoseLog(medicineId, scheduledAt, status, System.currentTimeMillis()))
        writeLogs(c, list.takeLast(MAX_LOGS))
    }

    fun clearAll(c: Context) = synchronized(lock) {
        prefs(c).edit().remove(KEY_MEDS).remove(KEY_LOGS).commit()
    }
}
