package com.example.alhuda.main.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.PowerManager
import android.util.Log

class AlarmReceiver : BroadcastReceiver() {

    companion object {
        const val EXTRA_PRAYER_NAME = "extra_prayer_name"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val prayerName = intent.getStringExtra(EXTRA_PRAYER_NAME) ?: "Sholat"
        Log.d("AlarmReceiver", "AlarmReceiver ON_RECEIVE dipanggil untuk $prayerName!")

        // 1. Bangunkan layar & CPU menggunakan WakeLock
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
        @Suppress("DEPRECATION")
        val wakeLock = powerManager?.newWakeLock(
            PowerManager.FULL_WAKE_LOCK or
            PowerManager.ACQUIRE_CAUSES_WAKEUP or
            PowerManager.ON_AFTER_RELEASE,
            "AlHuda:AdhanAlarmWakeLock"
        )
        wakeLock?.acquire(30000) // Tahan wake lock selama 30 detik

        // 2. Putar suara adzan looping sesuai waktu sholat (Subuh vs non-Subuh)
        AdhanAudioPlayer.play(context, prayerName)

        // 3. Tampilkan notifikasi dengan Full Screen Intent
        NotificationHelper.showAdhanNotification(context, prayerName)

        // 4. Panggil AdhanAlarmActivity langsung agar tampil di atas lock screen seketika
        val activityIntent = Intent(context, AdhanAlarmActivity::class.java).apply {
            putExtra(AdhanAlarmActivity.EXTRA_PRAYER_NAME, prayerName)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_CLEAR_TOP or
                    Intent.FLAG_ACTIVITY_SINGLE_TOP
        }

        try {
            context.startActivity(activityIntent)
            Log.d("AlarmReceiver", "startActivity AdhanAlarmActivity berhasil dieksekusi.")
        } catch (e: Exception) {
            Log.e("AlarmReceiver", "Gagal memanggil startActivity: ${e.message}")
        }
    }
}
