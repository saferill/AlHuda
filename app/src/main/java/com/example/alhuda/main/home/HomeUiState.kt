package com.example.alhuda.main.home

import com.example.alhuda.core.domain.model.PrayerTime

/**
 * UI State for HomeScreen.
 */
data class HomeUiState(
    val prayerTimes: List<PrayerTime> = emptyList(),
    val locationName: String = "",
    val latitude: Double? = null,
    val longitude: Double? = null,
    val isLoading: Boolean = false,
    val needsLocationSelection: Boolean = false,
    val errorMessage: String? = null
)
