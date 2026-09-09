package com.example.alhuda.core.presentation.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val FallbackDarkColorScheme = darkColorScheme()
private val FallbackLightColorScheme = lightColorScheme()

/**
 * Material You theme with dynamic color support (Android 12+).
 * Falls back to default Material 3 palette on older devices.
 */
@Composable
fun AlHudaTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val context = LocalContext.current
        if (darkTheme) dynamicDarkColorScheme(context)
        else dynamicLightColorScheme(context)
    } else {
        if (darkTheme) FallbackDarkColorScheme
        else FallbackLightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
}
