package com.example.alhuda.main.settings

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.OpenableColumns
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.alhuda.core.domain.model.AppCalculationMethod
import com.example.alhuda.core.domain.repository.AppSettingsRepository
import com.example.alhuda.main.alarm.AdhanAudioPlayer
import com.example.alhuda.main.alarm.SchedulerReconciler
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val appSettingsRepository: AppSettingsRepository,
    private val schedulerReconciler: SchedulerReconciler
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState(isLoading = true))
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            appSettingsRepository.getSettings().collect { settings ->
                _uiState.update { current ->
                    current.copy(
                        selectedMethod = settings.calculationMethod,
                        adhanSoundUri = settings.adhanSoundUri,
                        adhanSoundName = settings.adhanSoundName,
                        enabledPrayers = settings.enabledPrayers,
                        isLoading = false
                    )
                }
            }
        }
    }

    fun onAction(action: SettingsUiAction) {
        when (action) {
            is SettingsUiAction.SelectCalculationMethod -> {
                selectCalculationMethod(action.method)
            }
            is SettingsUiAction.SelectAdhanAudio -> {
                selectAdhanAudio(action.context, action.uri)
            }
            is SettingsUiAction.ResetAdhanAudio -> {
                resetAdhanAudio()
            }
            is SettingsUiAction.TogglePreview -> {
                togglePreview(action.context)
            }
            is SettingsUiAction.ToggleGlobalPrayer -> {
                toggleGlobalPrayer(action.prayerName)
            }
        }
    }

    private fun toggleGlobalPrayer(prayerName: String) {
        viewModelScope.launch {
            appSettingsRepository.toggleGlobalPrayerEnabled(prayerName)
            schedulerReconciler.reconcileAll()
        }
    }

    private fun selectCalculationMethod(method: AppCalculationMethod) {
        viewModelScope.launch {
            appSettingsRepository.updateCalculationMethod(method)
            schedulerReconciler.reconcileAll()
        }
    }

    private fun selectAdhanAudio(context: Context, uri: Uri) {
        viewModelScope.launch {
            try {
                val takeFlags = Intent.FLAG_GRANT_READ_URI_PERMISSION
                context.contentResolver.takePersistableUriPermission(uri, takeFlags)
            } catch (e: Exception) {
                // Ignore if not supported
            }

            val fileName = getFileName(context, uri) ?: uri.lastPathSegment ?: "Audio Pilihan"

            appSettingsRepository.updateAdhanSound(
                uri = uri.toString(),
                fileName = fileName
            )
        }
    }

    private fun resetAdhanAudio() {
        viewModelScope.launch {
            if (_uiState.value.isPreviewPlaying) {
                AdhanAudioPlayer.stopPreview()
                _uiState.update { it.copy(isPreviewPlaying = false) }
            }
            appSettingsRepository.updateAdhanSound(null, null)
        }
    }

    private fun togglePreview(context: Context) {
        if (_uiState.value.isPreviewPlaying) {
            AdhanAudioPlayer.stopPreview()
            _uiState.update { it.copy(isPreviewPlaying = false) }
        } else {
            _uiState.update { it.copy(isPreviewPlaying = true) }
            AdhanAudioPlayer.playPreview(
                context = context,
                customSoundUri = _uiState.value.adhanSoundUri
            ) {
                _uiState.update { it.copy(isPreviewPlaying = false) }
            }
        }
    }

    private fun getFileName(context: Context, uri: Uri): String? {
        var name: String? = null
        try {
            val cursor = context.contentResolver.query(uri, null, null, null, null)
            cursor?.use {
                if (it.moveToFirst()) {
                    val nameIndex = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (nameIndex >= 0) {
                        name = it.getString(nameIndex)
                    }
                }
            }
        } catch (e: Exception) {
            // Fallback
        }
        return name
    }

    override fun onCleared() {
        super.onCleared()
        AdhanAudioPlayer.stopPreview()
    }
}
