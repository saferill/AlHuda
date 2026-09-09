package com.example.alhuda.core.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import com.example.alhuda.core.domain.model.AppCalculationMethod
import com.example.alhuda.core.domain.model.LocationCoordinates
import com.example.alhuda.core.domain.model.PrayerTime
import com.example.alhuda.core.domain.repository.AppSettingsRepository
import com.example.alhuda.core.domain.repository.PrayerTimeRepository
import io.github.meypod.adhan_kotlin.CalculationMethod
import io.github.meypod.adhan_kotlin.CalculationParameters
import io.github.meypod.adhan_kotlin.Coordinates
import io.github.meypod.adhan_kotlin.PrayerTimes
import io.github.meypod.adhan_kotlin.data.DateComponents
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.Date
class PrayerTimeRepositoryImpl(
    private val dataStore: DataStore<Preferences>,
    private val appSettingsRepository: AppSettingsRepository
) : PrayerTimeRepository {

    companion object {
        private val KEY_LATITUDE = doublePreferencesKey("latitude")
        private val KEY_LONGITUDE = doublePreferencesKey("longitude")
    }

    override suspend fun getTodayPrayerTimes(location: LocationCoordinates): List<PrayerTime> {
        return getPrayerTimesForDate(location, LocalDate.now())
    }

    @OptIn(kotlin.time.ExperimentalTime::class)
    override suspend fun getPrayerTimesForDate(
        location: LocationCoordinates,
        date: LocalDate
    ): List<PrayerTime> {
        val settings = appSettingsRepository.getSettings().firstOrNull()
        val appMethod = settings?.calculationMethod ?: AppCalculationMethod.KEMENAG_INDONESIA

        val calculationParameters = getCalculationParameters(appMethod)

        val coordinates = Coordinates(location.latitude, location.longitude)
        val dateComponents = DateComponents(date.year, date.monthValue, date.dayOfMonth)

        val prayerTimes = PrayerTimes(coordinates, dateComponents, calculationParameters)
        val zoneId = ZoneId.systemDefault()

        fun kotlinx.datetime.Instant.toJavaLocalDateTime(): LocalDateTime =
            java.time.Instant.ofEpochMilli(this.toEpochMilliseconds()).atZone(zoneId).toLocalDateTime()

        return listOf(
            PrayerTime(name = "Subuh", time = prayerTimes.fajr.toJavaLocalDateTime()),
            PrayerTime(name = "Dzuhur", time = prayerTimes.dhuhr.toJavaLocalDateTime()),
            PrayerTime(name = "Ashar", time = prayerTimes.asr.toJavaLocalDateTime()),
            PrayerTime(name = "Maghrib", time = prayerTimes.maghrib.toJavaLocalDateTime()),
            PrayerTime(name = "Isya", time = prayerTimes.isha.toJavaLocalDateTime())
        )
    }

    private fun getCalculationParameters(method: AppCalculationMethod): CalculationParameters {
        return when (method) {
            AppCalculationMethod.KEMENAG_INDONESIA -> CalculationMethod.KEMENAG.parameters
            AppCalculationMethod.MUSLIM_WORLD_LEAGUE -> CalculationMethod.MUSLIM_WORLD_LEAGUE.parameters
            AppCalculationMethod.UMM_AL_QURA -> CalculationMethod.UMM_AL_QURA.parameters
            AppCalculationMethod.EGYPTIAN -> CalculationMethod.EGYPTIAN.parameters
            AppCalculationMethod.KARACHI -> CalculationMethod.KARACHI.parameters
            AppCalculationMethod.SINGAPORE -> CalculationMethod.SINGAPORE.parameters
        }
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
