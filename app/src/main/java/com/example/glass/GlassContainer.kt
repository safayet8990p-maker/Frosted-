package com.example.glass

import android.graphics.RenderEffect as AndroidRenderEffect
import android.graphics.RuntimeShader
import android.graphics.Shader
import android.os.Build
import android.view.HapticFeedbackConstants
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.boundsInParent
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.ui.theme.LocalGlassColors

@Immutable
data class GlassPressVisualState(
    val isPressed: Boolean,
    val isFocused: Boolean,
    val scaleX: Float,
    val scaleY: Float,
    val shimmerPhase: Float
)

@Composable
fun rememberGlassPressState(
    interactionSource: MutableInteractionSource,
    enabled: Boolean = true,
    onPressedHaptic: Boolean = true
): GlassPressVisualState {
    val config = LocalGlassConfig.current
    val haptics = LocalHapticFeedback.current
    val view = LocalView.current
    val isPressed by interactionSource.collectIsPressedAsState()
    val isFocused by interactionSource.collectIsFocusedAsState()

    val shimmer = remember { Animatable(0f) }

    LaunchedEffect(isPressed, enabled) {
        if (isPressed && enabled) {
            if (onPressedHaptic && config.hapticsEnabled) {
                if (config.hapticStrength >= 0.6f) {
                    view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                } else {
                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                }
            }
            if (!config.reduceMotion) {
                shimmer.snapTo(0.02f)
                shimmer.animateTo(1f, animationSpec = tween(durationMillis = 360))
            }
        } else {
            shimmer.snapTo(0f)
        }
    }

    val targetScaleX = when {
        !enabled -> 1f
        config.reduceMotion -> 1f
        isPressed -> config.pressScaleTarget
        isFocused -> 1.02f
        else -> 1f
    }
    val targetScaleY = when {
        !enabled -> 1f
        config.reduceMotion -> 1f
        // Slight droplet squash along Y on press
        isPressed -> config.pressScaleTarget * 0.975f
        isFocused -> 1.02f
        else -> 1f
    }

    val animScaleX by animateFloatAsState(
        targetValue = targetScaleX,
        animationSpec = if (config.reduceMotion) tween(0) else spring(
            dampingRatio = config.springDamping,
            stiffness = config.springStiffness
        ),
        label = "glassPressScaleX"
    )
    val animScaleY by animateFloatAsState(
        targetValue = targetScaleY,
        animationSpec = if (config.reduceMotion) tween(0) else spring(
            dampingRatio = config.springDamping,
            stiffness = config.springStiffness
        ),
        label = "glassPressScaleY"
    )

    return GlassPressVisualState(
        isPressed = isPressed && enabled,
        isFocused = isFocused && enabled,
        scaleX = animScaleX,
        scaleY = animScaleY,
        shimmerPhase = shimmer.value
    )
}

fun Modifier.glassElevationShadow(
    elevation: Dp,
    shape: Shape
): Modifier = composed {
    val glassColors = LocalGlassColors.current
    val config = LocalGlassConfig.current
    val effectiveElevation = if (config.reduceGlass) elevation * 0.5f else elevation
    this.shadow(
        elevation = effectiveElevation,
        shape = shape,
        ambientColor = glassColors.glassShadow,
        spotColor = glassColors.glassShadow
    )
}

/**
 * GlassContainer (analogous to iOS GlassEffectContainer):
 * Tracks child glass pill bounds and renders an SDF smooth-union liquid bridge
 * whenever adjacent glass elements approach within [morphSpacing].
 */
@Stable
class GlassContainerState {
    val childRects = mutableStateMapOf<String, Rect>()
}

private const val MORPH_CONTAINER_AGSL = """
    uniform shader content;
    uniform float2 resolution;
    uniform float4 rectA;
    uniform float4 rectB;
    uniform float smoothK;
    uniform float4 tintColor;
    uniform float4 backdropBaseColor;

    float sdRoundBox(float2 p, float2 center, float2 halfSize, float r) {
        float2 q = abs(p - center) - halfSize + r;
        return length(max(q, 0.0)) + min(max(q.x, q.y), 0.0) - r;
    }

    float smin(float a, float b, float k) {
        float h = clamp(0.5 + 0.5 * (b - a) / max(k, 0.001), 0.0, 1.0);
        return mix(b, a, h) - k * h * (1.0 - h);
    }

    half4 main(float2 fragCoord) {
        float2 cA = float2((rectA.x + rectA.z) * 0.5, (rectA.y + rectA.w) * 0.5);
        float2 hA = max(float2((rectA.z - rectA.x) * 0.5, (rectA.w - rectA.y) * 0.5), float2(1.0));
        float rA = min(hA.x, hA.y);

        float2 cB = float2((rectB.x + rectB.z) * 0.5, (rectB.y + rectB.w) * 0.5);
        float2 hB = max(float2((rectB.z - rectB.x) * 0.5, (rectB.w - rectB.y) * 0.5), float2(1.0));
        float rB = min(hB.x, hB.y);

        float dA = sdRoundBox(fragCoord, cA, hA, rA);
        float dB = sdRoundBox(fragCoord, cB, hB, rB);
        float dUnion = smin(dA, dB, smoothK);

        // Draw only the smooth-union neck bridge outside the individual pills
        float neckMask = smoothstep(2.0, -2.0, dUnion) * smoothstep(-1.0, 3.0, min(dA, dB));
        if (neckMask <= 0.01) {
            return half4(0.0, 0.0, 0.0, 0.0);
        }

        half4 bg = content.eval(fragCoord);
        half3 compBg = clamp(bg.rgb + half3(backdropBaseColor.rgb) * (half(1.0) - bg.a), 0.0, 1.0);
        half3 neckColor = mix(compBg, half3(tintColor.rgb), half(tintColor.a));
        float rim = smoothstep(3.0, 0.0, abs(dUnion)) * 0.35;
        return half4(clamp(neckColor + half3(rim), 0.0, 1.0), half(neckMask));
    }
"""

private data class MorphCacheKey(
    val w: Int,
    val h: Int,
    val ax1: Int,
    val ay1: Int,
    val ax2: Int,
    val ay2: Int,
    val bx1: Int,
    val by1: Int,
    val bx2: Int,
    val by2: Int,
    val isDark: Boolean
)

private class MorphCacheHolder {
    var key: MorphCacheKey? = null
}

@Composable
fun GlassContainer(
    modifier: Modifier = Modifier,
    morphSpacing: Dp = 20.dp,
    content: @Composable BoxScope.(GlassContainerState) -> Unit
) {
    val containerState = remember { GlassContainerState() }
    val config = LocalGlassConfig.current
    val glassColors = LocalGlassColors.current
    val backdropState = LocalGlassBackdrop.current
    val density = LocalDensity.current
    val bridgeLayer = rememberGraphicsLayer()
    val morphCache = remember { MorphCacheHolder() }
    var containerOffset = remember { Offset.Zero }
    val smoothKPx = with(density) { morphSpacing.toPx() }

    Box(
        modifier = modifier
            .onGloballyPositioned { coords ->
                if (coords.isAttached) {
                    containerOffset = coords.localToRoot(Offset.Zero)
                }
            }
            .drawWithContent {
                val rects = containerState.childRects.values.toList()
                val canMorph = rects.size >= 2 &&
                    size.width >= 2f &&
                    size.height >= 2f &&
                    !config.reduceGlass &&
                    !config.isPowerSaveFallback &&
                    Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                    backdropState != null &&
                    backdropState.hasRecordedContent &&
                    !backdropState.isRecordingSource &&
                    !isAnyBackdropRecording()

                if (canMorph) {
                    val rA = rects[0]
                    val rB = rects[1]
                    val gapX = maxOf(0f, maxOf(rA.left, rB.left) - minOf(rA.right, rB.right))
                    val activeBackdrop = backdropState
                    val relX = containerOffset.x - activeBackdrop.sourceOffsetInRoot.x
                    val relY = containerOffset.y - activeBackdrop.sourceOffsetInRoot.y

                    if (gapX < smoothKPx * 1.4f && relX.isFinite() && relY.isFinite()) {
                        val key = MorphCacheKey(
                            w = size.width.toInt(),
                            h = size.height.toInt(),
                            ax1 = rA.left.toInt(),
                            ay1 = rA.top.toInt(),
                            ax2 = rA.right.toInt(),
                            ay2 = rA.bottom.toInt(),
                            bx1 = rB.left.toInt(),
                            by1 = rB.top.toInt(),
                            bx2 = rB.right.toInt(),
                            by2 = rB.bottom.toInt(),
                            isDark = glassColors.isDark
                        )
                        if (key != morphCache.key) {
                            morphCache.key = key
                            val tint = if (glassColors.isDark) Color(0xB8181B22) else Color(0xD8FAFCFF)
                            val baseBg = glassColors.background
                            val shader = RuntimeShader(MORPH_CONTAINER_AGSL).apply {
                                setFloatUniform("resolution", size.width.coerceAtLeast(2f), size.height.coerceAtLeast(2f))
                                setFloatUniform("rectA", rA.left, rA.top, rA.right, rA.bottom)
                                setFloatUniform("rectB", rB.left, rB.top, rB.right, rB.bottom)
                                setFloatUniform("smoothK", smoothKPx)
                                setFloatUniform("tintColor", tint.red, tint.green, tint.blue, tint.alpha)
                                setFloatUniform("backdropBaseColor", baseBg.red, baseBg.green, baseBg.blue, 1f)
                            }
                            val bridgeEffect = AndroidRenderEffect.createRuntimeShaderEffect(shader, "content")
                            val blur = AndroidRenderEffect.createBlurEffect(14f, 14f, Shader.TileMode.CLAMP)
                            bridgeLayer.renderEffect = AndroidRenderEffect
                                .createChainEffect(bridgeEffect, blur)
                                .asComposeRenderEffect()
                        }

                        bridgeLayer.clip = true
                        bridgeLayer.record {
                            clipRect(0f, 0f, size.width, size.height) {
                                translate(left = -relX, top = -relY) {
                                    drawLayer(activeBackdrop.layer)
                                }
                            }
                        }
                        drawLayer(bridgeLayer)
                    }
                }
                drawContent()
            }
    ) {
        content(containerState)
    }
}

fun Modifier.glassContainerChild(
    state: GlassContainerState?,
    id: String
): Modifier = if (state == null) {
    this
} else {
    this.composed {
        DisposableEffect(state, id) {
            onDispose { state.childRects.remove(id) }
        }
        Modifier.onGloballyPositioned { coords ->
            if (coords.isAttached) {
                state.childRects[id] = coords.boundsInParent()
            }
        }
    }
}
