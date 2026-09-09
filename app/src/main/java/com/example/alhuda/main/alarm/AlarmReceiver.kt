package com.example.alhuda.main.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class AlarmReceiver : BroadcastReceiver() {

    companion object {
        const val EXTRA_PRAYER_NAME = "extra_prayer_name"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val prayerName = intent.getStringExtra(EXTRA_PRAYER_NAME) ?: "Sholat"

        // 1. Putar suara adzan looping sampai user mematikan / menunda
        AdhanAudioPlayer.play(context)

        // 2. Munculkan full screen intent ke AdhanAlarmActivity dan notifikasi fallback
        NotificationHelper.showAdhanNotification(context, prayerName)
    }
}
