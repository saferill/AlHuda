package com.example.alhuda.core.domain.repository

import com.example.alhuda.core.domain.model.AppCalculationMethod
import com.example.alhuda.core.domain.model.AppSettings
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

interface AppSettingsRepository {
    fun getSettings(): Flow<AppSettings>
    suspend fun updateSettings(settings: AppSettings)
    suspend fun updateCalculationMethod(method: AppCalculationMethod)
    suspend fun updateAdhanSound(uri: String?, fileName: String?)
    suspend fun toggleGlobalPrayerEnabled(prayerName: String)
    suspend fun setDateOverride(prayerName: String, date: LocalDate, isEnabled: Boolean)
    suspend fun cleanExpiredDateOverrides()
}
