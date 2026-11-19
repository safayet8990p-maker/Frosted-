package com.example.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.data.model.AppLanguage
import com.example.data.model.AppThemeMode
import com.example.data.model.CollectionSectionId
import com.example.data.model.CollectionTileLayout
import com.example.data.model.LibrarySortOrder
import com.example.data.model.SettingsState
import com.example.glass.GlassStyle
import com.example.ui.theme.AccentPreset
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.glasslySettingsStore: DataStore<Preferences> by preferencesDataStore(name = "glassly_settings")

class SettingsDataStore(private val context: Context) {

    private object Keys {
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val ACCENT_PRESET = stringPreferencesKey("accent_preset")
        val GLASS_STYLE = stringPreferencesKey("glass_style")
        val GLASS_INTENSITY = floatPreferencesKey("glass_intensity")
        val REDUCE_GLASS = booleanPreferencesKey("reduce_glass")
        val GRID_COLUMNS = intPreferencesKey("grid_columns")
        val SQUARE_GRID = booleanPreferencesKey("square_grid")
        val SHOW_BADGES = booleanPreferencesKey("show_badges")
        val SORT_ORDER = stringPreferencesKey("sort_order")
        val OPEN_AT_LATEST = booleanPreferencesKey("open_at_latest")
        val SHOW_LOCATION_HEADERS = booleanPreferencesKey("show_location_headers")
        val INCLUDE_SCREENSHOTS = booleanPreferencesKey("include_screenshots")
        val INCLUDE_SHARED = booleanPreferencesKey("include_shared")
        val AUTOPLAY_MOTION = booleanPreferencesKey("autoplay_motion")
        val AUTOPLAY_VIDEOS = booleanPreferencesKey("autoplay_videos")
        val LOOP_VIDEOS = booleanPreferencesKey("loop_videos")
        val PLAYBACK_SPEED = floatPreferencesKey("playback_speed")
        val KEEP_SCREEN_ON = booleanPreferencesKey("keep_screen_on")
        val SWIPE_UP_INFO = booleanPreferencesKey("swipe_up_info")
        val SPATIAL_CONTROL = booleanPreferencesKey("spatial_control")
        val EDITOR_APPEARANCE = stringPreferencesKey("editor_appearance")
        val HIDDEN_LOCK = booleanPreferencesKey("hidden_lock")
        val BIOMETRIC_UNLOCK = booleanPreferencesKey("biometric_unlock")
        val HIDDEN_PIN = stringPreferencesKey("hidden_pin")
        val HIDE_RECENTS_PREVIEW = booleanPreferencesKey("hide_recents_preview")
        val SECURE_SCREEN = booleanPreferencesKey("secure_screen")
        val TRASH_DAYS = intPreferencesKey("trash_days")
        val LANGUAGE = stringPreferencesKey("language")
        val REDUCE_MOTION = booleanPreferencesKey("reduce_motion")
        val HAPTICS_ENABLED = booleanPreferencesKey("haptics_enabled")
        val HAPTIC_STRENGTH = floatPreferencesKey("haptic_strength")
        val LARGER_TOUCH = booleanPreferencesKey("larger_touch")
        val HIGH_CONTRAST = booleanPreferencesKey("high_contrast")
        val ONBOARDING_DONE = booleanPreferencesKey("onboarding_done")
        val COLLECTION_LAYOUT = stringPreferencesKey("collection_layout")
        val SECTION_ORDER = stringPreferencesKey("section_order")
        val COLLAPSED_SECTIONS = stringPreferencesKey("collapsed_sections")
        val PINNED_IDS = stringPreferencesKey("pinned_ids")
    }

    val settingsFlow: Flow<SettingsState> = context.glasslySettingsStore.data.map { prefs ->
        val defaultState = SettingsState()
        SettingsState(
            themeMode = enumOrDefault(prefs[Keys.THEME_MODE], defaultState.themeMode),
            accentPreset = enumOrDefault(prefs[Keys.ACCENT_PRESET], defaultState.accentPreset),
            glassStyle = enumOrDefault(prefs[Keys.GLASS_STYLE], defaultState.glassStyle),
            glassIntensity = prefs[Keys.GLASS_INTENSITY] ?: defaultState.glassIntensity,
            reduceGlass = prefs[Keys.REDUCE_GLASS] ?: defaultState.reduceGlass,
            defaultGridColumns = prefs[Keys.GRID_COLUMNS] ?: defaultState.defaultGridColumns,
            squareGrid = prefs[Keys.SQUARE_GRID] ?: defaultState.squareGrid,
            showBadges = prefs[Keys.SHOW_BADGES] ?: defaultState.showBadges,
            sortOrder = enumOrDefault(prefs[Keys.SORT_ORDER], defaultState.sortOrder),
            openAtLatest = prefs[Keys.OPEN_AT_LATEST] ?: defaultState.openAtLatest,
            showLocationHeaders = prefs[Keys.SHOW_LOCATION_HEADERS] ?: defaultState.showLocationHeaders,
            includeScreenshotsInLibrary = prefs[Keys.INCLUDE_SCREENSHOTS] ?: defaultState.includeScreenshotsInLibrary,
            includeSharedWithYouInLibrary = prefs[Keys.INCLUDE_SHARED] ?: defaultState.includeSharedWithYouInLibrary,
            autoplayMotionPhotos = prefs[Keys.AUTOPLAY_MOTION] ?: defaultState.autoplayMotionPhotos,
            autoplayVideos = prefs[Keys.AUTOPLAY_VIDEOS] ?: defaultState.autoplayVideos,
            loopVideos = prefs[Keys.LOOP_VIDEOS] ?: defaultState.loopVideos,
            defaultPlaybackSpeed = prefs[Keys.PLAYBACK_SPEED] ?: defaultState.defaultPlaybackSpeed,
            keepScreenOn = prefs[Keys.KEEP_SCREEN_ON] ?: defaultState.keepScreenOn,
            swipeUpForInfo = prefs[Keys.SWIPE_UP_INFO] ?: defaultState.swipeUpForInfo,
            showSpatialPhotoControl = prefs[Keys.SPATIAL_CONTROL] ?: defaultState.showSpatialPhotoControl,
            editorAppearanceOverride = enumOrDefault(prefs[Keys.EDITOR_APPEARANCE], defaultState.editorAppearanceOverride),
            hiddenLockEnabled = prefs[Keys.HIDDEN_LOCK] ?: defaultState.hiddenLockEnabled,
            biometricUnlockEnabled = prefs[Keys.BIOMETRIC_UNLOCK] ?: defaultState.biometricUnlockEnabled,
            hiddenVaultPin = prefs[Keys.HIDDEN_PIN] ?: defaultState.hiddenVaultPin,
            hidePreviewsInRecents = prefs[Keys.HIDE_RECENTS_PREVIEW] ?: defaultState.hidePreviewsInRecents,
            secureScreen = prefs[Keys.SECURE_SCREEN] ?: defaultState.secureScreen,
            trashRetentionDays = prefs[Keys.TRASH_DAYS] ?: defaultState.trashRetentionDays,
            language = enumOrDefault(prefs[Keys.LANGUAGE], defaultState.language),
            reduceMotion = prefs[Keys.REDUCE_MOTION] ?: defaultState.reduceMotion,
            hapticsEnabled = prefs[Keys.HAPTICS_ENABLED] ?: defaultState.hapticsEnabled,
            hapticStrength = prefs[Keys.HAPTIC_STRENGTH] ?: defaultState.hapticStrength,
            largerTouchTargets = prefs[Keys.LARGER_TOUCH] ?: defaultState.largerTouchTargets,
            highContrastGlass = prefs[Keys.HIGH_CONTRAST] ?: defaultState.highContrastGlass,
            onboardingCompleted = prefs[Keys.ONBOARDING_DONE] ?: defaultState.onboardingCompleted,
            collectionTileLayout = enumOrDefault(prefs[Keys.COLLECTION_LAYOUT], defaultState.collectionTileLayout),
            sectionOrder = prefs[Keys.SECTION_ORDER]
                ?.split(",")
                ?.mapNotNull { runCatching { CollectionSectionId.valueOf(it) }.getOrNull() }
                ?.takeIf { it.isNotEmpty() }
                ?: defaultState.sectionOrder,
            collapsedSections = prefs[Keys.COLLAPSED_SECTIONS]
                ?.split(",")
                ?.filter { it.isNotBlank() }
                ?.mapNotNull { runCatching { CollectionSectionId.valueOf(it) }.getOrNull() }
                ?.toSet()
                ?: defaultState.collapsedSections,
            pinnedIds = prefs[Keys.PINNED_IDS]
                ?.split(",")
                ?.filter { it.isNotBlank() }
                ?.takeIf { it.isNotEmpty() }
                ?: defaultState.pinnedIds
        )
    }

    suspend fun update(transform: (SettingsState) -> SettingsState) {
        context.glasslySettingsStore.edit { prefs ->
            val current = SettingsState(
                themeMode = enumOrDefault(prefs[Keys.THEME_MODE], AppThemeMode.SYSTEM),
                accentPreset = enumOrDefault(prefs[Keys.ACCENT_PRESET], AccentPreset.FROSTED_GLASS),
                glassStyle = enumOrDefault(prefs[Keys.GLASS_STYLE], GlassStyle.CLEAR),
                glassIntensity = prefs[Keys.GLASS_INTENSITY] ?: 0.85f,
                reduceGlass = prefs[Keys.REDUCE_GLASS] ?: false,
                defaultGridColumns = prefs[Keys.GRID_COLUMNS] ?: 3,
                squareGrid = prefs[Keys.SQUARE_GRID] ?: true,
                showBadges = prefs[Keys.SHOW_BADGES] ?: true,
                sortOrder = enumOrDefault(prefs[Keys.SORT_ORDER], LibrarySortOrder.DATE_CAPTURED),
                openAtLatest = prefs[Keys.OPEN_AT_LATEST] ?: true,
                showLocationHeaders = prefs[Keys.SHOW_LOCATION_HEADERS] ?: true,
                includeScreenshotsInLibrary = prefs[Keys.INCLUDE_SCREENSHOTS] ?: true,
                includeSharedWithYouInLibrary = prefs[Keys.INCLUDE_SHARED] ?: true,
                autoplayMotionPhotos = prefs[Keys.AUTOPLAY_MOTION] ?: true,
                autoplayVideos = prefs[Keys.AUTOPLAY_VIDEOS] ?: true,
                loopVideos = prefs[Keys.LOOP_VIDEOS] ?: true,
                defaultPlaybackSpeed = prefs[Keys.PLAYBACK_SPEED] ?: 1.0f,
                keepScreenOn = prefs[Keys.KEEP_SCREEN_ON] ?: true,
                swipeUpForInfo = prefs[Keys.SWIPE_UP_INFO] ?: true,
                showSpatialPhotoControl = prefs[Keys.SPATIAL_CONTROL] ?: true,
                editorAppearanceOverride = enumOrDefault(prefs[Keys.EDITOR_APPEARANCE], AppThemeMode.SYSTEM),
                hiddenLockEnabled = prefs[Keys.HIDDEN_LOCK] ?: true,
                biometricUnlockEnabled = prefs[Keys.BIOMETRIC_UNLOCK] ?: true,
                hiddenVaultPin = prefs[Keys.HIDDEN_PIN] ?: "2626",
                hidePreviewsInRecents = prefs[Keys.HIDE_RECENTS_PREVIEW] ?: false,
                secureScreen = prefs[Keys.SECURE_SCREEN] ?: false,
                trashRetentionDays = prefs[Keys.TRASH_DAYS] ?: 30,
                language = enumOrDefault(prefs[Keys.LANGUAGE], AppLanguage.ENGLISH),
                reduceMotion = prefs[Keys.REDUCE_MOTION] ?: false,
                hapticsEnabled = prefs[Keys.HAPTICS_ENABLED] ?: true,
                hapticStrength = prefs[Keys.HAPTIC_STRENGTH] ?: 0.8f,
                largerTouchTargets = prefs[Keys.LARGER_TOUCH] ?: false,
                highContrastGlass = prefs[Keys.HIGH_CONTRAST] ?: false,
                onboardingCompleted = prefs[Keys.ONBOARDING_DONE] ?: false,
                collectionTileLayout = enumOrDefault(prefs[Keys.COLLECTION_LAYOUT], CollectionTileLayout.TOP_ROW_EMPHASIS),
                sectionOrder = prefs[Keys.SECTION_ORDER]
                    ?.split(",")
                    ?.mapNotNull { runCatching { CollectionSectionId.valueOf(it) }.getOrNull() }
                    ?.takeIf { it.isNotEmpty() }
                    ?: CollectionSectionId.entries,
                collapsedSections = prefs[Keys.COLLAPSED_SECTIONS]
                    ?.split(",")
                    ?.filter { it.isNotBlank() }
                    ?.mapNotNull { runCatching { CollectionSectionId.valueOf(it) }.getOrNull() }
                    ?.toSet()
                    ?: emptySet(),
                pinnedIds = prefs[Keys.PINNED_IDS]
                    ?.split(",")
                    ?.filter { it.isNotBlank() }
                    ?.takeIf { it.isNotEmpty() }
                    ?: listOf("favorites", "recently_saved", "videos", "screenshots")
            )
            val next = transform(current)
            prefs[Keys.THEME_MODE] = next.themeMode.name
            prefs[Keys.ACCENT_PRESET] = next.accentPreset.name
            prefs[Keys.GLASS_STYLE] = next.glassStyle.name
            prefs[Keys.GLASS_INTENSITY] = next.glassIntensity
            prefs[Keys.REDUCE_GLASS] = next.reduceGlass
            prefs[Keys.GRID_COLUMNS] = next.defaultGridColumns
            prefs[Keys.SQUARE_GRID] = next.squareGrid
            prefs[Keys.SHOW_BADGES] = next.showBadges
            prefs[Keys.SORT_ORDER] = next.sortOrder.name
            prefs[Keys.OPEN_AT_LATEST] = next.openAtLatest
            prefs[Keys.SHOW_LOCATION_HEADERS] = next.showLocationHeaders
            prefs[Keys.INCLUDE_SCREENSHOTS] = next.includeScreenshotsInLibrary
            prefs[Keys.INCLUDE_SHARED] = next.includeSharedWithYouInLibrary
            prefs[Keys.AUTOPLAY_MOTION] = next.autoplayMotionPhotos
            prefs[Keys.AUTOPLAY_VIDEOS] = next.autoplayVideos
            prefs[Keys.LOOP_VIDEOS] = next.loopVideos
            prefs[Keys.PLAYBACK_SPEED] = next.defaultPlaybackSpeed
            prefs[Keys.KEEP_SCREEN_ON] = next.keepScreenOn
            prefs[Keys.SWIPE_UP_INFO] = next.swipeUpForInfo
            prefs[Keys.SPATIAL_CONTROL] = next.showSpatialPhotoControl
            prefs[Keys.EDITOR_APPEARANCE] = next.editorAppearanceOverride.name
            prefs[Keys.HIDDEN_LOCK] = next.hiddenLockEnabled
            prefs[Keys.BIOMETRIC_UNLOCK] = next.biometricUnlockEnabled
            prefs[Keys.HIDDEN_PIN] = next.hiddenVaultPin
            prefs[Keys.HIDE_RECENTS_PREVIEW] = next.hidePreviewsInRecents
            prefs[Keys.SECURE_SCREEN] = next.secureScreen
            prefs[Keys.TRASH_DAYS] = next.trashRetentionDays
            prefs[Keys.LANGUAGE] = next.language.name
            prefs[Keys.REDUCE_MOTION] = next.reduceMotion
            prefs[Keys.HAPTICS_ENABLED] = next.hapticsEnabled
            prefs[Keys.HAPTIC_STRENGTH] = next.hapticStrength
            prefs[Keys.LARGER_TOUCH] = next.largerTouchTargets
            prefs[Keys.HIGH_CONTRAST] = next.highContrastGlass
            prefs[Keys.ONBOARDING_DONE] = next.onboardingCompleted
            prefs[Keys.COLLECTION_LAYOUT] = next.collectionTileLayout.name
            prefs[Keys.SECTION_ORDER] = next.sectionOrder.joinToString(",") { it.name }
            prefs[Keys.COLLAPSED_SECTIONS] = next.collapsedSections.joinToString(",") { it.name }
            prefs[Keys.PINNED_IDS] = next.pinnedIds.joinToString(",")
        }
    }

    suspend fun resetAllSettings() {
        val onboardingDone = true
        context.glasslySettingsStore.edit { prefs ->
            prefs.clear()
            prefs[Keys.ONBOARDING_DONE] = onboardingDone
        }
    }

    private inline fun <reified T : Enum<T>> enumOrDefault(raw: String?, default: T): T {
        if (raw == null) return default
        return runCatching { enumValueOf<T>(raw) }.getOrDefault(default)
    }
}
