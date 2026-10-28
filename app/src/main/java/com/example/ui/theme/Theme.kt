package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val BroAmoledColorScheme = darkColorScheme(
    primary = PureWhite,
    onPrimary = AmoledBlack,
    primaryContainer = AmoledBlack,
    onPrimaryContainer = PureWhite,
    secondary = SilverMid,
    onSecondary = AmoledBlack,
    secondaryContainer = AmoledBlack,
    onSecondaryContainer = SilverBright,
    tertiary = SilverMuted,
    onTertiary = PureWhite,
    background = AmoledBlack,
    onBackground = PureWhite,
    surface = AmoledBlack,
    onSurface = PureWhite,
    surfaceVariant = AmoledBlack,
    onSurfaceVariant = SilverMid,
    outline = WireframeBorderLow,
    outlineVariant = WireframeBorderMedium,
    error = SecurityDangerText,
    onError = PureWhite,
    errorContainer = AmoledBlack,
    onErrorContainer = SecurityDangerText
)

@Composable
fun BroTheme(
    content: @Composable () -> Unit
) {
    // Strictly AMOLED Pure Black (#000000) regardless of system dynamic color
    MaterialTheme(
        colorScheme = BroAmoledColorScheme,
        typography = Typography,
        content = content
    )
}
