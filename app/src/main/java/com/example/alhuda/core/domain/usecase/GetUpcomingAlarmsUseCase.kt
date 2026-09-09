package com.example.alhuda.core.domain.usecase

import com.example.alhuda.core.domain.model.LocationCoordinates
import com.example.alhuda.core.domain.model.UpcomingAlarmItem
import com.example.alhuda.core.domain.repository.AppSettingsRepository
import com.example.alhuda.core.domain.repository.FavoriteLocationsRepository
import com.example.alhuda.core.domain.repository.PrayerTimeRepository
import kotlinx.coroutines.flow.firstOrNull
import java.time.LocalDate
import java.time.LocalDateTime
import javax.inject.Inject

class GetUpcomingAlarmsUseCase @Inject constructor(
    private val prayerTimeRepository: PrayerTimeRepository,
    private val favoriteLocationsRepository: FavoriteLocationsRepository,
    private val appSettingsRepository: AppSettingsRepository
) {

    suspend operator fun invoke(): List<UpcomingAlarmItem> {
        val selectedLocation = favoriteLocationsRepository.getSelectedLocation()
        val coordinates = if (selectedLocation != null) {
            LocationCoordinates(selectedLocation.latitude, selectedLocation.longitude)
        } else {
            prayerTimeRepository.getLastLocation()
        } ?: return emptyList()

        val settings = appSettingsRepository.getSettings().firstOrNull()
        val enabledPrayers = settings?.enabledPrayers ?: setOf("Subuh", "Dzuhur", "Ashar", "Maghrib", "Isya")
        val skippedOccurrences = settings?.skippedOccurrences ?: emptySet()

        val today = LocalDate.now()
        val tomorrow = today.plusDays(1)
        val now = LocalDateTime.now()

        val todayTimes = prayerTimeRepository.getPrayerTimesForDate(coordinates, today)
        val tomorrowTimes = prayerTimeRepository.getPrayerTimesForDate(coordinates, tomorrow)

        val allTimes = todayTimes + tomorrowTimes

        return allTimes
            .filter { it.time.isAfter(now) }
            .map { pt ->
                val isEnabled = enabledPrayers.contains(pt.name)
                val isSkipped = skippedOccurrences.any { skip ->
                    skip.prayerName.equals(pt.name, ignoreCase = true) &&
                    skip.date == pt.time.toLocalDate()
                }

                UpcomingAlarmItem(
                    prayerName = pt.name,
                    dateTime = pt.time,
                    isEnabled = isEnabled,
                    isSkipped = isSkipped
                )
            }
            .sortedBy { it.dateTime }
    }
}
