package com.example.alhuda.core.domain.repository

import com.example.alhuda.core.domain.model.AppCalculationMethod
import com.example.alhuda.core.domain.model.AppSettings
import com.example.alhuda.core.domain.model.SkippedOccurrence
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

interface AppSettingsRepository {
    fun getSettings(): Flow<AppSettings>
    suspend fun updateSettings(settings: AppSettings)
    suspend fun updateCalculationMethod(method: AppCalculationMethod)
    suspend fun updateAdhanSound(uri: String?, fileName: String?)
    suspend fun togglePrayerEnabled(prayerName: String)
    suspend fun toggleSkipOccurrence(prayerName: String, date: LocalDate)
    suspend fun cleanExpiredSkippedOccurrences()
}
