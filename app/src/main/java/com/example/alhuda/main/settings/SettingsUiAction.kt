package com.example.alhuda.main.settings

import android.content.Context
import android.net.Uri
import com.example.alhuda.core.domain.model.AppCalculationMethod

sealed interface SettingsUiAction {
    data class SelectCalculationMethod(val method: AppCalculationMethod) : SettingsUiAction
    data class SelectAdhanAudio(val context: Context, val uri: Uri) : SettingsUiAction
    object ResetAdhanAudio : SettingsUiAction
    data class TogglePreview(val context: Context) : SettingsUiAction
    data class ToggleGlobalPrayer(val prayerName: String) : SettingsUiAction
}
