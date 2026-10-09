package com.voxcom.haai

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val medId = intent.getStringExtra(ReminderScheduler.EXTRA_MED_ID) ?: return
        val scheduledAt = intent.getLongExtra(ReminderScheduler.EXTRA_SCHEDULED_AT, 0L)
        val time = intent.getStringExtra(ReminderScheduler.EXTRA_TIME).orEmpty()
        val med = PillStore.getMedicine(context, medId) ?: return // medicine was deleted

        when (intent.action) {
            ReminderScheduler.ACTION_FIRE -> {
                val snooze = intent.getBooleanExtra(ReminderScheduler.EXTRA_SNOOZE, false)
                if (!snooze && time !in med.times) return // stale alarm from before an edit
                if (!PillStore.isLogged(context, medId, scheduledAt)) {
                    NotificationHelper.show(context, med, scheduledAt)
                }
                if (!snooze) {
                    ReminderScheduler.scheduleNext(
                        context, med, time, maxOf(System.currentTimeMillis(), scheduledAt)
                    )
                }
            }
            ReminderScheduler.ACTION_TAKEN -> {
                PillStore.logDose(context, medId, scheduledAt, DoseStatus.TAKEN)
                NotificationHelper.cancel(context, medId, scheduledAt)
            }
            ReminderScheduler.ACTION_SKIP -> {
                PillStore.logDose(context, medId, scheduledAt, DoseStatus.SKIPPED)
                NotificationHelper.cancel(context, medId, scheduledAt)
            }
            ReminderScheduler.ACTION_SNOOZE -> {
                NotificationHelper.cancel(context, medId, scheduledAt)
                ReminderScheduler.snooze(context, med, scheduledAt)
            }
        }
    }
}
