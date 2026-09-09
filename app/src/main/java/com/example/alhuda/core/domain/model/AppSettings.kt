package com.example.alhuda.core.domain.model

import java.time.LocalDate

data class AppSettings(
    val calculationMethod: AppCalculationMethod = AppCalculationMethod.KEMENAG_INDONESIA,
    val adhanSoundUri: String? = null,
    val adhanSoundName: String? = null,
    val enabledPrayers: Set<String> = setOf("Subuh", "Dzuhur", "Ashar", "Maghrib", "Isya"),
    val dateOverrides: Map<String, Boolean> = emptyMap()
)

/**
 * Cek apakah sholat aktif pada tanggal tertentu.
 * Cek dateOverrides dulu (override spesifik tanggal), jika tidak ada fallback ke enabledPrayers (global).
 */
fun isPrayerActiveOnDate(
    prayer: String,
    date: LocalDate,
    enabledPrayers: Set<String>,
    dateOverrides: Map<String, Boolean>
): Boolean {
    val key = "${prayer}_${date}"
    return dateOverrides[key] ?: enabledPrayers.any { it.equals(prayer, ignoreCase = true) }
}
