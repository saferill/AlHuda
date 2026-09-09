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
        loadUpcomingAlarms(_uiState.value.viewingDate)
        observeSettingsChanges()
    }

    private fun observeSettingsChanges() {
        viewModelScope.launch {
            appSettingsRepository.getSettings().collect {
                loadUpcomingAlarms(_uiState.value.viewingDate)
            }
        }
    }

    fun onAction(action: UpcomingAlarmsUiAction) {
        when (action) {
            is UpcomingAlarmsUiAction.ChangeViewingDate -> {
                _uiState.update { it.copy(viewingDate = action.date) }
                loadUpcomingAlarms(action.date)
            }
            is UpcomingAlarmsUiAction.ToggleDateOverride -> {
                toggleDateOverride(action.prayerName, action.date, action.isEnabled)
            }
            is UpcomingAlarmsUiAction.Refresh -> {
                loadUpcomingAlarms(_uiState.value.viewingDate)
            }
        }
    }

    fun loadUpcomingAlarms(date: LocalDate = _uiState.value.viewingDate) {
        viewModelScope.launch {
            try {
                appSettingsRepository.cleanExpiredDateOverrides()
                val selectedLoc = favoriteLocationsRepository.getSelectedLocation()
                val locationLabel = selectedLoc?.label ?: "Lokasi Tersimpan"

                val list = getUpcomingAlarmsUseCase(date)

                _uiState.update {
                    it.copy(
                        viewingDate = date,
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
                        errorMessage = e.localizedMessage ?: "Gagal memuat jadwal & alarm"
                    )
                }
            }
        }
    }

    private fun toggleDateOverride(prayerName: String, date: LocalDate, isEnabled: Boolean) {
        viewModelScope.launch {
            appSettingsRepository.setDateOverride(prayerName, date, isEnabled)
            schedulerReconciler.reconcileAll()
        }
    }
}
