package com.example.alhuda.core.domain.usecase

import com.example.alhuda.core.domain.model.LocationCoordinates
import com.example.alhuda.core.domain.model.NextPrayerInfo
import com.example.alhuda.core.domain.model.isPrayerActiveOnDate
import com.example.alhuda.core.domain.repository.AppSettingsRepository
import com.example.alhuda.core.domain.repository.FavoriteLocationsRepository
import com.example.alhuda.core.domain.repository.PrayerTimeRepository
import kotlinx.coroutines.flow.firstOrNull
import java.time.Duration
import java.time.LocalDateTime
import java.time.ZoneId
import javax.inject.Inject

class GetNextPrayerUseCase @Inject constructor(
    private val prayerTimeRepository: PrayerTimeRepository,
    private val favoriteLocationsRepository: FavoriteLocationsRepository,
    private val appSettingsRepository: AppSettingsRepository
) {

    suspend operator fun invoke(
        targetCoordinates: LocationCoordinates? = null,
        nowTime: LocalDateTime = LocalDateTime.now()
    ): NextPrayerInfo? {
        val coordinates = targetCoordinates ?: run {
            val selectedLocation = favoriteLocationsRepository.getSelectedLocation()
            if (selectedLocation != null) {
                LocationCoordinates(selectedLocation.latitude, selectedLocation.longitude)
            } else {
                prayerTimeRepository.getLastLocation()
            }
        } ?: return null

        val settings = appSettingsRepository.getSettings().firstOrNull()
        val enabledPrayers = settings?.enabledPrayers ?: setOf("Subuh", "Dzuhur", "Ashar", "Maghrib", "Isya")
        val dateOverrides = settings?.dateOverrides ?: emptyMap()

        val today = nowTime.toLocalDate()
        val todayTimes = prayerTimeRepository.getPrayerTimesForDate(coordinates, today)
            .sortedBy { it.time }

        // 1. Ambil sholat hari ini yang masih di depan dan berstatus aktif
        val nextToday = todayTimes.firstOrNull { pt ->
            pt.time.isAfter(nowTime) && isPrayerActiveOnDate(
                prayer = pt.name,
                date = today,
                enabledPrayers = enabledPrayers,
                dateOverrides = dateOverrides
            )
        }

        val zoneId = ZoneId.systemDefault()

        if (nextToday != null) {
            val prayerInstant = nextToday.time.atZone(zoneId).toInstant()
            val nowInstant = nowTime.atZone(zoneId).toInstant()
            val remainingSeconds = Duration.between(nowInstant, prayerInstant).seconds.coerceAtLeast(0)
            return NextPrayerInfo(
                prayerName = nextToday.name,
                prayerTime = prayerInstant,
                remainingSeconds = remainingSeconds
            )
        }

        // 2. Jika semua sholat hari ini sudah lewat atau nonaktif, cari untuk besok
        val tomorrow = today.plusDays(1)
        val tomorrowTimes = prayerTimeRepository.getPrayerTimesForDate(coordinates, tomorrow)
            .sortedBy { it.time }

        val nextTomorrow = tomorrowTimes.firstOrNull { pt ->
            isPrayerActiveOnDate(
                prayer = pt.name,
                date = tomorrow,
                enabledPrayers = enabledPrayers,
                dateOverrides = dateOverrides
            )
        }

        if (nextTomorrow != null) {
            val prayerInstant = nextTomorrow.time.atZone(zoneId).toInstant()
            val nowInstant = nowTime.atZone(zoneId).toInstant()
            val remainingSeconds = Duration.between(nowInstant, prayerInstant).seconds.coerceAtLeast(0)
            return NextPrayerInfo(
                prayerName = nextTomorrow.name,
                prayerTime = prayerInstant,
                remainingSeconds = remainingSeconds
            )
        }

        return null
    }
}
