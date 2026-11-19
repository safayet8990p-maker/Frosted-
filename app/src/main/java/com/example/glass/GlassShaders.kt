package com.example.glass

import android.graphics.RenderEffect as AndroidRenderEffect
import android.graphics.RuntimeShader
import android.graphics.Shader
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RenderEffect
import androidx.compose.ui.graphics.asComposeRenderEffect

object GlassShaders {

    /**
     * Custom AGSL Liquid Glass shader (API 33+) implementing the full 6-layer optical stack:
     * 1. Background Image composited cleanly over solid theme backdropBaseColor (prevents dark alpha=0 mud)
     * 2. Edge lensing / curved SDF refraction + subtle RGB chromatic dispersion
     * 3. Saturation Layer (~1.30x chroma boost so photo hues shine through frost)
     * 4. Adaptive White / Dark Tint balancing luminance and preserving WCAG AA legibility
     * 5. Blur Layer (chained GPU RenderEffect Gaussian blur)
     * 6. Fine Noise Layer + Directional Top-Left Specular Bevel Highlight + Shimmer Sweep
     */
    private const val LIQUID_GLASS_AGSL = """
        uniform shader content;
        uniform float2 resolution;
        uniform float cornerRadius;
        uniform float refractionStrength;
        uniform float rimWidth;
        uniform float dispersion;
        uniform float2 lightDir;
        uniform float noiseAmount;
        uniform float innerGlow;
        uniform float4 tintColor;
        uniform float4 backdropBaseColor;
        uniform float shimmerPhase;
        uniform float thicknessBoost;

        float sdRoundedBox(float2 p, float2 b, float r) {
            float2 q = abs(p) - b + r;
            return length(max(q, 0.0)) + min(max(q.x, q.y), 0.0) - r;
        }

        float2 calcNormal(float2 p, float2 b, float r) {
            float eps = 1.5;
            float dx = sdRoundedBox(p + float2(eps, 0.0), b, r) - sdRoundedBox(p - float2(eps, 0.0), b, r);
            float dy = sdRoundedBox(p + float2(0.0, eps), b, r) - sdRoundedBox(p - float2(0.0, eps), b, r);
            float2 g = float2(dx, dy);
            float len = length(g);
            return len > 0.0001 ? g / len : float2(0.0, -1.0);
        }

        float hash12(float2 p) {
            float3 p3 = fract(float3(p.xyx) * 0.1031);
            p3 += dot(p3, p3.yzx + 33.33);
            return fract((p3.x + p3.y) * p3.z);
        }

        half4 main(float2 fragCoord) {
            float2 halfSize = max(resolution * 0.5, float2(1.0));
            float2 p = fragCoord - halfSize;
            float r = clamp(cornerRadius, 1.0, min(halfSize.x, halfSize.y));

            float dist = sdRoundedBox(p, halfSize, r);
            float2 normal = calcNormal(p, halfSize, r);

            // Normalized rim zone: 1.0 right at edge, 0.0 inside core
            float rimFactor = clamp(1.0 - (-dist / max(rimWidth, 1.0)), 0.0, 1.0);
            float lensCurve = pow(rimFactor, 2.2) * (1.0 + thicknessBoost * 0.35);

            // Convex refraction bends sample inward toward the center along normal
            float2 refractOffset = -normal * (refractionStrength * lensCurve);
            float2 dispOffset = normal * (dispersion * lensCurve);

            float2 maxCoord = max(resolution - float2(1.0), float2(1.0));
            float2 sampleBase = clamp(fragCoord + refractOffset, float2(0.0), maxCoord);
            float2 sampleR = clamp(sampleBase + dispOffset, float2(0.0), maxCoord);
            float2 sampleB = clamp(sampleBase - dispOffset, float2(0.0), maxCoord);

            half3 baseBg = half3(backdropBaseColor.rgb);
            half4 rawR = content.eval(sampleR);
            half4 rawG = content.eval(sampleBase);
            half4 rawB = content.eval(sampleB);
            half3 cR = clamp(rawR.rgb + baseBg * (half(1.0) - rawR.a), 0.0, 1.0);
            half3 cG = clamp(rawG.rgb + baseBg * (half(1.0) - rawG.a), 0.0, 1.0);
            half3 cB = clamp(rawB.rgb + baseBg * (half(1.0) - rawB.a), 0.0, 1.0);
            half3 refracted = half3(cR.r, cG.g, cB.b);

            // Saturation Layer: preserve rich colorful photo hues underneath the frosted tint
            half luma = dot(refracted, half3(0.299, 0.587, 0.114));
            half3 saturated = clamp(mix(half3(luma), refracted, half(1.30)), 0.0, 1.0);

            // Adaptive Tint Layer: brighter crystal-white veil in Light mode, rich dark glass in Dark mode
            half tintWeight = half(tintColor.a * mix(1.0, 0.84, float(luma)));
            half3 tinted = mix(saturated, half3(tintColor.rgb), tintWeight);

            // Specular Highlight reacting to touch + device tilt (safe normalize against zero vector)
            float lLen = length(lightDir);
            float2 lDir = lLen > 0.001 ? (lightDir / lLen) : float2(-0.5, -0.8);
            float spec = pow(clamp(dot(normal, -lDir), 0.0, 1.0), 3.0);
            float rimEdge = smoothstep(3.5, 0.0, abs(dist));
            float bevelHighlight = (spec * 0.48 + 0.12) * rimFactor + rimEdge * 0.36;

            // Soft inner glow near perimeter
            float glow = pow(rimFactor, 1.6) * innerGlow;

            // Interactive press shimmer sweep across pill
            float shimmerBand = 0.0;
            if (shimmerPhase > 0.001 && shimmerPhase < 0.999) {
                float proj = (fragCoord.x / max(resolution.x, 1.0)) * 0.8 + (fragCoord.y / max(resolution.y, 1.0)) * 0.2;
                float dShimmer = abs(proj - shimmerPhase);
                shimmerBand = smoothstep(0.22, 0.0, dShimmer) * 0.28;
            }

            // Fine optical noise grain
            float grain = (hash12(fragCoord) - 0.5) * noiseAmount;

            half3 finalRgb = tinted
                + half3(bevelHighlight * 0.42 + glow * 0.22 + shimmerBand)
                + half3(grain);

            return half4(clamp(finalRgb, 0.0, 1.0), 1.0);
        }
    """

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    fun createLiquidGlassRenderEffect(
        widthPx: Float,
        heightPx: Float,
        cornerRadiusPx: Float,
        blurRadiusPx: Float,
        refractionStrength: Float,
        rimWidthPx: Float,
        dispersion: Float,
        lightDirX: Float,
        lightDirY: Float,
        noiseAmount: Float,
        innerGlow: Float,
        tintColor: Color,
        backdropBaseColor: Color,
        shimmerPhase: Float,
        thicknessBoost: Float
    ): RenderEffect {
        val safeW = widthPx.coerceAtLeast(2f)
        val safeH = heightPx.coerceAtLeast(2f)
        val safeBlur = blurRadiusPx.coerceIn(0f, 22f)
        val shader = RuntimeShader(LIQUID_GLASS_AGSL).apply {
            setFloatUniform("resolution", safeW, safeH)
            setFloatUniform("cornerRadius", cornerRadiusPx.coerceIn(1f, minOf(safeW, safeH) * 0.5f))
            setFloatUniform("refractionStrength", refractionStrength.coerceIn(0f, 28f))
            setFloatUniform("rimWidth", rimWidthPx.coerceIn(2f, 64f))
            setFloatUniform("dispersion", dispersion.coerceIn(0f, 8f))
            setFloatUniform("lightDir", lightDirX.coerceIn(-1f, 1f), lightDirY.coerceIn(-1f, 1f))
            setFloatUniform("noiseAmount", noiseAmount.coerceIn(0f, 0.1f))
            setFloatUniform("innerGlow", innerGlow.coerceIn(0f, 1f))
            setFloatUniform(
                "tintColor",
                tintColor.red,
                tintColor.green,
                tintColor.blue,
                tintColor.alpha
            )
            setFloatUniform(
                "backdropBaseColor",
                backdropBaseColor.red,
                backdropBaseColor.green,
                backdropBaseColor.blue,
                1f
            )
            setFloatUniform("shimmerPhase", shimmerPhase.coerceIn(0f, 1f))
            setFloatUniform("thicknessBoost", thicknessBoost.coerceIn(0f, 1.5f))
        }
        val lensEffect = AndroidRenderEffect.createRuntimeShaderEffect(shader, "content")
        return if (safeBlur >= 1f) {
            val blurEffect = AndroidRenderEffect.createBlurEffect(
                safeBlur,
                safeBlur,
                Shader.TileMode.CLAMP
            )
            AndroidRenderEffect.createChainEffect(lensEffect, blurEffect).asComposeRenderEffect()
        } else {
            lensEffect.asComposeRenderEffect()
        }
    }

    fun createBlurOnlyRenderEffect(blurRadiusPx: Float): RenderEffect? {
        val safeBlur = blurRadiusPx.coerceIn(0f, 22f)
        if (safeBlur < 1f) return null
        return AndroidRenderEffect.createBlurEffect(
            safeBlur,
            safeBlur,
            Shader.TileMode.CLAMP
        ).asComposeRenderEffect()
    }
}
