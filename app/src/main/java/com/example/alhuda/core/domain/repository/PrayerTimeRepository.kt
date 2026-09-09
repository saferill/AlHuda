package com.example.alhuda.core.domain.repository

import com.example.alhuda.core.domain.model.LocationCoordinates
import com.example.alhuda.core.domain.model.PrayerTime

interface PrayerTimeRepository {
    suspend fun getTodayPrayerTimes(location: LocationCoordinates): List<PrayerTime>
    suspend fun saveLastLocation(location: LocationCoordinates)
    suspend fun getLastLocation(): LocationCoordinates?
}
