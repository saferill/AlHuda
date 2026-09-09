package com.example.alhuda.main.upcoming_alarms

import java.time.LocalDate

sealed interface UpcomingAlarmsUiAction {
    data class TogglePrayerEnabled(val prayerName: String) : UpcomingAlarmsUiAction
    data class ToggleSkipOccurrence(val prayerName: String, val date: LocalDate) : UpcomingAlarmsUiAction
    object Refresh : UpcomingAlarmsUiAction
}
