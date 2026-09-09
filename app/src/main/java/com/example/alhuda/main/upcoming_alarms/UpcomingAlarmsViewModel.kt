package com.example.alhuda.main.upcoming_alarms

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.alhuda.core.domain.repository.AppSettingsRepository
import com.example.alhuda.core.domain.repository.FavoriteLocationsRepository
import com.example.alhuda.core.domain.usecase.GetUpcomingAlarmsUseCase
import com.example.alhuda.main.alarm.SchedulerReconciler
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

@HiltViewModel
class UpcomingAlarmsViewModel @Inject constructor(
    private val getUpcomingAlarmsUseCase: GetUpcomingAlarmsUseCase,
    private val appSettingsRepository: AppSettingsRepository,
    private val favoriteLocationsRepository: FavoriteLocationsRepository,
    private val schedulerReconciler: SchedulerReconciler
) : ViewModel() {

    private val _uiState = MutableStateFlow(UpcomingAlarmsUiState(isLoading = true))
    val uiState: StateFlow<UpcomingAlarmsUiState> = _uiState.asStateFlow()

    init {
        loadUpcomingAlarms()
        observeSettingsChanges()
    }

    private fun observeSettingsChanges() {
        viewModelScope.launch {
            appSettingsRepository.getSettings().collect {
                loadUpcomingAlarms()
            }
        }
    }

    fun onAction(action: UpcomingAlarmsUiAction) {
        when (action) {
            is UpcomingAlarmsUiAction.TogglePrayerEnabled -> {
                togglePrayerEnabled(action.prayerName)
            }
            is UpcomingAlarmsUiAction.ToggleSkipOccurrence -> {
                toggleSkipOccurrence(action.prayerName, action.date)
            }
            is UpcomingAlarmsUiAction.Refresh -> {
                loadUpcomingAlarms()
            }
        }
    }

    fun loadUpcomingAlarms() {
        viewModelScope.launch {
            try {
                appSettingsRepository.cleanExpiredSkippedOccurrences()
                val selectedLoc = favoriteLocationsRepository.getSelectedLocation()
                val locationLabel = selectedLoc?.label ?: "Lokasi Tersimpan"

                val list = getUpcomingAlarmsUseCase()

                _uiState.update {
                    it.copy(
                        alarms = list,
                        locationName = locationLabel,
                        isLoading = false,
                        errorMessage = null
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = e.localizedMessage ?: "Gagal memuat alarm mendatang"
                    )
                }
            }
        }
    }

    private fun togglePrayerEnabled(prayerName: String) {
        viewModelScope.launch {
            appSettingsRepository.togglePrayerEnabled(prayerName)
            schedulerReconciler.reconcileAll()
        }
    }

    private fun toggleSkipOccurrence(prayerName: String, date: LocalDate) {
        viewModelScope.launch {
            appSettingsRepository.toggleSkipOccurrence(prayerName, date)
            schedulerReconciler.reconcileAll()
        }
    }
}
