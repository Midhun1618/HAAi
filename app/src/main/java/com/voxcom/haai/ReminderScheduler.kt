package com.voxcom.haai

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build

object ReminderScheduler {
    const val ACTION_FIRE = "com.voxcom.haai.pill.FIRE"
    const val ACTION_TAKEN = "com.voxcom.haai.pill.TAKEN"
    const val ACTION_SKIP = "com.voxcom.haai.pill.SKIP"
    const val ACTION_SNOOZE = "com.voxcom.haai.pill.SNOOZE"

    const val EXTRA_MED_ID = "med_id"
    const val EXTRA_TIME = "time"
    const val EXTRA_SCHEDULED_AT = "scheduled_at"
    const val EXTRA_SNOOZE = "is_snooze"

    const val SNOOZE_MINUTES = 10

    private fun alarmManager(c: Context) = c.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    private fun fireIntent(c: Context, medId: String, time: String, at: Long, snooze: Boolean): PendingIntent {
        val data = if (snooze) "pill://snooze/$medId/$at" else "pill://fire/$medId/$time"
        val i = Intent(c, ReminderReceiver::class.java)
            .setAction(ACTION_FIRE)
            .setData(Uri.parse(data))
            .putExtra(EXTRA_MED_ID, medId)
            .putExtra(EXTRA_TIME, time)
            .putExtra(EXTRA_SCHEDULED_AT, at)
            .putExtra(EXTRA_SNOOZE, snooze)
        return PendingIntent.getBroadcast(
            c, 0, i, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun showIntent(c: Context): PendingIntent = PendingIntent.getActivity(
        c, 0, Intent(c, PillTrackerActivity::class.java), PendingIntent.FLAG_IMMUTABLE
    )

    fun canScheduleExact(c: Context): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.S || alarmManager(c).canScheduleExactAlarms()

    private fun setAlarm(c: Context, at: Long, pi: PendingIntent) {
        val am = alarmManager(c)
        try {
            if (canScheduleExact(c)) {
                am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pi)
            } else {
                am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pi)
            }
        } catch (e: SecurityException) {
            am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pi)
        }
    }

    fun scheduleNext(c: Context, m: Medicine, time: String, after: Long = System.currentTimeMillis()) {
        val at = DoseSchedule.nextOccurrence(m, time, after) ?: return
        setAlarm(c, at, fireIntent(c, m.id, time, at, false))
    }

    fun schedule(c: Context, m: Medicine) {
        m.times.forEach { scheduleNext(c, m, it) }
    }

    fun scheduleAll(c: Context) {
        PillStore.getMedicines(c).forEach { schedule(c, it) }
    }

    fun snooze(c: Context, m: Medicine, scheduledAt: Long) {
        val at = System.currentTimeMillis() + SNOOZE_MINUTES * 60_000L
        setAlarm(c, at, fireIntent(c, m.id, "", scheduledAt, true))
    }

    fun cancel(c: Context, m: Medicine) {
        val am = alarmManager(c)
        m.times.forEach { am.cancel(fireIntent(c, m.id, it, 0L, false)) }
    }

    fun cancelAll(c: Context) {
        PillStore.getMedicines(c).forEach { cancel(c, it) }
    }
}
