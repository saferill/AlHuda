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

    override fun scheduleAlarm(prayerTime: PrayerTime) {
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

        alarmManager?.let { manager ->
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    if (manager.canScheduleExactAlarms()) {
                        manager.setExactAndAllowWhileIdle(
                            AlarmManager.RTC_WAKEUP,
                            triggerEpochMillis,
                            pendingIntent
                        )
                    } else {
                        manager.setAndAllowWhileIdle(
                            AlarmManager.RTC_WAKEUP,
                            triggerEpochMillis,
                            pendingIntent
                        )
                    }
                } else {
                    manager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        triggerEpochMillis,
                        pendingIntent
                    )
                }
                Log.d("AlarmScheduler", "Alarm BERHASIL dijadwalkan untuk ${prayerTime.name} pada $targetTime (Epoch: $triggerEpochMillis)")
            } catch (e: SecurityException) {
                Log.e("AlarmScheduler", "Gagal menjadwalkan exact alarm: ${e.message}")
            }
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

    override fun rescheduleAllAlarms(prayerTimes: List<PrayerTime>) {
        prayerTimes.forEach { prayerTime ->
            scheduleAlarm(prayerTime)
        }
    }
}
