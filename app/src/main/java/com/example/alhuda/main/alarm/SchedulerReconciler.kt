package com.example.alhuda.main.alarm

import android.content.Context
import android.util.Log
import com.example.alhuda.core.domain.alarm.AlarmScheduler
import com.example.alhuda.core.domain.model.LocationCoordinates
import com.example.alhuda.core.domain.repository.FavoriteLocationsRepository
import com.example.alhuda.core.domain.repository.PrayerTimeRepository
import com.example.alhuda.core.util.android.LocationUtils
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SchedulerReconciler @Inject constructor(
    private val prayerTimeRepository: PrayerTimeRepository,
    private val favoriteLocationsRepository: FavoriteLocationsRepository,
    private val alarmScheduler: AlarmScheduler,
    @ApplicationContext private val context: Context
) {
    suspend fun reconcileAll(): Boolean = withContext(Dispatchers.IO) {
        try {
            val selectedLocation = favoriteLocationsRepository.getSelectedLocation()
            val coordinates = if (selectedLocation != null) {
                LocationCoordinates(selectedLocation.latitude, selectedLocation.longitude)
            } else {
                val gpsResult = LocationUtils.requestCurrentLocation(context)
                gpsResult.getOrNull()?.let {
                    LocationCoordinates(it.latitude, it.longitude)
                }
            }

            if (coordinates != null) {
                val times = prayerTimeRepository.getTodayPrayerTimes(coordinates)
                val success = alarmScheduler.rescheduleAllAlarms(times)
                Log.d("SchedulerReconciler", "Reconcile selesai. Berhasil: $success, Jumlah jadwal: ${times.size}")
                success
            } else {
                Log.w("SchedulerReconciler", "Reconcile dibatalkan: Lokasi belum tersedia")
                false
            }
        } catch (e: Exception) {
            Log.e("SchedulerReconciler", "Error saat reconcileAll: ${e.message}")
            false
        }
    }
}
