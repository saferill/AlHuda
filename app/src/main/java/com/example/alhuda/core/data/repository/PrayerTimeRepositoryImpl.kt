package com.example.alhuda.core.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import com.batoulapps.adhan.CalculationMethod
import com.batoulapps.adhan.Coordinates
import com.batoulapps.adhan.PrayerTimes
import com.batoulapps.adhan.data.DateComponents
import com.example.alhuda.core.domain.model.LocationCoordinates
import com.example.alhuda.core.domain.model.PrayerTime
import com.example.alhuda.core.domain.repository.PrayerTimeRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.Date
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PrayerTimeRepositoryImpl @Inject constructor(
    private val dataStore: DataStore<Preferences>
) : PrayerTimeRepository {

    companion object {
        private val KEY_LATITUDE = doublePreferencesKey("latitude")
        private val KEY_LONGITUDE = doublePreferencesKey("longitude")
    }

    override suspend fun getTodayPrayerTimes(location: LocationCoordinates): List<PrayerTime> {
        val coordinates = Coordinates(location.latitude, location.longitude)
        val today = LocalDate.now()
        val dateComponents = DateComponents(today.year, today.monthValue, today.dayOfMonth)
        val params = CalculationMethod.MUSLIM_WORLD_LEAGUE.parameters

        val prayerTimes = PrayerTimes(coordinates, dateComponents, params)
        val zoneId = ZoneId.systemDefault()

        fun Date.toLocalDateTime(): LocalDateTime =
            Instant.ofEpochMilli(this.time).atZone(zoneId).toLocalDateTime()

        return listOf(
            PrayerTime(name = "Subuh", time = prayerTimes.fajr.toLocalDateTime()),
            PrayerTime(name = "Dzuhur", time = prayerTimes.dhuhr.toLocalDateTime()),
            PrayerTime(name = "Ashar", time = prayerTimes.asr.toLocalDateTime()),
            PrayerTime(name = "Maghrib", time = prayerTimes.maghrib.toLocalDateTime()),
            PrayerTime(name = "Isya", time = prayerTimes.isha.toLocalDateTime())
        )
    }

    override suspend fun saveLastLocation(location: LocationCoordinates) {
        dataStore.edit { preferences ->
            preferences[KEY_LATITUDE] = location.latitude
            preferences[KEY_LONGITUDE] = location.longitude
        }
    }

    override suspend fun getLastLocation(): LocationCoordinates? {
        return dataStore.data.map { preferences ->
            val lat = preferences[KEY_LATITUDE]
            val lng = preferences[KEY_LONGITUDE]
            if (lat != null && lng != null) {
                LocationCoordinates(latitude = lat, longitude = lng)
            } else {
                null
            }
        }.firstOrNull()
    }
}
