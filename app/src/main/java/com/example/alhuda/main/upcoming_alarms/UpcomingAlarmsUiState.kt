package com.example.alhuda.main.upcoming_alarms

import com.example.alhuda.core.domain.model.UpcomingAlarmItem
import java.time.LocalDate

data class UpcomingAlarmsUiState(
    val viewingDate: LocalDate = LocalDate.now(),
    val alarms: List<UpcomingAlarmItem> = emptyList(),
    val locationName: String = "",
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)
