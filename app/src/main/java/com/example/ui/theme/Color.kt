package com.example.ui.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance

val SystemBlue = Color(0xFF007AFF)
val SystemBlueDark = Color(0xFF0A84FF)
val SystemRed = Color(0xFFFF3B30)
val SystemRedDark = Color(0xFFFF453A)
val SystemGreen = Color(0xFF34C759)
val SystemGreenDark = Color(0xFF30D158)
val SystemYellow = Color(0xFFFFD60A)
val SystemOrange = Color(0xFFFF9500)
val SystemPurple = Color(0xFFAF52DE)
val SystemTeal = Color(0xFF30B0C7)

enum class AccentPreset(val key: String, val lightColor: Color, val darkColor: Color) {
    FROSTED_GLASS("frosted", Color(0xFF1C1C1E), Color(0xFFFFFFFF)),
    OCEAN_BLUE("blue", SystemBlue, SystemBlueDark),
    VIBRANT_CORAL("coral", Color(0xFFFF5E54), Color(0xFFFF6E64)),
    EMERALD("emerald", Color(0xFF28B463), Color(0xFF34D076)),
    IRIS_VIOLET("violet", Color(0xFF7B61FF), Color(0xFF9A85FF)),
    WARM_AMBER("amber", Color(0xFFF59E0B), Color(0xFFFBBF24));

    fun resolve(isDark: Boolean): Color = if (isDark) darkColor else lightColor
}

@Immutable
data class GlassColorTokens(
    val isDark: Boolean,
    val background: Color,
    val secondaryBackground: Color,
    val cardSurface: Color,
    val flatRowFill: Color,
    val glassFillLight: Color,
    val glassFillDark: Color,
    val glassRimTopLeft: Color,
    val glassRimBottomRight: Color,
    val glassInnerGlow: Color,
    val glassShadow: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val textTertiary: Color,
    val divider: Color,
    val activePillTint: Color,
    val destructive: Color,
    val accentBlue: Color,
    val editorYellow: Color,
    val loupeBlue: Color,
    val loupeGreen: Color
)

val LightGlassColors = GlassColorTokens(
    isDark = false,
    background = Color(0xFFFFFFFF),
    secondaryBackground = Color(0xFFF2F2F7),
    cardSurface = Color(0xFFFFFFFF),
    flatRowFill = Color(0x0E000000),
    glassFillLight = Color(0xCCF8FAFD),
    glassFillDark = Color(0xB8F1F4F8),
    glassRimTopLeft = Color(0xFFFFFFFF),
    glassRimBottomRight = Color(0x99D8DFE8),
    glassInnerGlow = Color(0x66FFFFFF),
    glassShadow = Color(0x280A1220),
    textPrimary = Color(0xFF0E1116),
    textSecondary = Color(0xFF525A66),
    textTertiary = Color(0xFF7C8594),
    divider = Color(0x1C0E1116),
    activePillTint = Color(0x16000000),
    destructive = SystemRed,
    accentBlue = SystemBlue,
    editorYellow = SystemYellow,
    loupeBlue = SystemBlue,
    loupeGreen = SystemGreen
)

val DarkGlassColors = GlassColorTokens(
    isDark = true,
    background = Color(0xFF000000),
    secondaryBackground = Color(0xFF1C1C1E),
    cardSurface = Color(0xFF1C1C1E),
    flatRowFill = Color(0x1EFFFFFF),
    glassFillLight = Color(0x88FFFFFF),
    glassFillDark = Color(0xB2181B22),
    glassRimTopLeft = Color(0xCCFFFFFF),
    glassRimBottomRight = Color(0x38FFFFFF),
    glassInnerGlow = Color(0x2EFFFFFF),
    glassShadow = Color(0x70000000),
    textPrimary = Color(0xFFF7F8FA),
    textSecondary = Color(0xFFA7B0BE),
    textTertiary = Color(0xFF757E8D),
    divider = Color(0x24FFFFFF),
    activePillTint = Color(0x28FFFFFF),
    destructive = SystemRedDark,
    accentBlue = SystemBlueDark,
    editorYellow = SystemYellow,
    loupeBlue = SystemBlueDark,
    loupeGreen = SystemGreenDark
)

fun glassLightMaterialScheme(accent: Color) = lightColorScheme(
    primary = accent,
    onPrimary = if (accent.luminance() > 0.55f) Color(0xFF0E1116) else Color.White,
    secondary = SystemBlue,
    onSecondary = Color.White,
    tertiary = SystemTeal,
    background = LightGlassColors.background,
    onBackground = LightGlassColors.textPrimary,
    surface = LightGlassColors.cardSurface,
    onSurface = LightGlassColors.textPrimary,
    surfaceVariant = LightGlassColors.secondaryBackground,
    onSurfaceVariant = LightGlassColors.textSecondary,
    error = SystemRed,
    onError = Color.White,
    outline = LightGlassColors.divider
)

fun glassDarkMaterialScheme(accent: Color) = darkColorScheme(
    primary = accent,
    onPrimary = if (accent.luminance() > 0.55f) Color(0xFF0E1116) else Color.White,
    secondary = SystemBlueDark,
    onSecondary = Color.White,
    tertiary = SystemTeal,
    background = DarkGlassColors.background,
    onBackground = DarkGlassColors.textPrimary,
    surface = DarkGlassColors.cardSurface,
    onSurface = DarkGlassColors.textPrimary,
    surfaceVariant = DarkGlassColors.secondaryBackground,
    onSurfaceVariant = DarkGlassColors.textSecondary,
    error = SystemRedDark,
    onError = Color.Black,
    outline = DarkGlassColors.divider
)
