package com.example.alhuda.main.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.PowerManager
import android.util.Log
import com.example.alhuda.core.domain.model.isPrayerActiveOnDate
import com.example.alhuda.core.domain.repository.AppSettingsRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

@AndroidEntryPoint
class AlarmReceiver : BroadcastReceiver() {

    @Inject
    lateinit var appSettingsRepository: AppSettingsRepository

    companion object {
        const val EXTRA_PRAYER_NAME = "extra_prayer_name"
        private const val TAG = "AlarmReceiver"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val prayerName = intent.getStringExtra(EXTRA_PRAYER_NAME) ?: "Sholat"
        Log.d(TAG, "AlarmReceiver ON_RECEIVE dipanggil untuk $prayerName")

        val pendingResult = goAsync()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                // 1. Ambil pengaturan aplikasi
                val settings = try {
                    appSettingsRepository.getSettings().firstOrNull()
                } catch (e: Exception) {
                    null
                }

                val today = LocalDate.now()
                val enabledPrayers = settings?.enabledPrayers ?: setOf("Subuh", "Dzuhur", "Ashar", "Maghrib", "Isya")
                val dateOverrides = settings?.dateOverrides ?: emptyMap()

                // Cek status aktif menggunakan isPrayerActiveOnDate (override tanggal dulu, fallback ke global)
                val isActive = isPrayerActiveOnDate(
                    prayer = prayerName,
                    date = today,
                    enabledPrayers = enabledPrayers,
                    dateOverrides = dateOverrides
                )

                Log.d(TAG, "Cek status $prayerName pada $today: isActive=$isActive (overrides=$dateOverrides, global=$enabledPrayers)")

                if (!isActive) {
                    Log.d(TAG, "Alarm untuk $prayerName pada tanggal $today dinonaktifkan. Trigger dibatalkan.")
                    return@launch
                }

                // 2. Bangunkan layar & CPU menggunakan WakeLock
                val powerManager = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
                @Suppress("DEPRECATION")
                val wakeLock = powerManager?.newWakeLock(
                    PowerManager.FULL_WAKE_LOCK or
                    PowerManager.ACQUIRE_CAUSES_WAKEUP or
                    PowerManager.ON_AFTER_RELEASE,
                    "AlHuda:AdhanAlarmWakeLock"
                )
                wakeLock?.acquire(30000) // Tahan wake lock selama 30 detik

                // 3. Putar suara adzan
                val adhanSoundUri = settings?.adhanSoundUri
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
                    Log.d(TAG, "startActivity AdhanAlarmActivity berhasil dieksekusi.")
                } catch (e: Exception) {
                    Log.e(TAG, "Gagal memanggil startActivity: ${e.message}")
                }
            } finally {
                pendingResult.finish()
            }
        }
    }
}
