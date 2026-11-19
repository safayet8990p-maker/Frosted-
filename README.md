# Glassly (`v1.0.7`) — Liquid Glass Gallery for Android

**Glassly** (`com.glassly.app`, Version `1.0.7`, Build `111`) is a native Kotlin + Jetpack Compose gallery experience faithfully recreating Apple's iOS 26/27 **Liquid Glass** material system and **Photos** gestures, backed by Android `MediaStore`, `Room`, `DataStore`, `Coil`, `Media3 ExoPlayer`, and `AndroidX Biometric`.

---

## Setup & Build Steps

1. **Requirements**: Android Studio / Gradle JDK 17+, `minSdk = 31` (Android 12+), `targetSdk = 36`.
2. **Run / Build**:
   - In AI Studio, launch directly in the **Streaming Android Emulator**.
   - Via CLI: `gradle assembleDebug` or `gradle :app:testDebugUnitTest`.
3. **Permissions on First Launch**:
   - Glassly starts with a brief Welcome Tour and an iOS-style Media Privacy prompt.
   - Grant **Allow all**, **Allow limited access** (Android 14+ Partial Photo Access), or continue without permission to explore the built-in curated showcase library.

---

## How `GlassConfig` Works

Every optical, physics, and accessibility parameter of the Liquid Glass engine is centralized in `com.example.glass.GlassConfig`:

- `style`: `CLEAR` (bright transparent lensing), `REGULAR` (balanced frosted refraction), or `TINTED` (stronger backdrop separation).
- `intensity`: Global multiplier (`0.2..1.0`) adjusted live from Settings > Glass Intensity.
- `refractionStrength` & `rimWidthDp`: Controls how strongly the background bends inside the rounded-rectangle SDF edge bevel.
- `chromaticDispersion`: Subtle RGB channel offset along the beveled rim.
- `blurRadiusDp`: Underlying backdrop blur radius (`RenderEffect.createBlurEffect` on API 31+).
- `noiseAmount` & `innerGlowAlpha`: Fine film grain and soft top-left edge glow.
- `springStiffness`, `springDamping`, `pressScaleTarget`: Drives the spring press bounce (`~1.055x`), squash/stretch, and `GlassContainer` droplet morphing.
- `reduceGlass`, `reduceMotion`, `highContrast`, `isPowerSaveFallback`: Automatically disables heavy shaders/motion and switches to clean high-contrast tonal surfaces when Battery Saver or system accessibility toggles are enabled.

### Zero-Recursion Backdrop Capture
Floating controls never blur/refract themselves: content sits inside `Modifier.glassBackdropSource(backdropState)` (rendered into a `rememberGraphicsLayer()`), while floating chrome sits as siblings using `Modifier.liquidGlass(...)` reading that captured backdrop layer with real-time smoothed accelerometer tilt (`TiltLightState`) and press shimmer.

---

## Honest Differences from Apple iOS 26 Liquid Glass

1. **System Compositor vs. Per-Container Layering**: iOS 26 renders Liquid Glass inside Apple's system compositor (`CoreAnimation` / `Metal`) across every view globally. On Android, third-party apps cannot sample pixels behind other apps or system status bars, so Glassly captures its own internal content tree via Compose `GraphicsLayer`.
2. **Android API Level Tiers**:
   - **Android 13+ (API 33–36)**: Full custom **AGSL `RuntimeShader`** with SDF edge lensing, RGB chromatic dispersion, specular tilt highlight, and backdrop blur.
   - **Android 12 / 12L (API 31–32)**: Custom `RenderEffect` backdrop blur + crisp specular border and adaptive tint (because `RuntimeShader` requires API 33).
   - **Reduce Glass / Battery Saver**: Clean opaque/tonal Material surface with WCAG AA contrast.
3. **Spatial Scene 3D Depth**: iOS uses neural monocular depth-map synthesis on Apple Silicon. Glassly uses multi-plane foreground/depth separation driven smoothly by live device accelerometer tilt and single-finger drag so it works both handheld and on a desk/emulator.
4. **System Icons & Fonts**: No Apple icons, logos, or SF Pro assets are used. Glassly pairs custom original glass vectors, Google's **Plus Jakarta Sans** & **Inter**, and rounded **Material Symbols**.

---

## Plain-Language Summary for the Owner

### What Was Built
- **True Liquid Glass Feel**: Floating bars, switches, sliders, pop-up menus, dialogs, and buttons bend the colorful photos behind them along rounded glass edges, catch light when you tilt or tap your phone, and smoothly merge/separate like liquid droplets.
- **Complete Photos Experience**:
  - **Library**: Opens at your newest photos at the bottom, lets you pinch to zoom grid size, switch **Years / Months / All**, combine multiple filters (like Favorites + Videos), and multi-select items with floating Share and Delete pills.
  - **Collections**: Displays Pinned collections, Custom & Device Albums, Smart Groups, Utilities (Hidden Vault locked with PIN `2626` or fingerprint/face biometric, 30-day Recently Deleted trash, Duplicate Finder, Large Files Cleaner), plus customizable layout styles and section reordering.
  - **Search**: Thumb-friendly bottom glass search bar that understands natural phrases like `"videos last month"`, `"favorites"`, seasons, years, and places.
  - **Photo & Video Viewer**: Full-screen pinch zoom, double-tap zoom, swipe down to close, swipe up for camera/EXIF info and mini-map, custom glass video player with speed/loop/PiP controls, Motion Photo long-press, and the **Spatial Scene** 3D toggle.
  - **Editor**: Brightness/contrast/saturation/warmth/vignette, filters, crop/rotate/straighten, per-editor Light/Dark mode switch, Copy/Paste Edits, and **Markup Loupe** (a movable magnifying lens with blue resize and green zoom handles).
  - **Settings & Bilingual**: Live glass preview card, instant **English / Bangla** language switch, Reduce Glass/Motion accessibility controls, and Reset All Settings.

### Verification Status in This Cloud Environment
- Automated compilation (`compile_applet`) and Robolectric unit/Compose UI tests (`gradle :app:testDebugUnitTest`) verify the code, resources, shaders, search logic, editor serialization, and UI components.
- Because this server-side build workspace does not include a live Android emulator or ADB screenshot bridge, visual pixel inspection should be done directly in the **AI Studio Streaming Emulator**.
