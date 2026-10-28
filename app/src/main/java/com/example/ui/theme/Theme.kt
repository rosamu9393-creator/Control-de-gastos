package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColorScheme = lightColorScheme(
    primary = PrimaryGreen,
    onPrimary = Color.White,
    primaryContainer = PrimaryContainerGreen,
    onPrimaryContainer = OnPrimaryContainerGreen,
    secondary = AccentGold,
    onSecondary = Color.Black,
    secondaryContainer = AccentGoldContainer,
    onSecondaryContainer = Color(0xFF261900),
    background = BackgroundLight,
    onBackground = TextPrimary,
    surface = SurfaceLight,
    onSurface = TextPrimary,
    surfaceVariant = SurfaceVariantLight,
    onSurfaceVariant = TextSecondary,
    outline = OutlineLight
)

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFF55D898),
    onPrimary = Color(0xFF00391F),
    primaryContainer = PrimaryGreenDark,
    onPrimaryContainer = Color(0xFF75F5B3),
    secondary = AccentGold,
    onSecondary = Color.Black,
    background = Color(0xFF111412),
    onBackground = Color(0xFFE1E4DF),
    surface = Color(0xFF191D1A),
    onSurface = Color(0xFFE1E4DF),
    surfaceVariant = Color(0xFF232A25),
    onSurfaceVariant = Color(0xFFBFC9C1),
    outline = Color(0xFF404B43)
)

@Composable
fun GastosFacilTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
