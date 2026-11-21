package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.data.model.AppThemeMode

private val DarkColorScheme = darkColorScheme(
    primary = NeonViolet,
    onPrimary = Color(0xFF1F004D),
    primaryContainer = NeonVioletContainer,
    onPrimaryContainer = OnNeonVioletContainer,
    secondary = NeonCyan,
    onSecondary = Color(0xFF002630),
    secondaryContainer = NeonCyanContainer,
    onSecondaryContainer = OnNeonCyanContainer,
    tertiary = NeonPink,
    onTertiary = Color(0xFF3E0023),
    tertiaryContainer = NeonPinkContainer,
    onTertiaryContainer = OnNeonPinkContainer,
    background = CosmicBackground,
    onBackground = CosmicOnSurface,
    surface = CosmicSurface,
    onSurface = CosmicOnSurface,
    surfaceVariant = CosmicSurfaceVariant,
    onSurfaceVariant = CosmicOnSurfaceVariant,
    surfaceContainerHigh = CosmicSurfaceHigh,
    outline = CosmicOutline,
    error = NeonErrorDark,
    onError = Color(0xFF3D0011)
)

private val LightColorScheme = lightColorScheme(
    primary = LightViolet,
    onPrimary = Color.White,
    primaryContainer = LightVioletContainer,
    onPrimaryContainer = OnLightVioletContainer,
    secondary = LightCyan,
    onSecondary = Color.White,
    secondaryContainer = LightCyanContainer,
    onSecondaryContainer = OnLightCyanContainer,
    tertiary = LightPink,
    onTertiary = Color.White,
    tertiaryContainer = LightPinkContainer,
    onTertiaryContainer = OnLightPinkContainer,
    background = LightBackground,
    onBackground = LightOnSurface,
    surface = LightSurface,
    onSurface = LightOnSurface,
    surfaceVariant = LightSurfaceVariant,
    onSurfaceVariant = LightOnSurfaceVariant,
    outline = LightOutline,
    error = NeonErrorLight,
    onError = Color.White
)

private val PagliiShapes = Shapes(
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

@Composable
fun PagliiTheme(
    themeMode: AppThemeMode = AppThemeMode.DARK,
    content: @Composable () -> Unit
) {
    val useDark = when (themeMode) {
        AppThemeMode.DARK -> true
        AppThemeMode.LIGHT -> false
        AppThemeMode.SYSTEM -> isSystemInDarkTheme()
    }
    val colorScheme = if (useDark) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        shapes = PagliiShapes,
        content = content
    )
}
