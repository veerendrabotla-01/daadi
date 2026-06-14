package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

// Premium cozy Sandalwood & Rosewood color scheme
private val ClassicWoodLightColors = lightColorScheme(
    primary = DeepCarvingBrown,
    onPrimary = Color.White,
    primaryContainer = WarmWoodSandal,
    onPrimaryContainer = DeepCarvingBrown,
    secondary = WarmWoodSandal,
    onSecondary = DeepCarvingBrown,
    background = WarmWoodLight,
    onBackground = DeepCarvingBrown,
    surface = CardSandBackground,
    onSurface = DeepCarvingBrown,
    error = FailureCrimson,
    onError = Color.White
)

private val ClassicWoodDarkColors = darkColorScheme(
    primary = WarmWoodSandal,
    onPrimary = DeepCarvingBrown,
    primaryContainer = DeepCarvingBrown,
    onPrimaryContainer = WarmWoodLight,
    secondary = WarmWoodSandal,
    onSecondary = DeepCarvingBrown,
    background = Color(0xFF221307), // Deep dark mahogany wood
    onBackground = WarmWoodLight,
    surface = Color(0xFF2C1C10), // Rich walnut wood panels
    onSurface = WarmWoodLight,
    error = FailureCrimson,
    onError = Color.White
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colors = if (darkTheme) {
        ClassicWoodDarkColors
    } else {
        ClassicWoodLightColors
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as android.app.Activity).window
            window.statusBarColor = android.graphics.Color.TRANSPARENT
            window.navigationBarColor = android.graphics.Color.TRANSPARENT
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colors,
        typography = Typography,
        content = content
    )
}
