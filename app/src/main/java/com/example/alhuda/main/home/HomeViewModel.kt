package com.example.alhuda.main.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.alhuda.core.domain.alarm.AlarmScheduler
import com.example.alhuda.core.domain.model.LocationCoordinates
import com.example.alhuda.core.domain.model.PrayerTime
import com.example.alhuda.core.domain.repository.PrayerTimeRepository
import dagger.hilt.android.lifecycle.HiltViewModel
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
    private val alarmScheduler: AlarmScheduler
) : ViewModel() {

    companion object {
        // Hardcoded default coordinates (Jakarta)
        val DEFAULT_LOCATION = LocationCoordinates(
            latitude = -6.2088,
            longitude = 106.8456
        )
    }

    private val _uiState = MutableStateFlow(HomeUiState(isLoading = true))
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        loadPrayerTimes()
    }

    fun loadPrayerTimes() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                val savedLocation = prayerTimeRepository.getLastLocation()
                val targetLocation = savedLocation ?: DEFAULT_LOCATION
                val times = prayerTimeRepository.getTodayPrayerTimes(targetLocation)

                // Jadwalkan semua alarm sholat hari ini
                alarmScheduler.rescheduleAllAlarms(times)

                _uiState.update {
                    it.copy(
                        prayerTimes = times,
                        isLoading = false,
                        errorMessage = null
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = e.localizedMessage ?: "Terjadi kesalahan memuat waktu sholat"
                    )
                }
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
