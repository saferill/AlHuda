package com.example.alhuda.core.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.example.alhuda.core.domain.model.AppCalculationMethod
import com.example.alhuda.core.domain.model.AppSettings
import com.example.alhuda.core.domain.repository.AppSettingsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
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

            AppSettings(
                calculationMethod = method,
                adhanSoundUri = adhanUri,
                adhanSoundName = adhanName
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
}
