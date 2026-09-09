package com.example.alhuda.main.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.alhuda.core.domain.alarm.AlarmScheduler
import com.example.alhuda.core.domain.model.LocationCoordinates
import com.example.alhuda.core.domain.repository.PrayerTimeRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

@HiltWorker
class BootRescheduleWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val prayerTimeRepository: PrayerTimeRepository,
    private val alarmScheduler: AlarmScheduler
) : CoroutineWorker(appContext, workerParams) {

    companion object {
        private val DEFAULT_LOCATION = LocationCoordinates(
            latitude = -6.2088,
            longitude = 106.8456
        )
    }

    override suspend fun doWork(): Result {
        return try {
            val location = prayerTimeRepository.getLastLocation() ?: DEFAULT_LOCATION
            val prayerTimes = prayerTimeRepository.getTodayPrayerTimes(location)
            alarmScheduler.rescheduleAllAlarms(prayerTimes)
            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }
}
