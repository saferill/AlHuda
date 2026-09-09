package com.example.alhuda.main.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.PowerManager
import android.util.Log
import com.example.alhuda.core.domain.repository.AppSettingsRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class AlarmReceiver : BroadcastReceiver() {

    @Inject
    lateinit var appSettingsRepository: AppSettingsRepository

    companion object {
        const val EXTRA_PRAYER_NAME = "extra_prayer_name"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val prayerName = intent.getStringExtra(EXTRA_PRAYER_NAME) ?: "Sholat"
        Log.d("AlarmReceiver", "AlarmReceiver ON_RECEIVE dipanggil untuk $prayerName!")

        val pendingResult = goAsync()

        CoroutineScope(Dispatchers.IO).launch {
            try {
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

                // 2. Ambil URI suara adzan dari pengaturan
                val settings = try {
                    appSettingsRepository.getSettings().firstOrNull()
                } catch (e: Exception) {
                    null
                }
                val adhanSoundUri = settings?.adhanSoundUri

                // 3. Putar suara adzan (menggunakan custom URI jika ada, fallback ke default)
                AdhanAudioPlayer.play(context, prayerName, adhanSoundUri)

                // 4. Tampilkan notifikasi dengan Full Screen Intent
                NotificationHelper.showAdhanNotification(context, prayerName)

                // 5. Panggil AdhanAlarmActivity langsung agar tampil di atas lock screen seketika
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
            } finally {
                pendingResult.finish()
            }
        }
    }
}
