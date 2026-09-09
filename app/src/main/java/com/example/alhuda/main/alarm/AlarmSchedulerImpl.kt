package com.example.alhuda.main.alarm

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.example.alhuda.core.domain.alarm.AlarmScheduler
import com.example.alhuda.core.domain.model.PrayerTime
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.LocalDateTime
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AlarmSchedulerImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : AlarmScheduler {

    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager

    override fun canScheduleExactAlarms(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            alarmManager?.canScheduleExactAlarms() ?: false
        } else {
            true
        }
    }

    override fun scheduleAlarm(prayerTime: PrayerTime): Boolean {
        if (!canScheduleExactAlarms()) {
            Log.w("AlarmScheduler", "Tidak dapat menjadwalkan exact alarm: Izin SCHEDULE_EXACT_ALARM belum di-grant (Android 12+)")
            return false
        }

        var targetTime: LocalDateTime = prayerTime.time
        var triggerEpochMillis = targetTime
            .atZone(ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli()

        // Jika waktu sholat hari ini sudah lewat, jadwalkan untuk besok di jam yang sama (+1 hari)
        if (triggerEpochMillis <= System.currentTimeMillis()) {
            targetTime = targetTime.plusDays(1)
            triggerEpochMillis = targetTime
                .atZone(ZoneId.systemDefault())
                .toInstant()
                .toEpochMilli()
            Log.d("AlarmScheduler", "Waktu ${prayerTime.name} hari ini telah lewat. Dijadwalkan untuk besok: $targetTime")
        }

        val intent = Intent(context, AlarmReceiver::class.java).apply {
            putExtra(AlarmReceiver.EXTRA_PRAYER_NAME, prayerTime.name)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            prayerTime.name.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return try {
            alarmManager?.let { manager ->
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    manager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        triggerEpochMillis,
                        pendingIntent
                    )
                } else {
                    manager.setExact(
                        AlarmManager.RTC_WAKEUP,
                        triggerEpochMillis,
                        pendingIntent
                    )
                }
                Log.d("AlarmScheduler", "Alarm BERHASIL dijadwalkan untuk ${prayerTime.name} pada $targetTime (Epoch: $triggerEpochMillis)")
                true
            } ?: false
        } catch (e: SecurityException) {
            Log.e("AlarmScheduler", "SecurityException saat scheduleAlarm: ${e.message}")
            false
        } catch (e: Exception) {
            Log.e("AlarmScheduler", "Gagal menjadwalkan alarm: ${e.message}")
            false
        }
    }

    override fun cancelAlarm(prayerName: String) {
        val intent = Intent(context, AlarmReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            prayerName.hashCode(),
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )

        if (pendingIntent != null && alarmManager != null) {
            alarmManager.cancel(pendingIntent)
            pendingIntent.cancel()
            Log.d("AlarmScheduler", "Alarm dibatalkan untuk $prayerName")
        }
    }

    override fun rescheduleAllAlarms(prayerTimes: List<PrayerTime>): Boolean {
        if (!canScheduleExactAlarms()) {
            Log.w("AlarmScheduler", "rescheduleAllAlarms dibatalkan karena izin exact alarm belum aktif")
            return false
        }

        var allSuccess = true
        prayerTimes.forEach { prayerTime ->
            val success = scheduleAlarm(prayerTime)
            if (!success) allSuccess = false
        }
        return allSuccess
    }
}
