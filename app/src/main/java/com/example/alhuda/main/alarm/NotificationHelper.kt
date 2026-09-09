package com.example.alhuda.main.alarm

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat

object NotificationHelper {

    const val CHANNEL_ID = "adhan_channel"
    private const val CHANNEL_NAME = "Notifikasi Adzan"
    private const val CHANNEL_DESCRIPTION = "Pengingat waktu sholat dan adzan"
    const val NOTIFICATION_ID_BASE = 1000

    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = CHANNEL_DESCRIPTION
                enableVibration(true)
                setSound(null, null) // Suara di-handle oleh AdhanAudioPlayer (MediaPlayer loop)
            }

            val notificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            notificationManager?.createNotificationChannel(channel)
        }
    }

    fun showAdhanNotification(context: Context, prayerName: String) {
        createNotificationChannel(context)

        // Full Screen Intent menuju AdhanAlarmActivity
        val fullScreenIntent = Intent(context, AdhanAlarmActivity::class.java).apply {
            putExtra(AdhanAlarmActivity.EXTRA_PRAYER_NAME, prayerName)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_CLEAR_TOP or
                    Intent.FLAG_ACTIVITY_SINGLE_TOP
        }

        val fullScreenPendingIntent = PendingIntent.getActivity(
            context,
            prayerName.hashCode(),
            fullScreenIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle("Waktunya $prayerName")
            .setContentText("Telah masuk waktu sholat $prayerName. Mari tunaikan sholat.")
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setAutoCancel(false)
            .setOngoing(true)
            .setContentIntent(fullScreenPendingIntent)
            .setFullScreenIntent(fullScreenPendingIntent, true)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .build()

        val notificationManager = NotificationManagerCompat.from(context)
        try {
            notificationManager.notify(getNotificationId(prayerName), notification)
        } catch (e: SecurityException) {
            // Permission POST_NOTIFICATIONS
        }
    }

    fun dismissNotification(context: Context, prayerName: String) {
        val notificationManager = NotificationManagerCompat.from(context)
        notificationManager.cancel(getNotificationId(prayerName))
    }

    fun getNotificationId(prayerName: String): Int =
        NOTIFICATION_ID_BASE + kotlin.math.abs(prayerName.hashCode() % 1000)
}
