package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

val LocalGlassColors = staticCompositionLocalOf { LightGlassColors }

@Composable
fun GlasslyTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    accentColor: Color = AccentPreset.FROSTED_GLASS.resolve(darkTheme),
    useDynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val glassColors = if (darkTheme) DarkGlassColors else LightGlassColors
    val materialScheme = when {
        useDynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> glassDarkMaterialScheme(accentColor)
        else -> glassLightMaterialScheme(accentColor)
    }

    CompositionLocalProvider(
        LocalGlassColors provides glassColors
    ) {
        MaterialTheme(
            colorScheme = materialScheme,
            typography = GlassTypography,
            content = content
        )
    }
}
