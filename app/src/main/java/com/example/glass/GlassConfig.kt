package com.example.glass

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.PowerManager
import android.provider.Settings
import androidx.compose.animation.core.Spring
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import com.example.ui.theme.AccentPreset

enum class GlassStyle {
    CLEAR,
    REGULAR,
    TINTED
}

enum class GlassVariant {
    REGULAR,
    CLEAR,
    TINTED,
    PROMINENT
}

@Immutable
data class GlassConfig(
    val style: GlassStyle = GlassStyle.CLEAR,
    val intensity: Float = 0.85f,
    val refractionStrength: Float = 18f,
    val rimWidthDp: Float = 18f,
    val chromaticDispersion: Float = 2.4f,
    val blurRadiusDp: Float = 18f,
    val tintAlpha: Float = 0.24f,
    val noiseAmount: Float = 0.028f,
    val innerGlowAlpha: Float = 0.34f,
    val accentPreset: AccentPreset = AccentPreset.FROSTED_GLASS,
    val springStiffness: Float = Spring.StiffnessMediumLow,
    val springDamping: Float = 0.68f,
    val pressScaleTarget: Float = 1.055f,
    val reduceGlass: Boolean = false,
    val reduceMotion: Boolean = false,
    val highContrast: Boolean = false,
    val hapticsEnabled: Boolean = true,
    val hapticStrength: Float = 0.8f,
    val largerTouchTargets: Boolean = false,
    val isPowerSaveFallback: Boolean = false
) {
    val effectiveBlurDp: Float
        get() = when {
            reduceGlass || isPowerSaveFallback -> 0f
            style == GlassStyle.CLEAR -> blurRadiusDp * (0.45f + 0.45f * intensity)
            style == GlassStyle.REGULAR -> blurRadiusDp * (0.65f + 0.55f * intensity)
            style == GlassStyle.TINTED -> blurRadiusDp * (0.85f + 0.65f * intensity)
            else -> blurRadiusDp
        }

    val effectiveRefraction: Float
        get() = when {
            reduceGlass || isPowerSaveFallback -> 0f
            style == GlassStyle.CLEAR -> refractionStrength * (0.7f + 0.6f * intensity)
            style == GlassStyle.REGULAR -> refractionStrength * (0.55f + 0.45f * intensity)
            style == GlassStyle.TINTED -> refractionStrength * (0.4f + 0.4f * intensity)
            else -> refractionStrength
        }

    val effectiveTintAlpha: Float
        get() = when {
            reduceGlass || isPowerSaveFallback -> if (highContrast) 0.96f else 0.92f
            highContrast -> (tintAlpha + 0.46f).coerceAtMost(0.90f)
            style == GlassStyle.CLEAR -> 0.34f + 0.18f * intensity
            style == GlassStyle.REGULAR -> 0.52f + 0.22f * intensity
            style == GlassStyle.TINTED -> 0.64f + 0.24f * intensity
            else -> tintAlpha
        }
}

@Stable
class TiltLightState {
    var tiltX by mutableFloatStateOf(-0.45f)
    var tiltY by mutableFloatStateOf(-0.65f)
    var touchOffsetX by mutableFloatStateOf(0f)
    var touchOffsetY by mutableFloatStateOf(0f)

    val lightDirX: Float get() = (tiltX + touchOffsetX).coerceIn(-1f, 1f)
    val lightDirY: Float get() = (tiltY + touchOffsetY).coerceIn(-1f, 1f)
}

@Composable
fun rememberTiltLightState(enabled: Boolean): TiltLightState {
    val context = LocalContext.current
    val state = remember { TiltLightState() }

    DisposableEffect(context, enabled) {
        if (!enabled) {
            state.tiltX = -0.45f
            state.tiltY = -0.65f
            return@DisposableEffect onDispose {}
        }
        val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
        val sensor = sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        if (sensorManager == null || sensor == null) {
            return@DisposableEffect onDispose {}
        }
        val listener = object : SensorEventListener {
            private var smoothX = -0.45f
            private var smoothY = -0.65f

            override fun onSensorChanged(event: SensorEvent) {
                // Low-pass filter for silky specular bevel & 3D parallax motion
                val targetX = (-event.values[0] / 9.81f).coerceIn(-0.85f, 0.85f)
                val targetY = ((event.values[1] - 4.5f) / 9.81f).coerceIn(-0.85f, 0.85f)
                smoothX += (targetX - smoothX) * 0.12f
                smoothY += (targetY - smoothY) * 0.12f
                state.tiltX = smoothX
                state.tiltY = smoothY
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
        }
        sensorManager.registerListener(listener, sensor, SensorManager.SENSOR_DELAY_UI)
        onDispose {
            sensorManager.unregisterListener(listener)
        }
    }
    return state
}

fun Context.isSystemAnimationsRemoved(): Boolean {
    return try {
        Settings.Global.getFloat(contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f
    } catch (_: Throwable) {
        false
    }
}

fun Context.isPowerSaveActive(): Boolean {
    return try {
        val pm = getSystemService(Context.POWER_SERVICE) as? PowerManager
        pm?.isPowerSaveMode == true
    } catch (_: Throwable) {
        false
    }
}

val LocalGlassConfig = staticCompositionLocalOf { GlassConfig() }
val LocalTiltLightState = staticCompositionLocalOf { TiltLightState() }

@Composable
fun resolveGlassAccentColor(isDark: Boolean): Color {
    val config = LocalGlassConfig.current
    return config.accentPreset.resolve(isDark)
}
