package com.example.alhuda.main.settings

import com.example.alhuda.core.domain.model.AppCalculationMethod

data class SettingsUiState(
    val selectedMethod: AppCalculationMethod = AppCalculationMethod.KEMENAG_INDONESIA,
    val adhanSoundUri: String? = null,
    val adhanSoundName: String? = null,
    val enabledPrayers: Set<String> = setOf("Subuh", "Dzuhur", "Ashar", "Maghrib", "Isya"),
    val isPreviewPlaying: Boolean = false,
    val isLoading: Boolean = false
)
