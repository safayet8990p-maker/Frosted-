package com.example.data.model

import androidx.annotation.DrawableRes
import androidx.compose.runtime.Immutable
import com.example.glass.GlassStyle
import com.example.ui.theme.AccentPreset

enum class MediaType {
    PHOTO,
    VIDEO,
    RAW,
    GIF,
    MOTION_PHOTO,
    SCREENSHOT
}

enum class TimeSegment {
    YEARS,
    MONTHS,
    DAYS,
    ALL
}

enum class LibrarySortOrder {
    RECENTLY_ADDED,
    DATE_CAPTURED
}

enum class LibraryFilterType {
    FAVORITES,
    EDITED,
    PHOTOS,
    VIDEOS,
    SCREENSHOTS,
    SHARED_WITH_YOU,
    NOT_IN_ALBUM
}

enum class CollectionTileLayout {
    TOP_ROW_EMPHASIS,
    DENSE_EQUAL,
    LARGE_SQUARE
}

enum class CollectionSectionId(val title: String) {
    PINNED("Pinned"),
    SHARED_ALBUMS("Shared Albums"),
    MEDIA_TYPES("Media Types"),
    PEOPLE_PETS("People & Pets"),
    UTILITIES("Utilities"),
    MEMORIES("Memories"),
    ALBUMS("Albums"),
    TRIPS("Trips"),
    FEATURED_PHOTOS("Featured Photos"),
    WALLPAPER_SUGGESTIONS("Wallpaper Suggestions"),
    RECENT_DAYS("Recent Days")
}

enum class AppThemeMode {
    SYSTEM,
    LIGHT,
    DARK
}

enum class AppLanguage(val code: String, val displayName: String) {
    ENGLISH("en", "English"),
    BANGLA("bn", "বাংলা")
}

enum class MediaPermissionState {
    UNKNOWN,
    FULL,
    PARTIAL,
    DENIED
}

enum class FilterPreset(val label: String) {
    ORIGINAL("Original"),
    VIVID("Vivid"),
    DRAMATIC("Dramatic"),
    MONO("Mono"),
    SILVERTONE("Silvertone"),
    WARM("Warm"),
    COOL("Cool"),
    NOIR("Noir")
}

enum class CropAspectRatio(val label: String, val ratio: Float?) {
    FREE("Free", null),
    SQUARE("1:1", 1f),
    FOUR_THREE("4:3", 4f / 3f),
    SIXTEEN_NINE("16:9", 16f / 9f),
    THREE_FOUR("3:4", 3f / 4f)
}

@Immutable
data class LoupeAnnotation(
    val id: String,
    val centerX: Float = 0.5f,
    val centerY: Float = 0.48f,
    val radiusFraction: Float = 0.22f,
    val zoomLevel: Float = 2.0f, // 1.2f .. 3.5f controlled by green rim handle along 12..3 o'clock arc
    val strokeColorArgb: Long? = 0xFFFFFFFFL,
    val strokeWidthPt: Int = 3
)

@Immutable
data class DrawStroke(
    val points: List<Pair<Float, Float>>,
    val colorArgb: Long,
    val strokeWidthPx: Float,
    val isHighlighter: Boolean = false
)

@Immutable
data class TextOverlay(
    val id: String,
    val text: String,
    val x: Float = 0.5f,
    val y: Float = 0.5f,
    val colorArgb: Long = 0xFFFFFFFFL
)

@Immutable
data class PhotoEditState(
    val brightness: Float = 0f,       // -1f..1f
    val contrast: Float = 1f,         // 0.5f..1.8f
    val saturation: Float = 1f,       // 0f..2f
    val warmth: Float = 0f,           // -1f..1f
    val sharpness: Float = 0f,        // 0f..1f
    val vignette: Float = 0f,         // 0f..1f
    val rotationDegrees: Int = 0,     // 0, 90, 180, 270
    val straightenDegrees: Float = 0f,// -45f..45f
    val flipHorizontal: Boolean = false,
    val flipVertical: Boolean = false,
    val cropRatio: CropAspectRatio = CropAspectRatio.FREE,
    val cropInset: Float = 0f,        // 0f..0.25f
    val filterPreset: FilterPreset = FilterPreset.ORIGINAL,
    val strokes: List<DrawStroke> = emptyList(),
    val textOverlays: List<TextOverlay> = emptyList(),
    val loupes: List<LoupeAnnotation> = emptyList(),
    val imageDescription: String = ""
) {
    val hasChanges: Boolean
        get() = brightness != 0f || contrast != 1f || saturation != 1f ||
            warmth != 0f || sharpness != 0f || vignette != 0f ||
            rotationDegrees != 0 || straightenDegrees != 0f ||
            flipHorizontal || flipVertical ||
            cropRatio != CropAspectRatio.FREE || cropInset != 0f ||
            filterPreset != FilterPreset.ORIGINAL ||
            strokes.isNotEmpty() || textOverlays.isNotEmpty() ||
            loupes.isNotEmpty() || imageDescription.isNotEmpty()
}

@Immutable
data class ExifMetadata(
    val cameraModel: String = "Glassly Optics",
    val lensInfo: String = "24mm f/1.8",
    val iso: String = "ISO 100",
    val shutterSpeed: String = "1/250s",
    val aperture: String = "f/1.8"
)

@Immutable
data class MediaItem(
    val id: String,
    val mediaStoreId: Long?,
    val uriString: String,
    @DrawableRes val sampleDrawableRes: Int? = null,
    val displayName: String,
    val mimeType: String,
    val mediaType: MediaType,
    val dateTakenMillis: Long,
    val dateAddedMillis: Long,
    val durationMs: Long = 0L,
    val width: Int = 1200,
    val height: Int = 900,
    val sizeBytes: Long = 2_400_000L,
    val bucketId: String = "camera",
    val bucketName: String = "Camera Roll",
    val filePath: String = "/DCIM/Camera",
    val locationName: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val isFavorite: Boolean = false,
    val isEdited: Boolean = false,
    val isSharedWithYou: Boolean = false,
    val isScreenshot: Boolean = false,
    val isSelfie: Boolean = false,
    val isHidden: Boolean = false,
    val isTrashed: Boolean = false,
    val trashedAtMillis: Long? = null,
    val isSampleFallback: Boolean = false,
    val editState: PhotoEditState = PhotoEditState(),
    val exif: ExifMetadata = ExifMetadata()
) {
    val aspectRatio: Float
        get() = if (height > 0) (width.toFloat() / height.toFloat()).coerceIn(0.45f, 2.2f) else 1f
}

@Immutable
data class AlbumModel(
    val id: String,
    val title: String,
    val coverUri: String?,
    @DrawableRes val coverSampleRes: Int? = null,
    val itemCount: Int,
    val isCustom: Boolean = false,
    val isPinned: Boolean = false,
    val isShared: Boolean = false,
    val mediaIds: List<String> = emptyList()
)

@Immutable
data class PinnedEntry(
    val id: String,
    val title: String,
    val subtitle: String,
    val iconKey: String,
    val coverUri: String? = null,
    @DrawableRes val coverSampleRes: Int? = null
)

@Immutable
data class SettingsState(
    val themeMode: AppThemeMode = AppThemeMode.SYSTEM,
    val accentPreset: AccentPreset = AccentPreset.FROSTED_GLASS,
    val glassStyle: GlassStyle = GlassStyle.CLEAR,
    val glassIntensity: Float = 0.85f,
    val reduceGlass: Boolean = false,
    val defaultGridColumns: Int = 3,
    val squareGrid: Boolean = true,
    val showBadges: Boolean = true,
    val sortOrder: LibrarySortOrder = LibrarySortOrder.DATE_CAPTURED,
    val openAtLatest: Boolean = true,
    val showLocationHeaders: Boolean = true,
    val includeScreenshotsInLibrary: Boolean = true,
    val includeSharedWithYouInLibrary: Boolean = true,
    val autoplayMotionPhotos: Boolean = true,
    val autoplayVideos: Boolean = true,
    val loopVideos: Boolean = true,
    val defaultPlaybackSpeed: Float = 1.0f,
    val keepScreenOn: Boolean = true,
    val swipeUpForInfo: Boolean = true,
    val showSpatialPhotoControl: Boolean = true,
    val editorAppearanceOverride: AppThemeMode = AppThemeMode.SYSTEM,
    val hiddenLockEnabled: Boolean = true,
    val biometricUnlockEnabled: Boolean = true,
    val hiddenVaultPin: String = "2626",
    val hidePreviewsInRecents: Boolean = false,
    val secureScreen: Boolean = false,
    val trashRetentionDays: Int = 30,
    val language: AppLanguage = AppLanguage.ENGLISH,
    val reduceMotion: Boolean = false,
    val hapticsEnabled: Boolean = true,
    val hapticStrength: Float = 0.8f,
    val largerTouchTargets: Boolean = false,
    val highContrastGlass: Boolean = false,
    val onboardingCompleted: Boolean = false,
    val collectionTileLayout: CollectionTileLayout = CollectionTileLayout.TOP_ROW_EMPHASIS,
    val sectionOrder: List<CollectionSectionId> = CollectionSectionId.entries,
    val collapsedSections: Set<CollectionSectionId> = emptySet(),
    val pinnedIds: List<String> = listOf("favorites", "recently_saved", "videos", "screenshots")
)

sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Empty(val message: String) : UiState<Nothing>
    data class Error(val message: String) : UiState<Nothing>
}
