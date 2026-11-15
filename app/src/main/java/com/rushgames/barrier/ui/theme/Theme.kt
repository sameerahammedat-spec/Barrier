package com.rushgames.barrier.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val LightColorScheme = lightColorScheme(
    primary = BarrierGreen,
    onPrimary = Color.White,
    primaryContainer = BarrierGreenContainer,
    onPrimaryContainer = BarrierGreenDark,
    secondary = BarrierPurple,
    onSecondary = Color.White,
    secondaryContainer = BarrierPurpleContainer,
    onSecondaryContainer = BarrierPurpleDark,
    error = BarrierRed,
    onError = Color.White,
    background = SurfaceLight,
    onBackground = TextPrimary,
    surface = SurfaceWhite,
    onSurface = TextPrimary,
    surfaceVariant = SurfaceLight,
    onSurfaceVariant = TextSecondary,
    outline = SurfaceBorder,
    outlineVariant = SurfaceBorder,
    inverseSurface = TextPrimary,
    inverseOnSurface = Color.White,
)

private val DarkColorScheme = darkColorScheme(
    primary = BarrierGreen,
    onPrimary = Color.Black,
    primaryContainer = BarrierGreenDark,
    onPrimaryContainer = BarrierGreenContainer,
    secondary = BarrierPurple,
    onSecondary = Color.Black,
    secondaryContainer = BarrierPurpleDark,
    onSecondaryContainer = BarrierPurpleContainer,
    error = BarrierRed,
    onError = Color.Black,
    background = SurfaceDark,
    onBackground = TextPrimaryDark,
    surface = SurfaceDarkCard,
    onSurface = TextPrimaryDark,
    surfaceVariant = SurfaceDarkElevated,
    onSurfaceVariant = TextSecondaryDark,
    outline = SurfaceDarkBorder,
    outlineVariant = SurfaceDarkBorder,
    inverseSurface = TextPrimaryDark,
    inverseOnSurface = SurfaceDark,
)

@Composable
fun BarrierTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val view = LocalView.current

    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.background.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = BarrierTypography,
        content = content
    )
}
