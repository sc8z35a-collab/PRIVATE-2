package com.rstlab.pocketpetultra.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColors = lightColorScheme(
    primary = Color(0xFF6F5B8C),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFEDE2FF),
    onPrimaryContainer = Color(0xFF241A32),
    secondary = Color(0xFF54706B),
    secondaryContainer = Color(0xFFD7E9E5),
    tertiary = Color(0xFF8A6048),
    background = Color(0xFFF9F7FB),
    surface = Color(0xFFFFFBFF),
    surfaceVariant = Color(0xFFECE7EE),
    error = Color(0xFFBA1A1A)
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFFD8B9FA),
    onPrimary = Color(0xFF3E2E54),
    primaryContainer = Color(0xFF55436D),
    onPrimaryContainer = Color(0xFFEEDCFF),
    secondary = Color(0xFFB8CCC7),
    secondaryContainer = Color(0xFF344B47),
    tertiary = Color(0xFFFFB68F),
    background = Color(0xFF131217),
    surface = Color(0xFF1B191F),
    surfaceVariant = Color(0xFF48454C),
    error = Color(0xFFFFB4AB)
)

@Composable
fun PocketPetTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (isSystemInDarkTheme()) DarkColors else LightColors,
        content = content
    )
}
