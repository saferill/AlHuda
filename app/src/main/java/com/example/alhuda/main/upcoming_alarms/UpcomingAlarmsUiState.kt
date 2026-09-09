package com.example.alhuda.main.upcoming_alarms

import com.example.alhuda.core.domain.model.UpcomingAlarmItem

data class UpcomingAlarmsUiState(
    val alarms: List<UpcomingAlarmItem> = emptyList(),
    val locationName: String = "",
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)
