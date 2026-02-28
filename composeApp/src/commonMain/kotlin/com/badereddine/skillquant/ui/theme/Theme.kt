package com.badereddine.skillquant.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// SkillQuant fintech-inspired color palette
val DarkBackground = Color(0xFF0D1117)
val DarkSurface = Color(0xFF161B22)
val DarkSurfaceVariant = Color(0xFF21262D)
val DarkOnBackground = Color(0xFFF0F6FC)
val DarkOnSurface = Color(0xFFC9D1D9)
val DarkOnSurfaceVariant = Color(0xFF8B949E)

val TealPrimary = Color(0xFF2DD4BF)
val TealPrimaryDark = Color(0xFF14B8A6)
val TealContainer = Color(0xFF0D3D38)

val GoldAccent = Color(0xFFFFB020)
val GoldContainer = Color(0xFF3D2E0A)

val PositiveGreen = Color(0xFF3FB950)
val NegativeRed = Color(0xFFF85149)
val NeutralBlue = Color(0xFF58A6FF)

val LightBackground = Color(0xFFF6F8FA)
val LightSurface = Color(0xFFFFFFFF)
val LightSurfaceVariant = Color(0xFFE8ECF0)
val LightOnBackground = Color(0xFF1F2328)
val LightOnSurface = Color(0xFF1F2328)
val LightOnSurfaceVariant = Color(0xFF656D76)

private val DarkColorScheme = darkColorScheme(
    primary = TealPrimary,
    onPrimary = DarkBackground,
    primaryContainer = TealContainer,
    onPrimaryContainer = TealPrimary,
    secondary = GoldAccent,
    onSecondary = DarkBackground,
    secondaryContainer = GoldContainer,
    onSecondaryContainer = GoldAccent,
    tertiary = NeutralBlue,
    background = DarkBackground,
    onBackground = DarkOnBackground,
    surface = DarkSurface,
    onSurface = DarkOnSurface,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = DarkOnSurfaceVariant,
    error = NegativeRed,
    onError = DarkBackground,
)

private val LightColorScheme = lightColorScheme(
    primary = TealPrimaryDark,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD0F5EE),
    onPrimaryContainer = TealPrimaryDark,
    secondary = Color(0xFFD97706),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFFFF3CD),
    onSecondaryContainer = Color(0xFF92400E),
    tertiary = Color(0xFF2563EB),
    background = LightBackground,
    onBackground = LightOnBackground,
    surface = LightSurface,
    onSurface = LightOnSurface,
    surfaceVariant = LightSurfaceVariant,
    onSurfaceVariant = LightOnSurfaceVariant,
    error = Color(0xFFDC2626),
    onError = Color.White,
)

@Composable
fun SkillQuantTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
}

