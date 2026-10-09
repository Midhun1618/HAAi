package com.voxcom.haai

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat

object NotificationHelper {
    const val CHANNEL_ID = "pill_reminders"

    fun ensureChannel(c: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val nm = c.getSystemService(NotificationManager::class.java)
            if (nm.getNotificationChannel(CHANNEL_ID) == null) {
                val ch = NotificationChannel(
                    CHANNEL_ID,
                    c.getString(R.string.pill_channel_name),
                    NotificationManager.IMPORTANCE_HIGH
                )
                ch.description = c.getString(R.string.pill_channel_desc)
                nm.createNotificationChannel(ch)
            }
        }
    }

    private fun notifId(medId: String, scheduledAt: Long) = (medId + scheduledAt).hashCode()

    fun cancel(c: Context, medId: String, scheduledAt: Long) {
        NotificationManagerCompat.from(c).cancel(notifId(medId, scheduledAt))
    }

    private fun actionIntent(c: Context, action: String, med: Medicine, scheduledAt: Long): PendingIntent {
        val i = Intent(c, ReminderReceiver::class.java)
            .setAction(action)
            .setData(Uri.parse("pill://$action/${med.id}/$scheduledAt"))
            .putExtra(ReminderScheduler.EXTRA_MED_ID, med.id)
            .putExtra(ReminderScheduler.EXTRA_SCHEDULED_AT, scheduledAt)
        return PendingIntent.getBroadcast(
            c, 0, i, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    @SuppressLint("MissingPermission")
    fun show(c: Context, med: Medicine, scheduledAt: Long) {
        val nmc = NotificationManagerCompat.from(c)
        if (!nmc.areNotificationsEnabled()) return // also false when POST_NOTIFICATIONS is not granted
        ensureChannel(c)

        val open = PendingIntent.getActivity(
            c, 0,
            Intent(c, PillTrackerActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val timeText = DoseSchedule.display(c, DoseSchedule.timeOf(scheduledAt))
        val body = listOf(med.dose, timeText).filter { it.isNotBlank() }.joinToString(" · ")

        val n = NotificationCompat.Builder(c, CHANNEL_ID)
            .setSmallIcon(R.drawable.icon_pill)
            .setContentTitle(c.getString(R.string.pill_notif_title, med.name))
            .setContentText(body)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setAutoCancel(true)
            .setContentIntent(open)
            .addAction(0, c.getString(R.string.pill_action_taken), actionIntent(c, ReminderScheduler.ACTION_TAKEN, med, scheduledAt))
            .addAction(0, c.getString(R.string.pill_action_skip), actionIntent(c, ReminderScheduler.ACTION_SKIP, med, scheduledAt))
            .addAction(0, c.getString(R.string.pill_action_snooze), actionIntent(c, ReminderScheduler.ACTION_SNOOZE, med, scheduledAt))
            .build()
        nmc.notify(notifId(med.id, scheduledAt), n)
    }
}
