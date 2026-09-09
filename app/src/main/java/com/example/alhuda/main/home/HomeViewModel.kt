package com.example.alhuda.main.home

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.alhuda.core.domain.alarm.AlarmScheduler
import com.example.alhuda.core.domain.model.FavoriteLocation
import com.example.alhuda.core.domain.model.LocationCoordinates
import com.example.alhuda.core.domain.model.PrayerTime
import com.example.alhuda.core.domain.repository.FavoriteLocationsRepository
import com.example.alhuda.core.domain.repository.PrayerTimeRepository
import com.example.alhuda.core.util.android.LocationUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val prayerTimeRepository: PrayerTimeRepository,
    private val favoriteLocationsRepository: FavoriteLocationsRepository,
    private val alarmScheduler: AlarmScheduler,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState(isLoading = true))
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        loadPrayerTimes()
    }

    fun loadPrayerTimes() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }

            // 1. Cek lokasi tersimpan dari FavoriteLocationsRepository
            val savedLocation = favoriteLocationsRepository.getSelectedLocation()
            if (savedLocation != null) {
                calculateAndScheduleTimes(
                    coordinates = LocationCoordinates(savedLocation.latitude, savedLocation.longitude),
                    label = savedLocation.label
                )
                return@launch
            }

            // 2. Jika belum ada lokasi tersimpan, coba minta lokasi GPS asli
            val gpsResult = LocationUtils.requestCurrentLocation(context)
            gpsResult.onSuccess { loc ->
                val autoLoc = FavoriteLocation(
                    id = "gps_current",
                    label = "Lokasi GPS Saat Ini",
                    latitude = loc.latitude,
                    longitude = loc.longitude,
                    isSelected = true
                )
                favoriteLocationsRepository.saveLocation(autoLoc)
                calculateAndScheduleTimes(
                    coordinates = LocationCoordinates(loc.latitude, loc.longitude),
                    label = autoLoc.label
                )
            }.onFailure { _ ->
                // Jika permission ditolak atau GPS tidak aktif dan belum ada lokasi
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        needsLocationSelection = true,
                        errorMessage = "Lokasi belum ditentukan. Silakan aktifkan izin lokasi GPS atau pilih lokasi manual."
                    )
                }
            }
        }
    }

    private suspend fun calculateAndScheduleTimes(
        coordinates: LocationCoordinates,
        label: String
    ) {
        try {
            val times = prayerTimeRepository.getTodayPrayerTimes(coordinates)
            // Simpan juga ke prayerTimeRepository untuk boot rescheduling
            prayerTimeRepository.saveLastLocation(coordinates)
            // Jadwalkan alarm waktu sholat
            alarmScheduler.rescheduleAllAlarms(times)

            _uiState.update {
                it.copy(
                    prayerTimes = times,
                    locationName = label,
                    latitude = coordinates.latitude,
                    longitude = coordinates.longitude,
                    isLoading = false,
                    needsLocationSelection = false,
                    errorMessage = null
                )
            }
        } catch (e: Exception) {
            _uiState.update {
                it.copy(
                    isLoading = false,
                    errorMessage = e.localizedMessage ?: "Gagal menghitung waktu sholat"
                )
            }
        }
    }

    /**
     * Helper untuk testing: Menjadwalkan alarm dummy beberapa detik ke depan
     */
    fun testAlarmInSeconds(seconds: Long = 5) {
        val testTime = LocalDateTime.now().plusSeconds(seconds)
        alarmScheduler.scheduleAlarm(
            PrayerTime(
                name = "Test Adzan ($seconds detik)",
                time = testTime
            )
        )
    }
}
