package com.example.alhuda.main.upcoming_alarms

import java.time.LocalDate

sealed interface UpcomingAlarmsUiAction {
    data class ChangeViewingDate(val date: LocalDate) : UpcomingAlarmsUiAction
    data class ToggleDateOverride(val prayerName: String, val date: LocalDate, val isEnabled: Boolean) : UpcomingAlarmsUiAction
    object Refresh : UpcomingAlarmsUiAction
}
