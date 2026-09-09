package com.example.alhuda.core.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import com.example.alhuda.core.domain.model.AppCalculationMethod
import com.example.alhuda.core.domain.model.AppSettings
import com.example.alhuda.core.domain.repository.AppSettingsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AppSettingsRepositoryImpl @Inject constructor(
    private val dataStore: DataStore<Preferences>
) : AppSettingsRepository {

    companion object {
        private val KEY_CALCULATION_METHOD = stringPreferencesKey("app_calculation_method")
        private val KEY_ADHAN_SOUND_URI = stringPreferencesKey("app_adhan_sound_uri")
        private val KEY_ADHAN_SOUND_NAME = stringPreferencesKey("app_adhan_sound_name")
        private val KEY_ENABLED_PRAYERS = stringSetPreferencesKey("app_enabled_prayers")
        private val KEY_DATE_OVERRIDES = stringSetPreferencesKey("app_date_overrides")
        private val DEFAULT_ENABLED_PRAYERS = setOf("Subuh", "Dzuhur", "Ashar", "Maghrib", "Isya")
    }

    override fun getSettings(): Flow<AppSettings> {
        return dataStore.data.map { prefs ->
            val methodName = prefs[KEY_CALCULATION_METHOD]
            val method = try {
                if (methodName != null) AppCalculationMethod.valueOf(methodName)
                else AppCalculationMethod.KEMENAG_INDONESIA
            } catch (e: Exception) {
                AppCalculationMethod.KEMENAG_INDONESIA
            }

            val adhanUri = prefs[KEY_ADHAN_SOUND_URI]
            val adhanName = prefs[KEY_ADHAN_SOUND_NAME]
            val enabledPrayers = prefs[KEY_ENABLED_PRAYERS] ?: DEFAULT_ENABLED_PRAYERS

            val rawOverrides = prefs[KEY_DATE_OVERRIDES] ?: emptySet()
            val dateOverrides = rawOverrides.mapNotNull { entry ->
                val parts = entry.split("=")
                if (parts.size == 2) {
                    parts[0] to parts[1].toBoolean()
                } else null
            }.toMap()

            AppSettings(
                calculationMethod = method,
                adhanSoundUri = adhanUri,
                adhanSoundName = adhanName,
                enabledPrayers = enabledPrayers,
                dateOverrides = dateOverrides
            )
        }
    }

    override suspend fun updateSettings(settings: AppSettings) {
        dataStore.edit { prefs ->
            prefs[KEY_CALCULATION_METHOD] = settings.calculationMethod.name
            if (settings.adhanSoundUri != null) {
                prefs[KEY_ADHAN_SOUND_URI] = settings.adhanSoundUri
            } else {
                prefs.remove(KEY_ADHAN_SOUND_URI)
            }
            if (settings.adhanSoundName != null) {
                prefs[KEY_ADHAN_SOUND_NAME] = settings.adhanSoundName
            } else {
                prefs.remove(KEY_ADHAN_SOUND_NAME)
            }
            prefs[KEY_ENABLED_PRAYERS] = settings.enabledPrayers
            prefs[KEY_DATE_OVERRIDES] = settings.dateOverrides.map { "${it.key}=${it.value}" }.toSet()
        }
    }

    override suspend fun updateCalculationMethod(method: AppCalculationMethod) {
        dataStore.edit { prefs ->
            prefs[KEY_CALCULATION_METHOD] = method.name
        }
    }

    override suspend fun updateAdhanSound(uri: String?, fileName: String?) {
        dataStore.edit { prefs ->
            if (uri != null) {
                prefs[KEY_ADHAN_SOUND_URI] = uri
            } else {
                prefs.remove(KEY_ADHAN_SOUND_URI)
            }
            if (fileName != null) {
                prefs[KEY_ADHAN_SOUND_NAME] = fileName
            } else {
                prefs.remove(KEY_ADHAN_SOUND_NAME)
            }
        }
    }

    override suspend fun toggleGlobalPrayerEnabled(prayerName: String) {
        dataStore.edit { prefs ->
            val current = (prefs[KEY_ENABLED_PRAYERS] ?: DEFAULT_ENABLED_PRAYERS).toMutableSet()
            if (current.contains(prayerName)) {
                current.remove(prayerName)
            } else {
                current.add(prayerName)
            }
            prefs[KEY_ENABLED_PRAYERS] = current
        }
    }

    override suspend fun setDateOverride(prayerName: String, date: LocalDate, isEnabled: Boolean) {
        val key = "${prayerName}_${date}"
        dataStore.edit { prefs ->
            val current = (prefs[KEY_DATE_OVERRIDES] ?: emptySet()).toMutableSet()
            current.removeAll { it.startsWith("${key}=") }
            current.add("${key}=${isEnabled}")
            prefs[KEY_DATE_OVERRIDES] = current
        }
    }

    override suspend fun cleanExpiredDateOverrides() {
        val today = LocalDate.now()
        dataStore.edit { prefs ->
            val current = prefs[KEY_DATE_OVERRIDES] ?: emptySet()
            val valid = current.filter { entry ->
                try {
                    val key = entry.substringBefore("=")
                    val dateStr = key.substringAfter("_")
                    val date = LocalDate.parse(dateStr)
                    !date.isBefore(today)
                } catch (e: Exception) {
                    false
                }
            }.toSet()
            prefs[KEY_DATE_OVERRIDES] = valid
        }
    }
}
