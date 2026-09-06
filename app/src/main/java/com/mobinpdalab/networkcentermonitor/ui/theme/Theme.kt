package com.mobinpdalab.networkcentermonitor.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import com.mobinpdalab.networkcentermonitor.R

private val Vazirharf = FontFamily(
    Font(R.font.vazirharf_regular, FontWeight.Normal),
    Font(R.font.vazirharf_medium, FontWeight.Medium),
    Font(R.font.vazirharf_bold, FontWeight.Bold)
)

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

private val AppTypography = Typography().run {
    copy(
        displayLarge = displayLarge.copy(fontFamily = Vazirharf),
        displayMedium = displayMedium.copy(fontFamily = Vazirharf),
        displaySmall = displaySmall.copy(fontFamily = Vazirharf),
        headlineLarge = headlineLarge.copy(fontFamily = Vazirharf),
        headlineMedium = headlineMedium.copy(fontFamily = Vazirharf),
        headlineSmall = headlineSmall.copy(fontFamily = Vazirharf),
        titleLarge = titleLarge.copy(fontFamily = Vazirharf),
        titleMedium = titleMedium.copy(fontFamily = Vazirharf),
        titleSmall = titleSmall.copy(fontFamily = Vazirharf),
        bodyLarge = bodyLarge.copy(fontFamily = Vazirharf),
        bodyMedium = bodyMedium.copy(fontFamily = Vazirharf),
        bodySmall = bodySmall.copy(fontFamily = Vazirharf),
        labelLarge = labelLarge.copy(fontFamily = Vazirharf),
        labelMedium = labelMedium.copy(fontFamily = Vazirharf),
        labelSmall = labelSmall.copy(fontFamily = Vazirharf)
    )
}

@Composable
fun NetworkCenterMonitorTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = LightColors,
        typography = AppTypography,
        content = content
    )
}
