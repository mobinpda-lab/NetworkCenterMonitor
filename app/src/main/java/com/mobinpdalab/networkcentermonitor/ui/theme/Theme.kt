package com.mobinpdalab.networkcentermonitor.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColors = lightColorScheme(
    primary = AppStatusColors.ActionBlue,
    onPrimary = Color.White,
    background = Color(0xFFF8F9FB),
    surface = Color.White,
    onBackground = Color(0xFF1F2328),
    onSurface = Color(0xFF1F2328),
    surfaceVariant = Color(0xFFF1F3F6),
    onSurfaceVariant = Color(0xFF666B73),
    error = AppStatusColors.Disconnected
)

@Composable
fun NetworkCenterMonitorTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = LightColors,
        typography = Typography(),
        content = content
    )
}
