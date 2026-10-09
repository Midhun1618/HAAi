package com.voxcom.haai

import android.content.Context
import java.text.DateFormatSymbols
import java.util.Calendar

enum class DoseState { UPCOMING, DUE, TAKEN, SKIPPED, MISSED }

data class ScheduledDose(
    val medicine: Medicine,
    val time: String,
    val scheduledAt: Long,
    val state: DoseState
)

object DoseSchedule {
    const val MISSED_AFTER_MS = 2 * 60 * 60 * 1000L
    private const val EARLY_WINDOW_MS = 60 * 60 * 1000L

    fun parse(time: String): Pair<Int, Int> {
        val p = time.split(":")
        return p[0].toInt() to p[1].toInt()
    }

    fun display(c: Context, time: String): String {
        val (h, m) = parse(time)
        val cal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, h)
            set(Calendar.MINUTE, m)
        }
        return android.text.format.DateFormat.getTimeFormat(c).format(cal.time)
    }

    /** "HH:mm" for an epoch-millis value, in the device time zone. */
    fun timeOf(millis: Long): String {
        val cal = Calendar.getInstance().apply { timeInMillis = millis }
        return String.format(java.util.Locale.US, "%02d:%02d", cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE))
    }

    fun daysSummary(days: Set<Int>): String {
        if (days.isEmpty() || days.size == 7) return "Every day"
        val names = DateFormatSymbols.getInstance().shortWeekdays
        // Show Monday first
        return listOf(2, 3, 4, 5, 6, 7, 1).filter { it in days }.joinToString(", ") { names[it] }
    }

    /** Next occurrence strictly after [after], or null if none within a week. */
    fun nextOccurrence(m: Medicine, time: String, after: Long): Long? {
        val (h, min) = parse(time)
        val cal = Calendar.getInstance()
        for (offset in 0..7) {
            cal.timeInMillis = after
            cal.add(Calendar.DAY_OF_YEAR, offset)
            cal.set(Calendar.HOUR_OF_DAY, h)
            cal.set(Calendar.MINUTE, min)
            cal.set(Calendar.SECOND, 0)
            cal.set(Calendar.MILLISECOND, 0)
            if (cal.timeInMillis > after &&
                (m.days.isEmpty() || cal.get(Calendar.DAY_OF_WEEK) in m.days)
            ) return cal.timeInMillis
        }
        return null
    }

    /** All doses scheduled on the calendar day containing [dayMillis], with their state as of [now]. */
    fun dosesForDay(c: Context, dayMillis: Long, now: Long): List<ScheduledDose> {
        val meds = PillStore.getMedicines(c)
        val logs = PillStore.getLogs(c)
        val day = Calendar.getInstance().apply {
            timeInMillis = dayMillis
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val dow = day.get(Calendar.DAY_OF_WEEK)
        val result = mutableListOf<ScheduledDose>()
        for (m in meds) {
            if (m.days.isNotEmpty() && dow !in m.days) continue
            for (t in m.times) {
                val (h, min) = parse(t)
                val at = (day.clone() as Calendar).apply {
                    set(Calendar.HOUR_OF_DAY, h)
                    set(Calendar.MINUTE, min)
                }.timeInMillis
                if (at < m.createdAt) continue // don't show doses from before the medicine was added
                val log = logs.firstOrNull { it.medicineId == m.id && it.scheduledAt == at }
                val state = when {
                    log != null -> if (log.status == DoseStatus.TAKEN) DoseState.TAKEN else DoseState.SKIPPED
                    at > now -> DoseState.UPCOMING
                    now - at <= MISSED_AFTER_MS -> DoseState.DUE
                    else -> DoseState.MISSED
                }
                result += ScheduledDose(m, t, at, state)
            }
        }
        return result.sortedBy { it.scheduledAt }
    }

    /** Whether Taken / Skip buttons should be offered for this dose. */
    fun canLog(d: ScheduledDose, now: Long): Boolean = when (d.state) {
        DoseState.DUE, DoseState.MISSED -> true
        DoseState.UPCOMING -> d.scheduledAt - now <= EARLY_WINDOW_MS
        else -> false
    }
}
