package com.example.alhuda.core.domain.usecase

import com.example.alhuda.core.domain.model.LocationCoordinates
import com.example.alhuda.core.domain.model.UpcomingAlarmItem
import com.example.alhuda.core.domain.model.isPrayerActiveOnDate
import com.example.alhuda.core.domain.repository.AppSettingsRepository
import com.example.alhuda.core.domain.repository.FavoriteLocationsRepository
import com.example.alhuda.core.domain.repository.PrayerTimeRepository
import kotlinx.coroutines.flow.firstOrNull
import java.time.LocalDate
import javax.inject.Inject

class GetUpcomingAlarmsUseCase @Inject constructor(
    private val prayerTimeRepository: PrayerTimeRepository,
    private val favoriteLocationsRepository: FavoriteLocationsRepository,
    private val appSettingsRepository: AppSettingsRepository
) {

    suspend operator fun invoke(date: LocalDate = LocalDate.now()): List<UpcomingAlarmItem> {
        val selectedLocation = favoriteLocationsRepository.getSelectedLocation()
        val coordinates = if (selectedLocation != null) {
            LocationCoordinates(selectedLocation.latitude, selectedLocation.longitude)
        } else {
            prayerTimeRepository.getLastLocation()
        } ?: return emptyList()

        val settings = appSettingsRepository.getSettings().firstOrNull()
        val enabledPrayers = settings?.enabledPrayers ?: setOf("Subuh", "Dzuhur", "Ashar", "Maghrib", "Isya")
        val dateOverrides = settings?.dateOverrides ?: emptyMap()

        val times = prayerTimeRepository.getPrayerTimesForDate(coordinates, date)
        val today = LocalDate.now()
        val isPastDate = date.isBefore(today)

        return times.map { pt ->
            val isActive = isPrayerActiveOnDate(
                prayer = pt.name,
                date = date,
                enabledPrayers = enabledPrayers,
                dateOverrides = dateOverrides
            )

            UpcomingAlarmItem(
                prayerName = pt.name,
                dateTime = pt.time,
                isEnabled = isActive,
                isPastDate = isPastDate
            )
        }.sortedBy { it.dateTime }
    }
}
