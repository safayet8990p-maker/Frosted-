package com.example.ui.theme

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.runtime.Immutable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Immutable
object Spacing {
    val xxs: Dp = 2.dp
    val xs: Dp = 4.dp
    val sm: Dp = 8.dp
    val md: Dp = 12.dp
    val lg: Dp = 16.dp
    val xl: Dp = 24.dp
    val xxl: Dp = 32.dp
    val touchTarget: Dp = 48.dp
}

@Immutable
object CornerRadius {
    val xs: Dp = 8.dp
    val sm: Dp = 14.dp
    val md: Dp = 20.dp
    val lg: Dp = 26.dp
    val xl: Dp = 32.dp
    val pill: Dp = 999.dp

    /** Concentric radius rule: innerRadius = max(4.dp, outerRadius - padding) */
    fun concentric(outer: Dp, padding: Dp): Dp = (outer - padding).coerceAtLeast(4.dp)
}

@Immutable
object MotionTokens {
    const val SplashSettleMs = 650
    const val FastFadeMs = 160
    const val StandardTransitionMs = 280
    const val ExpandedMenuMs = 320

    fun <T> bouncySpring(stiffness: Float = Spring.StiffnessMediumLow) = spring<T>(
        dampingRatio = 0.68f,
        stiffness = stiffness
    )

    fun <T> crispSpring(stiffness: Float = Spring.StiffnessMedium) = spring<T>(
        dampingRatio = 0.85f,
        stiffness = stiffness
    )
}

@Immutable
object ElevationRules {
    val restingShadow: Dp = 8.dp
    val floatingBarShadow: Dp = 14.dp
    val expandedMenuShadow: Dp = 24.dp
    val dialogShadow: Dp = 30.dp
}
