package com.example.glass

import android.os.Build
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.ui.theme.LocalGlassColors
import kotlin.math.abs

@Stable
class GlassBackdropState(
    val layer: GraphicsLayer
) {
    var sourceOffsetInRoot: Offset = Offset.Zero
    var sourceSize: Size = Size.Zero
    var estimatedBackdropLuminance: Float = 0.5f
    @Volatile var hasRecordedContent: Boolean = false
    @Volatile var isRecordingSource: Boolean = false
}

val LocalGlassBackdrop = compositionLocalOf<GlassBackdropState?> { null }

/**
 * Enforces the "never glass inside glass" rule: when a floating glass container
 * (GlassCard, GlassBottomSheetOverlay, GlassPopoverMenu, GlassAlertDialog) renders its
 * outer Liquid Glass surface, child controls inside it fall back to crisp flat tonal fills
 * instead of nesting another RuntimeShader + GraphicsLayer.
 */
val LocalInsideGlassSurface = compositionLocalOf { false }

@Composable
fun rememberGlassBackdropState(): GlassBackdropState {
    val layer = rememberGraphicsLayer()
    return remember(layer) { GlassBackdropState(layer) }
}

private object GlassBackdropRecorderGuard {
    @Volatile var activeRecordingCount: Int = 0
}

fun isAnyBackdropRecording(): Boolean = GlassBackdropRecorderGuard.activeRecordingCount > 0

/**
 * Wrap ONLY the non-floating background/content layer with this modifier.
 * Floating glass controls must sit in sibling overlay layers to guarantee zero draw recursion.
 */
fun Modifier.glassBackdropSource(
    state: GlassBackdropState,
    backdropLuminanceHint: Float = 0.5f
): Modifier = this
    .onGloballyPositioned { coords ->
        if (coords.isAttached) {
            state.sourceOffsetInRoot = coords.positionInRoot()
            state.sourceSize = Size(coords.size.width.toFloat(), coords.size.height.toFloat())
            state.estimatedBackdropLuminance = backdropLuminanceHint
        }
    }
    .drawWithContent {
        if (size.width >= 2f && size.height >= 2f && !state.isRecordingSource) {
            state.isRecordingSource = true
            GlassBackdropRecorderGuard.activeRecordingCount += 1
            try {
                state.layer.clip = true
                state.layer.record {
                    this@drawWithContent.drawContent()
                }
                state.hasRecordedContent = true
            } finally {
                GlassBackdropRecorderGuard.activeRecordingCount =
                    (GlassBackdropRecorderGuard.activeRecordingCount - 1).coerceAtLeast(0)
                state.isRecordingSource = false
            }
            drawLayer(state.layer)
        } else {
            drawContent()
        }
    }

private data class CachedGlassEffectKey(
    val widthPx: Int,
    val heightPx: Int,
    val cornerPx: Int,
    val blurPx: Int,
    val refraction: Int,
    val dispersion: Int,
    val lightX: Int,
    val lightY: Int,
    val tintArgb: Int,
    val backdropBaseArgb: Int,
    val shimmerQ: Int,
    val thicknessQ: Int
)

private class EffectCacheHolder {
    var key: CachedGlassEffectKey? = null
}

/**
 * Applies authentic Liquid Glass rendering to any floating surface without distorting child text/icons.
 */
fun Modifier.liquidGlass(
    shape: Shape = RoundedCornerShape(999.dp),
    cornerRadius: Dp = 24.dp,
    variant: GlassVariant = GlassVariant.REGULAR,
    tintOverride: Color? = null,
    isPressed: Boolean = false,
    thicknessBoost: Float = 0f,
    shimmerPhase: Float = 0f,
    adaptiveDarkBackdrop: Boolean? = null
): Modifier = composed {
    val config = LocalGlassConfig.current
    val glassColors = LocalGlassColors.current
    val backdropState = LocalGlassBackdrop.current
    val insideGlassSurface = LocalInsideGlassSurface.current
    val tiltState = LocalTiltLightState.current
    val density = LocalDensity.current

    val glassSurfaceLayer = rememberGraphicsLayer()
    val effectCache = remember { EffectCacheHolder() }
    var elementPosInRoot by remember { mutableStateOf(Offset.Zero) }

    val pressedBoost by animateFloatAsState(
        targetValue = if (isPressed) 0.35f else 0f,
        animationSpec = tween(140),
        label = "glassPressedThickness"
    )
    val totalThickness = (thicknessBoost + pressedBoost).coerceIn(0f, 1.2f)

    val useDarkGlass = adaptiveDarkBackdrop ?: glassColors.isDark
    val accent = resolveGlassAccentColor(useDarkGlass)
    val backdropBaseColor = if (useDarkGlass) Color.Black else Color.White

    val baseTint = when {
        tintOverride != null -> tintOverride
        variant == GlassVariant.PROMINENT -> {
            if (config.accentPreset.key == "frosted") {
                if (useDarkGlass) Color(0xE0FFFFFF) else Color(0xEC0E1116)
            } else {
                accent.copy(alpha = (config.effectiveTintAlpha + 0.24f).coerceAtMost(0.90f))
            }
        }
        variant == GlassVariant.TINTED -> {
            if (config.accentPreset.key == "frosted") {
                if (useDarkGlass) Color(0xA620232B) else Color(0xC8F5F7FA)
            } else {
                accent.copy(alpha = (config.effectiveTintAlpha * 0.65f + 0.14f).coerceAtMost(0.75f))
            }
        }
        variant == GlassVariant.CLEAR -> {
            val clearAlpha = if (config.reduceGlass || config.isPowerSaveFallback) {
                0.92f
            } else if (useDarkGlass) {
                (config.effectiveTintAlpha * 0.72f).coerceIn(0.28f, 0.56f)
            } else {
                (config.effectiveTintAlpha * 0.85f).coerceIn(0.42f, 0.68f)
            }
            if (useDarkGlass) Color(0xFF14161D).copy(alpha = clearAlpha)
            else Color(0xFFFAFCFF).copy(alpha = clearAlpha)
        }
        else -> {
            val regAlpha = if (config.reduceGlass || config.isPowerSaveFallback) {
                0.94f
            } else if (useDarkGlass) {
                (config.effectiveTintAlpha * 0.88f).coerceIn(0.45f, 0.76f)
            } else {
                (config.effectiveTintAlpha * 1.05f).coerceIn(0.58f, 0.82f)
            }
            if (useDarkGlass) Color(0xFF181B22).copy(alpha = regAlpha)
            else Color(0xFFFBFCFE).copy(alpha = regAlpha)
        }
    }

    val blurPx = with(density) { config.effectiveBlurDp.dp.toPx() }.coerceIn(0f, 26f)
    val rimWidthPx = with(density) { config.rimWidthDp.dp.toPx() }
    val cornerPx = with(density) { cornerRadius.toPx() }

    this
        .clip(shape)
        .onGloballyPositioned { coords ->
            if (coords.isAttached) {
                elementPosInRoot = coords.positionInRoot()
            }
        }
        .drawWithContent {
            val w = size.width
            val h = size.height
            if (w < 2f || h < 2f) {
                drawContent()
                return@drawWithContent
            }
            val effectiveCornerPx = cornerPx.coerceAtMost(minOf(w, h) * 0.5f)

            val srcW = backdropState?.sourceSize?.width ?: 0f
            val srcH = backdropState?.sourceSize?.height ?: 0f
            val relX = if (backdropState != null) elementPosInRoot.x - backdropState.sourceOffsetInRoot.x else 0f
            val relY = if (backdropState != null) elementPosInRoot.y - backdropState.sourceOffsetInRoot.y else 0f

            val intersectsSource = backdropState != null &&
                srcW >= 2f && srcH >= 2f &&
                relX.isFinite() && relY.isFinite() &&
                relX < srcW && relY < srcH &&
                (relX + w) > 0f && (relY + h) > 0f

            val canUseRealBackdrop = backdropState != null &&
                !insideGlassSurface &&
                backdropState.hasRecordedContent &&
                !backdropState.isRecordingSource &&
                !isAnyBackdropRecording() &&
                !config.reduceGlass &&
                !config.isPowerSaveFallback &&
                intersectsSource

            if (canUseRealBackdrop) {
                val activeBackdrop = backdropState
                val activeRefraction = config.effectiveRefraction * (1f + totalThickness * 0.4f)
                val activeDispersion = config.chromaticDispersion * config.intensity

                val newKey = CachedGlassEffectKey(
                    widthPx = w.toInt(),
                    heightPx = h.toInt(),
                    cornerPx = effectiveCornerPx.toInt(),
                    blurPx = blurPx.toInt(),
                    refraction = activeRefraction.toInt(),
                    dispersion = (activeDispersion * 10f).toInt(),
                    lightX = (tiltState.lightDirX * 8f).toInt(),
                    lightY = (tiltState.lightDirY * 8f).toInt(),
                    tintArgb = baseTint.hashCode(),
                    backdropBaseArgb = backdropBaseColor.hashCode(),
                    shimmerQ = (shimmerPhase * 30f).toInt(),
                    thicknessQ = (totalThickness * 16f).toInt()
                )

                if (newKey != effectCache.key) {
                    effectCache.key = newKey
                    glassSurfaceLayer.renderEffect = when {
                        Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU -> {
                            GlassShaders.createLiquidGlassRenderEffect(
                                widthPx = w,
                                heightPx = h,
                                cornerRadiusPx = effectiveCornerPx,
                                blurRadiusPx = blurPx,
                                refractionStrength = activeRefraction,
                                rimWidthPx = rimWidthPx,
                                dispersion = activeDispersion,
                                lightDirX = tiltState.lightDirX,
                                lightDirY = tiltState.lightDirY,
                                noiseAmount = config.noiseAmount,
                                innerGlow = config.innerGlowAlpha,
                                tintColor = baseTint,
                                backdropBaseColor = backdropBaseColor,
                                shimmerPhase = shimmerPhase,
                                thicknessBoost = totalThickness
                            )
                        }
                        else -> GlassShaders.createBlurOnlyRenderEffect(blurPx)
                    }
                }

                glassSurfaceLayer.clip = true
                glassSurfaceLayer.record {
                    clipRect(0f, 0f, w, h) {
                        translate(left = -relX, top = -relY) {
                            drawLayer(activeBackdrop.layer)
                        }
                    }
                }
                drawLayer(glassSurfaceLayer)

                // On API 31-32 (blur-only), apply clean adaptive tint manually
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
                    drawRoundRect(
                        color = baseTint,
                        cornerRadius = CornerRadius(effectiveCornerPx, effectiveCornerPx)
                    )
                }
            } else {
                // Crisp tonal fallback when inside outer glass, Reduce Glass / Power Save active, or offscreen
                if (variant == GlassVariant.PROMINENT && tintOverride != null) {
                    drawRoundRect(
                        color = tintOverride.copy(alpha = tintOverride.alpha.coerceAtLeast(0.92f)),
                        cornerRadius = CornerRadius(effectiveCornerPx, effectiveCornerPx)
                    )
                } else if (insideGlassSurface) {
                    drawRoundRect(
                        color = if (useDarkGlass) Color(0x30FFFFFF) else Color(0xC2F3F5F9),
                        cornerRadius = CornerRadius(effectiveCornerPx, effectiveCornerPx)
                    )
                } else {
                    drawRoundRect(
                        color = if (useDarkGlass) Color(0xEB1A1D24) else Color(0xF6F8FAFD),
                        cornerRadius = CornerRadius(effectiveCornerPx, effectiveCornerPx)
                    )
                    if (tintOverride != null) {
                        drawRoundRect(
                            color = baseTint.copy(alpha = (baseTint.alpha * 0.72f).coerceAtLeast(0.20f)),
                            cornerRadius = CornerRadius(effectiveCornerPx, effectiveCornerPx)
                        )
                    }
                }
            }

            // Crisp optical specular bevel stroke + subtle outer definition hairline on pure #FFFFFF
            val rimStrokeWidth = with(density) { if (config.highContrast) 1.5.dp.toPx() else 1.dp.toPx() }
            val startSpec = if (tiltState.lightDirX < 0f) Offset.Zero else Offset(w, 0f)
            val endSpec = Offset(w - startSpec.x, h)

            if (!useDarkGlass) {
                drawRoundRect(
                    color = Color(0x180E1116),
                    cornerRadius = CornerRadius(effectiveCornerPx, effectiveCornerPx),
                    style = Stroke(width = with(density) { 0.85.dp.toPx() })
                )
            }

            drawRoundRect(
                brush = Brush.linearGradient(
                    colors = listOf(
                        glassColors.glassRimTopLeft.copy(alpha = if (useDarkGlass) 0.62f else 0.96f),
                        Color.White.copy(alpha = if (useDarkGlass) 0.16f else 0.35f),
                        glassColors.glassRimBottomRight.copy(alpha = if (useDarkGlass) 0.28f else 0.70f)
                    ),
                    start = startSpec,
                    end = endSpec
                ),
                cornerRadius = CornerRadius(effectiveCornerPx, effectiveCornerPx),
                style = Stroke(width = rimStrokeWidth)
            )

            // Draw crisp un-blurred UI content (text, icons, sliders) on top
            drawContent()
        }
}
