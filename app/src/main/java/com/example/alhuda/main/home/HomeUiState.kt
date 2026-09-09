package com.example.alhuda.main.home

import com.example.alhuda.core.domain.model.PrayerTime

/**
 * UI State for HomeScreen.
 */
data class HomeUiState(
    val prayerTimes: List<PrayerTime> = emptyList(),
    val locationName: String = "Jakarta",
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)
