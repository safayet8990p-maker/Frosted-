# Changelog

All notable changes to **Glassly** are documented in this file.

## [1.0.7] - Build 111

### Changed & Improved
- **Crystal-White Light Glass & Optical 6-Layer Stack**: Fixed Light-theme transparent backdrop compositing so floating bars and pills never render dark gray/black; upgraded `GlassShaders` AGSL pipeline to match the 6-layer diagram (`Background Image -> Saturation Boost -> Adaptive White/Dark Tint -> Blur Layer -> Fine Noise -> Directional Specular Highlight & SDF Refraction`).
- **Pure White & Pure Black Backgrounds**: Set pure `#FFFFFF` in Light mode and `#000000` in Dark mode across Library, Collections, Search, and Settings, letting photos drive color behind glass.
- **iOS 26 Video-Exact Header & Version Sync**: Cleaned Library top-right controls to Filter + Select (with Time Grouping accessible inside View Options), enhanced active tab contrast in Light mode, added live album backdrop refraction on Collections, and updated app version to **1.0.7**.

## [0.01.10] - Build 110

### Added
- **App Identity & Adaptive Launcher Icon**: Configured `com.glassly.app`, Version `0.01.10` (`versionCode = 110`), custom layered-glass adaptive icon (light, dark, and monochrome themed variants) with density PNG fallbacks, and sub-1-second Glassly splash entrance.
- **Liquid Glass Engine (`com.example.glass`)**:
  - Central `GlassConfig` dataclass controlling refraction strength, SDF rim width, RGB chromatic dispersion, blur radius, tint alpha, grain noise, spring stiffness/damping, and accessibility/battery-saver fallbacks.
  - Custom AGSL `RuntimeShader` (API 33+) with rounded-box SDF edge lensing, subtle chromatic dispersion, top-left specular bevel reactive to smoothed accelerometer tilt + touch position, film grain noise, and press shimmer sweep.
  - Zero-recursion `rememberGraphicsLayer` backdrop capture split (`Modifier.glassBackdropSource` + sibling `Modifier.liquidGlass`), API 31–32 `RenderEffect` blur fallback, and high-contrast solid tonal fallback when Reduce Glass or Battery Saver is active.
  - `GlassContainer` with AGSL smooth-union droplet morphing between adjacent glass shapes, plus spring press bounce and haptic ticks.
- **Shared Liquid Glass Component Library**: `GlassButton`, `GlassIconButton`, `GlassCard`, `GlassSwitch`, `GlassSlider`, `GlassStepper`, `GlassSegmentedControl`, `GlassSearchField`, `GlassChip`, `GlassBadge`, `GlassProgressLoader`, `GlassScrubberBubble`, `GlassTopToolbar`, `GlassSplitBottomBar`, `GlassPopoverMenu`, `GlassAlertDialog`, `GlassBottomSheetOverlay` (with flat tonal `GlassFlatSheetRow` inside so glass is never nested inside glass), `GlassToastBanner`, and a debug `GlassGalleryDebugScreen`.
- **iOS 26 Photos Architecture & Gestures**:
  - Brief iOS-style Welcome Tour + Android 13/14/15 full/partial/denied MediaStore permission flow with curated high-resolution showcase fallback when MediaStore is empty.
  - **Library Tab**: Oldest-at-top, newest-at-bottom auto-scroll, quick top-left header tap to jump to oldest, Library tab tap to jump to latest, pinch-to-zoom grid density (2–6 columns), Square vs Aspect Ratio grid toggle, Filter/Sort/View Options Liquid Glass popovers with multi-filter combinators, Years/Months/Days/All bottom-bar morph, drag/tap multi-select with batch actions and bottom `[Share | X Photos Selected | Delete]` bar, and fast-scroll date scrubber.
  - **Collections Tab**: `PG` Settings badge, 3-icon layout switcher (Top Row Emphasis / Dense Equal / Large Square), `Show All` / `Collapse All`, section Reorder mode, long-press Pin/Unpin & Album CRUD, and `Pinned` management sheet.
  - **Search Tab**: Bottom thumb-reach Glass search field, Recents cards (Recently Viewed, Edited, Shared), suggestion chips, persistent Room search history, and natural-language queries (e.g., `"videos last month"`, `"favorites"`, seasons, years, places).
  - **Photo & Video Viewer**: Horizontal pager, pinch/double-tap zoom, inertial pan, swipe-down-to-dismiss, swipe-up EXIF Info sheet with vector Mini-Map card, hexagonal **Spatial Scene** 3D depth parallax mode (tilt + touch), Media3 `ExoPlayer` with floating glass transport controls (`-10s`, Play/Pause, `+10s`, scrubber, speed, loop, mute, fullscreen rotation, Picture-in-Picture), Motion Photo long-press live pulse, Wallpaper setter, and Print support.
  - **Photo Editor**: Non-destructive Adjust sliders, 8 live Filter presets, Crop ratios / Rotate / Flip / Straighten, per-editor `Appearance` (`System / Dark / Light`) override, Copy/Paste Edits, Save as Duplicate or Copy/Overwrite, Hold-to-Compare, Markup brushes + **`+` Add Loupe** interactive magnifier with drag positioning, blue radius rim handle, green 12–3 o'clock zoom arc handle, and floating Glass Loupe mini-toolbar.
  - **Management & Settings**: Recently Deleted (30-day retention, restore, delete forever, empty trash), Hidden Vault (4-digit Glass PIN keypad + AndroidX `BiometricPrompt`), Duplicate Finder, Large Files Cleaner, Ken Burns Slideshow, and full Settings screen with Live Preview card, Bangla/English instant localization, and expanding Reset All confirmation.
