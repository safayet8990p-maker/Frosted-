package com.example

import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.EditRecipeCodec
import com.example.data.local.GlasslyDatabase
import com.example.data.model.CollectionTileLayout
import com.example.data.model.FilterPreset
import com.example.data.model.LoupeAnnotation
import com.example.data.model.MediaPermissionState
import com.example.data.model.MediaType
import com.example.data.model.PhotoEditState
import com.example.data.model.SettingsState
import com.example.data.model.TimeSegment
import com.example.data.model.UiState
import com.example.data.repository.MediaRepository
import com.example.glass.GlassConfig
import com.example.glass.GlassStyle
import com.example.glass.LocalGlassConfig
import com.example.glass.glassBackdropSource
import com.example.ui.i18n.BanglaStrings
import com.example.ui.i18n.EnglishStrings
import com.example.ui.i18n.LocalAppStrings
import com.example.ui.screens.CollectionsScreen
import com.example.ui.screens.GlassGalleryDebugScreen
import com.example.ui.screens.LibraryScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.theme.GlasslyTheme
import com.example.viewmodel.LibraryGroup
import com.example.viewmodel.SmartCollectionSummary
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], qualifiers = "w411dp-h915dp")
class GlasslyFeaturesTest {

    @get:Rule
    val composeRule = createComposeRule()

    private lateinit var db: GlasslyDatabase
    private lateinit var repo: MediaRepository

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, GlasslyDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repo = MediaRepository(context, db.glasslyDao())
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun `edit recipe codec round-trips loupe zoom radius and filters`() {
        val original = PhotoEditState(
            brightness = 0.25f,
            contrast = 1.2f,
            filterPreset = FilterPreset.VIVID,
            imageDescription = "Aegean sunset loupe test",
            loupes = listOf(
                LoupeAnnotation(
                    id = "l1",
                    centerX = 0.42f,
                    centerY = 0.61f,
                    radiusFraction = 0.27f,
                    zoomLevel = 2.8f,
                    strokeColorArgb = 0xFF007AFFL,
                    strokeWidthPt = 5
                )
            )
        )
        val encoded = EditRecipeCodec.encode(original)
        val decoded = EditRecipeCodec.decode(encoded)
        assertEquals(original.filterPreset, decoded.filterPreset)
        assertEquals(original.imageDescription, decoded.imageDescription)
        assertEquals(1, decoded.loupes.size)
        assertEquals(2.8f, decoded.loupes.first().zoomLevel, 0.01f)
        assertEquals(0.27f, decoded.loupes.first().radiusFraction, 0.01f)
        assertEquals(5, decoded.loupes.first().strokeWidthPt)
    }

    @Test
    fun `natural language search filters videos and favorites cleanly`() = runTest {
        val media = repo.loadLibraryMedia(MediaPermissionState.DENIED)
        assertTrue(media.isNotEmpty())

        val videos = repo.searchMediaNaturalLanguage(media, "videos")
        assertTrue(videos.isNotEmpty())
        assertTrue(videos.all { it.mediaType == MediaType.VIDEO })

        val favs = repo.searchMediaNaturalLanguage(media, "favorites")
        assertTrue(favs.isNotEmpty())
        assertTrue(favs.all { it.isFavorite })

        val none = repo.searchMediaNaturalLanguage(media, "nonexistentkeywordxyz123")
        assertTrue(none.isEmpty())
    }

    @Test
    fun `glass config degrades gracefully when reduceGlass or powerSave is enabled`() {
        val full = GlassConfig(style = GlassStyle.CLEAR, intensity = 0.9f, reduceGlass = false, isPowerSaveFallback = false)
        assertTrue(full.effectiveRefraction > 0f)
        assertTrue(full.effectiveBlurDp > 0f)

        val reduced = full.copy(reduceGlass = true)
        assertEquals(0f, reduced.effectiveRefraction, 0.01f)
        assertEquals(0f, reduced.effectiveBlurDp, 0.01f)

        val powerSave = full.copy(isPowerSaveFallback = true)
        assertEquals(0f, powerSave.effectiveRefraction, 0.01f)
        assertEquals(0f, powerSave.effectiveBlurDp, 0.01f)
    }

    @Test
    fun `settings screen shows live preview and theme controls`() {
        var currentSettings = SettingsState()
        composeRule.setContent {
            GlasslyTheme(darkTheme = false) {
                androidx.compose.runtime.CompositionLocalProvider(
                    LocalAppStrings provides EnglishStrings,
                    LocalGlassConfig provides GlassConfig()
                ) {
                    SettingsScreen(
                        settings = currentSettings,
                        permissionState = MediaPermissionState.DENIED,
                        cacheSizeLabel = "12 MB",
                        onUpdateSettings = { fn -> currentSettings = fn(currentSettings) },
                        onResetAllSettings = {},
                        onClearCache = {},
                        onRequestPermissions = {},
                        onOpenDuplicates = {},
                        onOpenLargeFiles = {},
                        onOpenGlassGalleryDebug = {},
                        onShowToast = {},
                        onBack = {}
                    )
                }
            }
        }

        composeRule.waitForIdle()
        composeRule.onNodeWithTag("settings_live_preview_card").assertIsDisplayed()
        composeRule.onNodeWithTag("settings_theme_segment").assertIsDisplayed()
        composeRule.onNodeWithTag("settings_glass_style_segment").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Settings").assertIsDisplayed()
    }

    @Test
    fun `glass gallery debug screen and bangla localization render cleanly`() {
        assertEquals("গ্লাসলি", BanglaStrings.appName)
        composeRule.setContent {
            GlasslyTheme(darkTheme = true) {
                androidx.compose.runtime.CompositionLocalProvider(
                    LocalAppStrings provides BanglaStrings,
                    LocalGlassConfig provides GlassConfig()
                ) {
                    GlassGalleryDebugScreen(onBack = {})
                }
            }
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Glass Gallery").assertIsDisplayed()
        composeRule.onAllNodesWithText("Regular").onFirst().performClick()
    }

    @Test
    fun `library screen opens filter popover and displays showcase media`() = runTest {
        val sample = repo.loadLibraryMedia(MediaPermissionState.DENIED)
        val group = LibraryGroup("all_photos", "All Photos", "Santorini, Greece", sample)
        var selectionMode = false

        composeRule.setContent {
            GlasslyTheme(darkTheme = false) {
                androidx.compose.runtime.CompositionLocalProvider(
                    LocalAppStrings provides EnglishStrings,
                    LocalGlassConfig provides GlassConfig()
                ) {
                    LibraryScreen(
                        uiState = UiState.Success(listOf(group)),
                        settings = SettingsState(),
                        permissionState = MediaPermissionState.FULL,
                        gridColumns = 3,
                        activeTimeSegment = TimeSegment.ALL,
                        activeFilters = emptySet(),
                        isSelectionMode = selectionMode,
                        selectedIds = emptySet(),
                        isRefreshing = false,
                        onScrollDirectionChanged = {},
                        onRefresh = {},
                        onRetry = {},
                        onRequestPermissions = {},
                        onOpenMedia = {},
                        onSetGridColumns = {},
                        onSetSortOrder = {},
                        onToggleFilter = {},
                        onClearFilters = {},
                        onToggleSquareGrid = {},
                        onToggleIncludeScreenshots = {},
                        onToggleIncludeShared = {},
                        onToggleSelectionMode = { selectionMode = it },
                        onToggleItemSelected = {},
                        onSelectAll = {},
                        onBatchCopy = {},
                        onBatchFavorite = {},
                        onBatchDuplicate = {},
                        onBatchHide = {},
                        onBatchSlideshow = {},
                        onBatchAddToAlbum = {},
                        onBatchAdjustDate = {},
                        onBatchAdjustLocation = {},
                        onBatchDeleteConfirm = {},
                        onToggleTimeSegmentsBar = {}
                    )
                }
            }
        }

        composeRule.waitForIdle()
        composeRule.onNodeWithTag("library_time_toggle_button").assertDoesNotExist()
        composeRule.onNodeWithTag("library_filter_button").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("menu_open_filter_submenu").assertIsDisplayed()
        // Tapping outside the popover first dismisses the modal scrim, then Select activates selection mode
        composeRule.onNodeWithTag("library_select_button").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("library_select_button").performClick()
        composeRule.waitForIdle()
        assertTrue(selectionMode)
    }

    @Test
    fun `collections screen opens settings badge and more popover`() = runTest {
        val sample = repo.loadLibraryMedia(MediaPermissionState.DENIED)
        var settingsOpened = false
        var showAllTriggered = false

        composeRule.setContent {
            GlasslyTheme(darkTheme = false) {
                androidx.compose.runtime.CompositionLocalProvider(
                    LocalAppStrings provides EnglishStrings,
                    LocalGlassConfig provides GlassConfig()
                ) {
                    CollectionsScreen(
                        settings = SettingsState(collectionTileLayout = CollectionTileLayout.TOP_ROW_EMPHASIS),
                        visibleMedia = sample,
                        albums = emptyList(),
                        smartCollections = listOf(SmartCollectionSummary("videos", "Videos", 2, null, null, "video")),
                        pinnedEntries = emptyList(),
                        trashedCount = 0,
                        hiddenCount = 0,
                        duplicatesCount = 0,
                        largeFilesCount = 1,
                        onSetLayout = {},
                        onToggleSectionCollapsed = {},
                        onShowAllSections = { showAllTriggered = true },
                        onCollapseAllSections = {},
                        onMoveSection = { _, _ -> },
                        onTogglePin = {},
                        onMovePinned = { _, _ -> },
                        onCreateAlbum = {},
                        onRenameAlbum = { _, _ -> },
                        onDeleteAlbum = {},
                        onOpenAlbum = {},
                        onOpenSmartGroup = {},
                        onOpenTrash = {},
                        onOpenHidden = {},
                        onOpenDuplicates = {},
                        onOpenLargeFiles = {},
                        onOpenSettings = { settingsOpened = true },
                        onOpenMedia = {}
                    )
                }
            }
        }

        composeRule.waitForIdle()
        composeRule.onNodeWithTag("collections_more_button").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Show All").assertIsDisplayed()
        composeRule.onNodeWithText("Show All").performClick()
        composeRule.waitForIdle()
        assertTrue(showAllTriggered)
        composeRule.onNodeWithTag("collections_profile_settings_button").performClick()
        composeRule.waitForIdle()
        assertTrue(settingsOpened)
    }

    @Test
    fun `splash screen and all bundled drawable assets load without resource resolution errors`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val drawables = listOf(
            R.drawable.img_backdrop_aurora,
            R.drawable.glassly_icon_fg,
            R.drawable.img_sample_aegean,
            R.drawable.img_sample_alpine,
            R.drawable.img_sample_poppy
        )
        drawables.forEach { resId ->
            val bmp = android.graphics.BitmapFactory.decodeResource(context.resources, resId)
            assertTrue("Drawable $resId failed to decode", bmp != null && bmp.width > 0 && bmp.height > 0)
        }

        var splashFinished = false
        composeRule.setContent {
            GlasslyTheme(darkTheme = false) {
                androidx.compose.runtime.CompositionLocalProvider(
                    LocalAppStrings provides EnglishStrings,
                    LocalGlassConfig provides GlassConfig(reduceMotion = true)
                ) {
                    com.example.ui.screens.GlasslySplashScreen(onFinished = { splashFinished = true })
                }
            }
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Glassly").assertIsDisplayed()
        composeRule.onNodeWithText("Version 1.0.7").assertIsDisplayed()
    }

    @Test
    fun `collections screen with live glassBackdropSource and reorder mode renders without cycle`() = runTest {
        val sample = repo.loadLibraryMedia(MediaPermissionState.DENIED)

        composeRule.setContent {
            val backdrop = com.example.glass.rememberGlassBackdropState()
            GlasslyTheme(darkTheme = false) {
                androidx.compose.runtime.CompositionLocalProvider(
                    LocalAppStrings provides EnglishStrings,
                    LocalGlassConfig provides GlassConfig(),
                    com.example.glass.LocalGlassBackdrop provides backdrop
                ) {
                    CollectionsScreen(
                        settings = SettingsState(collectionTileLayout = CollectionTileLayout.TOP_ROW_EMPHASIS),
                        visibleMedia = sample,
                        albums = emptyList(),
                        smartCollections = listOf(SmartCollectionSummary("videos", "Videos", 2, null, null, "video")),
                        pinnedEntries = emptyList(),
                        trashedCount = 0,
                        hiddenCount = 0,
                        duplicatesCount = 0,
                        largeFilesCount = 1,
                        onSetLayout = {},
                        onToggleSectionCollapsed = {},
                        onShowAllSections = {},
                        onCollapseAllSections = {},
                        onMoveSection = { _, _ -> },
                        onTogglePin = {},
                        onMovePinned = { _, _ -> },
                        onCreateAlbum = {},
                        onRenameAlbum = { _, _ -> },
                        onDeleteAlbum = {},
                        onOpenAlbum = {},
                        onOpenSmartGroup = {},
                        onOpenTrash = {},
                        onOpenHidden = {},
                        onOpenDuplicates = {},
                        onOpenLargeFiles = {},
                        onOpenSettings = {},
                        onOpenMedia = {},
                        backdropModifier = androidx.compose.ui.Modifier.glassBackdropSource(backdrop)
                    )
                }
            }
        }

        composeRule.waitForIdle()
        composeRule.onNodeWithText("Collections").assertIsDisplayed()
        composeRule.onNodeWithTag("collections_more_button").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Reorder").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("collections_reorder_done_button").assertIsDisplayed()
    }
}
