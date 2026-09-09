package com.example.alhuda.core.domain.model

data class AppSettings(
    val calculationMethod: AppCalculationMethod = AppCalculationMethod.KEMENAG_INDONESIA,
    val adhanSoundUri: String? = null,
    val adhanSoundName: String? = null
)
