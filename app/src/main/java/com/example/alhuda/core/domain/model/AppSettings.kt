package com.example.alhuda.core.domain.model

data class AppSettings(
    val calculationMethod: AppCalculationMethod = AppCalculationMethod.KEMENAG_INDONESIA,
    val adhanSoundUri: String? = null,
    val adhanSoundName: String? = null,
    val enabledPrayers: Set<String> = setOf("Subuh", "Dzuhur", "Ashar", "Maghrib", "Isya"),
    val skippedOccurrences: Set<SkippedOccurrence> = emptySet()
)
