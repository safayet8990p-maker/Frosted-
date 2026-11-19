package com.example.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.DeleteSweep
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.PhotoLibrary
import androidx.compose.material.icons.rounded.PlayCircle
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Translate
import androidx.compose.material.icons.rounded.VerifiedUser
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.BuildConfig
import com.example.R
import com.example.data.model.AppLanguage
import com.example.data.model.AppThemeMode
import com.example.data.model.LibrarySortOrder
import com.example.data.model.MediaPermissionState
import com.example.data.model.SettingsState
import com.example.glass.GlassAlertDialog
import com.example.glass.GlassButton
import com.example.glass.GlassCard
import com.example.glass.GlassChip
import com.example.glass.GlassIconButton
import com.example.glass.GlassPickerRow
import com.example.glass.GlassSegmentedControl
import com.example.glass.GlassSlider
import com.example.glass.GlassStepper
import com.example.glass.GlassSwitch
import com.example.glass.GlassVariant
import com.example.glass.glassBackdropSource
import com.example.glass.glassElevationShadow
import com.example.glass.liquidGlass
import com.example.glass.rememberGlassBackdropState
import com.example.ui.i18n.LocalAppStrings
import com.example.ui.theme.AccentPreset
import com.example.ui.theme.CornerRadius
import com.example.ui.theme.LocalGlassColors
import com.example.ui.theme.Spacing

@Composable
fun SettingsScreen(
    settings: SettingsState,
    permissionState: MediaPermissionState,
    cacheSizeLabel: String,
    onUpdateSettings: ((SettingsState) -> SettingsState) -> Unit,
    onResetAllSettings: () -> Unit,
    onClearCache: () -> Unit,
    onRequestPermissions: () -> Unit,
    onOpenDuplicates: () -> Unit,
    onOpenLargeFiles: () -> Unit,
    onOpenGlassGalleryDebug: () -> Unit,
    onShowToast: (String) -> Unit,
    onBack: () -> Unit
) {
    val strings = LocalAppStrings.current
    val glassColors = LocalGlassColors.current
    val backdropState = rememberGlassBackdropState()
    val listState = rememberLazyListState()
    val isHeaderCollapsed by remember {
        derivedStateOf { listState.firstVisibleItemIndex > 0 || listState.firstVisibleItemScrollOffset > 80 }
    }

    var showResetConfirm by remember { mutableStateOf(false) }
    var showPinDialog by remember { mutableStateOf(false) }
    var showLicenseSheet by remember { mutableStateOf(false) }
    var pinInput by remember { mutableStateOf(settings.hiddenVaultPin) }

    androidx.compose.runtime.CompositionLocalProvider(
        com.example.glass.LocalGlassBackdrop provides backdropState
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // Pure solid White (#FFFFFF) / Black (#000000) background matching owner preference
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .glassBackdropSource(backdropState)
                    .background(glassColors.background)
            )

            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxSize()
                    .navigationBarsPadding(),
                contentPadding = PaddingValues(
                    top = 92.dp,
                    bottom = 48.dp,
                    start = Spacing.lg,
                    end = Spacing.lg
                ),
                verticalArrangement = Arrangement.spacedBy(Spacing.lg),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                item {
                    Text(
                        text = strings.settingsTitle,
                        style = MaterialTheme.typography.displayLarge,
                        color = glassColors.textPrimary,
                        modifier = Modifier
                            .fillMaxWidth()
                            .widthIn(max = 600.dp)
                            .padding(bottom = Spacing.xs)
                    )
                }

                // 0. LIVE PREVIEW CARD (matches iOS 26 Liquid Glass live preview over real colorful photography)
                item {
                    val previewBackdrop = rememberGlassBackdropState()
                    GlassCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .widthIn(max = 600.dp)
                            .testTag("settings_live_preview_card"),
                        cornerRadius = CornerRadius.xl
                    ) {
                        Text(
                            text = strings.livePreview,
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                            color = glassColors.textPrimary
                        )
                        Spacer(Modifier.height(Spacing.md))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(150.dp)
                                .clip(RoundedCornerShape(CornerRadius.md))
                        ) {
                            Image(
                                painter = painterResource(id = R.drawable.img_sample_aegean),
                                contentDescription = "Live preview backdrop",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .glassBackdropSource(previewBackdrop)
                            )
                            androidx.compose.runtime.CompositionLocalProvider(
                                com.example.glass.LocalGlassBackdrop provides previewBackdrop,
                                com.example.glass.LocalInsideGlassSurface provides false
                            ) {
                                Row(
                                    modifier = Modifier
                                        .align(Alignment.BottomCenter)
                                        .padding(Spacing.md)
                                        .fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    GlassButton(
                                        onClick = {},
                                        variant = when (settings.glassStyle) {
                                            com.example.glass.GlassStyle.CLEAR -> GlassVariant.CLEAR
                                            com.example.glass.GlassStyle.REGULAR -> GlassVariant.REGULAR
                                            com.example.glass.GlassStyle.TINTED -> GlassVariant.TINTED
                                        }
                                    ) {
                                        Text("Glassly ${settings.glassStyle.name.lowercase().replaceFirstChar { it.uppercase() }}")
                                    }
                                    GlassIconButton(
                                        icon = Icons.Rounded.AutoAwesome,
                                        contentDescription = "Preview icon",
                                        onClick = {},
                                        variant = GlassVariant.TINTED
                                    )
                                }
                            }
                        }
                    }
                }

                // 1. APPEARANCE
                item {
                    SettingsGroupCard(title = strings.appearance) {
                        Text(
                            text = "Theme",
                            style = MaterialTheme.typography.labelLarge,
                            color = glassColors.textSecondary
                        )
                        Spacer(Modifier.height(Spacing.sm))
                        GlassSegmentedControl(
                            options = listOf(AppThemeMode.SYSTEM, AppThemeMode.LIGHT, AppThemeMode.DARK),
                            selectedOption = settings.themeMode,
                            onOptionSelected = { mode -> onUpdateSettings { it.copy(themeMode = mode) } },
                            labelProvider = {
                                when (it) {
                                    AppThemeMode.SYSTEM -> strings.system
                                    AppThemeMode.LIGHT -> strings.light
                                    AppThemeMode.DARK -> strings.dark
                                }
                            },
                            testTag = "settings_theme_segment"
                        )

                        Spacer(Modifier.height(Spacing.lg))
                        Text(
                            text = strings.accentTint,
                            style = MaterialTheme.typography.labelLarge,
                            color = glassColors.textSecondary
                        )
                        Spacer(Modifier.height(Spacing.sm))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            AccentPreset.entries.forEach { preset ->
                                val swatchColor = if (preset == AccentPreset.FROSTED_GLASS) Color(0xFFDDE5F0) else preset.resolve(glassColors.isDark)
                                val selected = settings.accentPreset == preset
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(CircleShape)
                                        .background(swatchColor)
                                        .border(
                                            width = if (selected) 3.dp else 1.dp,
                                            color = if (selected) glassColors.accentBlue else Color.White.copy(alpha = 0.6f),
                                            shape = CircleShape
                                        )
                                        .clickable { onUpdateSettings { it.copy(accentPreset = preset) } },
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (selected) {
                                        Icon(
                                            imageVector = Icons.Rounded.Check,
                                            contentDescription = preset.key,
                                            tint = if (preset == AccentPreset.FROSTED_GLASS) Color.Black else Color.White,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(Modifier.height(Spacing.lg))
                        Text(
                            text = strings.glassStyle,
                            style = MaterialTheme.typography.labelLarge,
                            color = glassColors.textSecondary
                        )
                        Spacer(Modifier.height(Spacing.sm))
                        GlassSegmentedControl(
                            options = listOf(
                                com.example.glass.GlassStyle.CLEAR,
                                com.example.glass.GlassStyle.REGULAR,
                                com.example.glass.GlassStyle.TINTED
                            ),
                            selectedOption = settings.glassStyle,
                            onOptionSelected = { style -> onUpdateSettings { it.copy(glassStyle = style) } },
                            labelProvider = {
                                when (it) {
                                    com.example.glass.GlassStyle.CLEAR -> strings.glassClear
                                    com.example.glass.GlassStyle.REGULAR -> strings.glassRegular
                                    com.example.glass.GlassStyle.TINTED -> strings.glassTinted
                                }
                            },
                            testTag = "settings_glass_style_segment"
                        )

                        Spacer(Modifier.height(Spacing.lg))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(strings.glassIntensity, style = MaterialTheme.typography.bodyLarge, color = glassColors.textPrimary)
                            Text("${(settings.glassIntensity * 100).toInt()}%", style = MaterialTheme.typography.labelLarge, color = glassColors.accentBlue)
                        }
                        GlassSlider(
                            value = settings.glassIntensity,
                            onValueChange = { v -> onUpdateSettings { it.copy(glassIntensity = v) } },
                            valueRange = 0.2f..1f,
                            testTag = "settings_glass_intensity_slider"
                        )

                        SettingsToggleRow(
                            title = strings.reduceGlass,
                            checked = settings.reduceGlass,
                            onCheckedChange = { c -> onUpdateSettings { it.copy(reduceGlass = c) } },
                            testTag = "settings_reduce_glass_switch"
                        )
                    }
                }

                // 2. LIBRARY
                item {
                    SettingsGroupCard(title = strings.librarySection) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Default Grid Density", style = MaterialTheme.typography.bodyLarge, color = glassColors.textPrimary)
                            GlassStepper(
                                value = settings.defaultGridColumns,
                                onValueChange = { c -> onUpdateSettings { it.copy(defaultGridColumns = c) } },
                                range = 2..6
                            )
                        }
                        SettingsDivider()
                        SettingsToggleRow(
                            title = "Square Photo Grid",
                            checked = settings.squareGrid,
                            onCheckedChange = { c -> onUpdateSettings { it.copy(squareGrid = c) } }
                        )
                        SettingsToggleRow(
                            title = "Show Media Badges",
                            checked = settings.showBadges,
                            onCheckedChange = { c -> onUpdateSettings { it.copy(showBadges = c) } }
                        )
                        SettingsToggleRow(
                            title = "Open at Latest Photos",
                            checked = settings.openAtLatest,
                            onCheckedChange = { c -> onUpdateSettings { it.copy(openAtLatest = c) } }
                        )
                        SettingsToggleRow(
                            title = "Show Location in Headers",
                            checked = settings.showLocationHeaders,
                            onCheckedChange = { c -> onUpdateSettings { it.copy(showLocationHeaders = c) } }
                        )
                        Spacer(Modifier.height(Spacing.sm))
                        GlassPickerRow(
                            options = listOf(strings.sortByDateCaptured, strings.sortByRecentlyAdded),
                            selectedIndex = if (settings.sortOrder == LibrarySortOrder.DATE_CAPTURED) 0 else 1,
                            onSelect = { idx ->
                                onUpdateSettings {
                                    it.copy(
                                        sortOrder = if (idx == 0) LibrarySortOrder.DATE_CAPTURED else LibrarySortOrder.RECENTLY_ADDED
                                    )
                                }
                            }
                        )
                    }
                }

                // 3. VIEWER & VIDEO (includes Spatial Photo control!)
                item {
                    SettingsGroupCard(title = strings.viewerVideoSection) {
                        SettingsToggleRow(
                            title = strings.spatialToggleTitle,
                            checked = settings.showSpatialPhotoControl,
                            onCheckedChange = { c -> onUpdateSettings { it.copy(showSpatialPhotoControl = c) } },
                            testTag = "settings_spatial_toggle"
                        )
                        SettingsToggleRow(
                            title = "Autoplay Motion Photos",
                            checked = settings.autoplayMotionPhotos,
                            onCheckedChange = { c -> onUpdateSettings { it.copy(autoplayMotionPhotos = c) } }
                        )
                        SettingsToggleRow(
                            title = "Autoplay Videos",
                            checked = settings.autoplayVideos,
                            onCheckedChange = { c -> onUpdateSettings { it.copy(autoplayVideos = c) } }
                        )
                        SettingsToggleRow(
                            title = "Loop Videos",
                            checked = settings.loopVideos,
                            onCheckedChange = { c -> onUpdateSettings { it.copy(loopVideos = c) } }
                        )
                        SettingsToggleRow(
                            title = "Keep Screen On",
                            checked = settings.keepScreenOn,
                            onCheckedChange = { c -> onUpdateSettings { it.copy(keepScreenOn = c) } }
                        )
                        SettingsToggleRow(
                            title = "Swipe Up for Info",
                            checked = settings.swipeUpForInfo,
                            onCheckedChange = { c -> onUpdateSettings { it.copy(swipeUpForInfo = c) } }
                        )
                        Spacer(Modifier.height(Spacing.sm))
                        Text("Default Playback Speed", style = MaterialTheme.typography.labelLarge, color = glassColors.textSecondary)
                        Spacer(Modifier.height(Spacing.xs))
                        val speeds = listOf(0.5f, 1.0f, 1.5f, 2.0f)
                        GlassPickerRow(
                            options = speeds.map { "${it}x" },
                            selectedIndex = speeds.indexOf(settings.defaultPlaybackSpeed).coerceAtLeast(1),
                            onSelect = { idx -> onUpdateSettings { it.copy(defaultPlaybackSpeed = speeds[idx]) } }
                        )
                    }
                }

                // 4. PRIVACY & SECURITY
                item {
                    SettingsGroupCard(title = strings.privacySecuritySection) {
                        SettingsToggleRow(
                            title = "Lock Hidden Album",
                            checked = settings.hiddenLockEnabled,
                            onCheckedChange = { c -> onUpdateSettings { it.copy(hiddenLockEnabled = c) } }
                        )
                        SettingsToggleRow(
                            title = "Biometric Unlock",
                            checked = settings.biometricUnlockEnabled,
                            onCheckedChange = { c -> onUpdateSettings { it.copy(biometricUnlockEnabled = c) } }
                        )
                        SettingsToggleRow(
                            title = "Hide Previews in Recents",
                            checked = settings.hidePreviewsInRecents,
                            onCheckedChange = { c -> onUpdateSettings { it.copy(hidePreviewsInRecents = c) } }
                        )
                        SettingsToggleRow(
                            title = "Secure Screen (Block Screenshots)",
                            checked = settings.secureScreen,
                            onCheckedChange = { c -> onUpdateSettings { it.copy(secureScreen = c) } }
                        )
                        SettingsActionRow(
                            title = "Set / Change Vault PIN",
                            subtitle = "Current: ••••",
                            onClick = {
                                pinInput = settings.hiddenVaultPin
                                showPinDialog = true
                            }
                        )
                    }
                }

                // 5. STORAGE
                item {
                    SettingsGroupCard(title = strings.storageSection) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("Thumbnail Cache", style = MaterialTheme.typography.bodyLarge, color = glassColors.textPrimary)
                                Text(cacheSizeLabel, style = MaterialTheme.typography.bodySmall, color = glassColors.textSecondary)
                            }
                            GlassButton(onClick = onClearCache, variant = GlassVariant.CLEAR) {
                                Text("Clear")
                            }
                        }
                        SettingsDivider()
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Trash Retention", style = MaterialTheme.typography.bodyLarge, color = glassColors.textPrimary)
                            GlassStepper(
                                value = settings.trashRetentionDays,
                                onValueChange = { d -> onUpdateSettings { it.copy(trashRetentionDays = d) } },
                                range = 7..90,
                                unitSuffix = "d"
                            )
                        }
                        SettingsDivider()
                        SettingsActionRow(title = strings.duplicatesTitle, onClick = onOpenDuplicates)
                        SettingsActionRow(title = strings.largeFilesTitle, onClick = onOpenLargeFiles)
                    }
                }

                // 6. LANGUAGE (Instant Bangla / English Switch)
                item {
                    SettingsGroupCard(title = strings.languageSection) {
                        GlassSegmentedControl(
                            options = listOf(AppLanguage.ENGLISH, AppLanguage.BANGLA),
                            selectedOption = settings.language,
                            onOptionSelected = { lang -> onUpdateSettings { it.copy(language = lang) } },
                            labelProvider = { it.displayName },
                            testTag = "settings_language_segment"
                        )
                    }
                }

                // 7. ACCESSIBILITY
                item {
                    SettingsGroupCard(title = strings.accessibilitySection) {
                        SettingsToggleRow(
                            title = "Reduce Motion",
                            checked = settings.reduceMotion,
                            onCheckedChange = { c -> onUpdateSettings { it.copy(reduceMotion = c) } }
                        )
                        SettingsToggleRow(
                            title = "Haptic Feedback",
                            checked = settings.hapticsEnabled,
                            onCheckedChange = { c -> onUpdateSettings { it.copy(hapticsEnabled = c) } }
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Haptic Strength", style = MaterialTheme.typography.bodyMedium, color = glassColors.textSecondary)
                            Text("${(settings.hapticStrength * 100).toInt()}%", style = MaterialTheme.typography.labelMedium, color = glassColors.accentBlue)
                        }
                        GlassSlider(
                            value = settings.hapticStrength,
                            onValueChange = { v -> onUpdateSettings { it.copy(hapticStrength = v) } },
                            enabled = settings.hapticsEnabled
                        )
                        SettingsToggleRow(
                            title = "Larger Touch Targets",
                            checked = settings.largerTouchTargets,
                            onCheckedChange = { c -> onUpdateSettings { it.copy(largerTouchTargets = c) } }
                        )
                        SettingsToggleRow(
                            title = "High Contrast Glass",
                            checked = settings.highContrastGlass,
                            onCheckedChange = { c -> onUpdateSettings { it.copy(highContrastGlass = c) } }
                        )
                    }
                }

                // 8. PERMISSIONS
                item {
                    SettingsGroupCard(title = strings.permissionsSection) {
                        val stateLabel = when (permissionState) {
                            MediaPermissionState.FULL -> "Full Photo & Video Access"
                            MediaPermissionState.PARTIAL -> "Limited / Selected Photos Only"
                            MediaPermissionState.DENIED, MediaPermissionState.UNKNOWN -> "Denied (Showcase Mode Active)"
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Media Access", style = MaterialTheme.typography.bodyLarge, color = glassColors.textPrimary)
                                Text(stateLabel, style = MaterialTheme.typography.bodySmall, color = glassColors.textSecondary)
                            }
                            GlassButton(onClick = onRequestPermissions, variant = GlassVariant.TINTED) {
                                Text("Change")
                            }
                        }
                    }
                }

                // 9. ABOUT (Single Source of Truth: BuildConfig.VERSION_NAME = 1.0.7)
                item {
                    SettingsGroupCard(title = strings.aboutSection) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Image(
                                painter = painterResource(id = R.drawable.glassly_icon_fg),
                                contentDescription = "Glassly Logo",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .size(56.dp)
                                    .clip(RoundedCornerShape(14.dp))
                            )
                            Spacer(Modifier.width(Spacing.md))
                            Column {
                                Text(strings.appName, style = MaterialTheme.typography.titleLarge, color = glassColors.textPrimary)
                                Text(
                                    text = "Version ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = glassColors.textSecondary,
                                    modifier = Modifier.testTag("settings_about_version")
                                )
                            }
                        }
                        SettingsDivider()
                        SettingsActionRow(title = "Open-Source Licenses", onClick = { showLicenseSheet = true })
                        SettingsActionRow(title = "Privacy Policy", onClick = { onShowToast("100% offline on-device processing — zero telemetry.") })
                        SettingsActionRow(title = "Send Feedback", onClick = { onShowToast("Thank you for shaping Glassly ${BuildConfig.VERSION_NAME}!") })
                        SettingsActionRow(title = "Rate Glassly", onClick = { onShowToast("Thank you for rating Glassly!") })
                        if (BuildConfig.DEBUG) {
                            SettingsDivider()
                            SettingsActionRow(
                                title = strings.glassGalleryDebug,
                                subtitle = "Inspect all Liquid Glass components & states",
                                onClick = onOpenGlassGalleryDebug
                            )
                        }
                    }
                }

                // 10. RESET ALL SETTINGS
                item {
                    GlassButton(
                        onClick = { showResetConfirm = true },
                        modifier = Modifier
                            .fillMaxWidth()
                            .widthIn(max = 600.dp),
                        isDestructive = true,
                        testTag = "settings_reset_all_button"
                    ) {
                        Icon(Icons.Rounded.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(Spacing.sm))
                        Text(strings.resetAllSettings, style = MaterialTheme.typography.labelLarge)
                    }
                }
            }

            // Floating Glass Collapsible Top Bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = Spacing.lg, vertical = Spacing.sm)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    GlassIconButton(
                        icon = Icons.AutoMirrored.Rounded.ArrowBack,
                        contentDescription = "Back",
                        onClick = onBack,
                        testTag = "settings_back_button"
                    )
                    if (isHeaderCollapsed) {
                        Box(
                            modifier = Modifier
                                .glassElevationShadow(10.dp, CircleShape)
                                .liquidGlass(shape = CircleShape, cornerRadius = 999.dp, variant = GlassVariant.REGULAR)
                                .padding(horizontal = 18.dp, vertical = 8.dp)
                        ) {
                            Text(
                                text = strings.settingsTitle,
                                style = MaterialTheme.typography.titleMedium,
                                color = glassColors.textPrimary
                            )
                        }
                    } else {
                        Spacer(Modifier.width(48.dp))
                    }
                    Spacer(Modifier.width(48.dp))
                }
            }

            // Expanding Reset Confirmation Alert
            GlassAlertDialog(
                visible = showResetConfirm,
                title = strings.resetAllConfirmTitle,
                message = strings.resetAllConfirmBody,
                confirmLabel = strings.resetAction,
                cancelLabel = strings.cancel,
                isDestructive = true,
                origin = TransformOrigin(0.5f, 0.9f),
                onConfirm = {
                    showResetConfirm = false
                    onResetAllSettings()
                },
                onDismiss = { showResetConfirm = false }
            )

            // Vault PIN Editor Dialog
            if (showPinDialog) {
                GlassAlertDialog(
                    visible = true,
                    title = "Vault PIN Updated",
                    message = "Default PIN is 2626. Toggle PIN lock or enter a 4-digit code in the Hidden Vault keypad.",
                    confirmLabel = "Set 2626",
                    cancelLabel = strings.cancel,
                    onConfirm = {
                        onUpdateSettings { it.copy(hiddenVaultPin = "2626") }
                        showPinDialog = false
                        onShowToast("Vault PIN set to 2626")
                    },
                    onDismiss = { showPinDialog = false }
                )
            }

            if (showLicenseSheet) {
                GlassAlertDialog(
                    visible = true,
                    title = "Open-Source Licenses",
                    message = "Built with Jetpack Compose, AndroidX Media3, Coil, Room, DataStore, ExifInterface, and AndroidX Biometric.",
                    confirmLabel = strings.done,
                    cancelLabel = strings.cancel,
                    onConfirm = { showLicenseSheet = false },
                    onDismiss = { showLicenseSheet = false }
                )
            }
        }
    }
}

@Composable
private fun SettingsGroupCard(
    title: String,
    content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit
) {
    val glassColors = LocalGlassColors.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .widthIn(max = 600.dp)
    ) {
        Text(
            text = title.uppercase(),
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
            color = glassColors.textSecondary,
            modifier = Modifier.padding(start = Spacing.md, bottom = Spacing.xs)
        )
        GlassCard(
            modifier = Modifier.fillMaxWidth(),
            cornerRadius = CornerRadius.lg,
            variant = GlassVariant.REGULAR,
            content = content
        )
    }
}

@Composable
private fun SettingsToggleRow(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    testTag: String? = null
) {
    val glassColors = LocalGlassColors.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge,
            color = glassColors.textPrimary,
            modifier = Modifier.weight(1f).padding(end = Spacing.sm)
        )
        GlassSwitch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            contentDescription = title,
            testTag = testTag
        )
    }
}

@Composable
private fun SettingsActionRow(
    title: String,
    onClick: () -> Unit,
    subtitle: String? = null
) {
    val glassColors = LocalGlassColors.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(CornerRadius.sm))
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge, color = glassColors.textPrimary)
            if (subtitle != null) {
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = glassColors.textSecondary)
            }
        }
        Icon(
            imageVector = Icons.Rounded.ChevronRight,
            contentDescription = null,
            tint = glassColors.textTertiary
        )
    }
}

@Composable
private fun SettingsDivider() {
    val glassColors = LocalGlassColors.current
    HorizontalDivider(
        modifier = Modifier.padding(vertical = Spacing.sm),
        color = glassColors.divider
    )
}
