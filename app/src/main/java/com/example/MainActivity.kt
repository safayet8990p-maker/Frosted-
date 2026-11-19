package com.example

import android.Manifest
import android.app.PictureInPictureParams
import android.content.pm.ActivityInfo
import android.os.Build
import android.os.Bundle
import android.util.Rational
import android.view.WindowManager
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Collections
import androidx.compose.material.icons.rounded.PhotoLibrary
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Text
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.AlbumModel
import com.example.data.model.AppLanguage
import com.example.data.model.AppThemeMode
import com.example.data.model.MediaItem
import com.example.data.model.MediaType
import com.example.data.model.UiState
import com.example.data.repository.MediaRepository
import com.example.glass.AuroraMeshBackdrop
import com.example.glass.GlassAlertDialog
import com.example.glass.GlassSplitBottomBar
import com.example.glass.GlassToastBanner
import com.example.glass.LocalGlassBackdrop
import com.example.glass.LocalGlassConfig
import com.example.glass.LocalTiltLightState
import com.example.glass.MainNavTab
import com.example.glass.glassBackdropSource
import com.example.glass.rememberGlassBackdropState
import com.example.glass.rememberTiltLightState
import com.example.ui.i18n.BanglaStrings
import com.example.ui.i18n.EnglishStrings
import com.example.ui.i18n.LocalAppStrings
import com.example.ui.screens.AddToAlbumSheet
import com.example.ui.screens.AdjustDateLocationSheet
import com.example.ui.screens.AlbumDetailScreen
import com.example.ui.screens.CollectionsScreen
import com.example.ui.screens.DuplicatesScreen
import com.example.ui.screens.GlassGalleryDebugScreen
import com.example.ui.screens.GlasslySplashScreen
import com.example.ui.screens.HiddenVaultScreen
import com.example.ui.screens.LargeFilesScreen
import com.example.ui.screens.LibraryScreen
import com.example.ui.screens.PhotoEditorScreen
import com.example.ui.screens.PhotoViewerScreen
import com.example.ui.screens.SearchOverlayScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.SlideshowScreen
import com.example.ui.screens.TrashScreen
import com.example.ui.screens.WelcomeTourAndPermissionScreen
import com.example.ui.screens.shareMediaItems
import com.example.ui.theme.GlasslyTheme
import com.example.ui.theme.LocalGlassColors
import com.example.viewmodel.GlasslyViewModel
import kotlinx.coroutines.launch

sealed interface AppOverlay {
    data object None : AppOverlay
    data object Search : AppOverlay
    data object Settings : AppOverlay
    data object GlassGalleryDebug : AppOverlay
    data object Trash : AppOverlay
    data object HiddenVault : AppOverlay
    data object Duplicates : AppOverlay
    data object LargeFiles : AppOverlay
    data class AlbumDetail(val album: AlbumModel, val overrideItems: List<MediaItem>? = null) : AppOverlay
    data class PhotoViewer(val items: List<MediaItem>, val initialMediaId: String) : AppOverlay
    data class PhotoEditor(val item: MediaItem) : AppOverlay
    data class Slideshow(val items: List<MediaItem>) : AppOverlay
}

class MainActivity : FragmentActivity() {

    private val viewModel: GlasslyViewModel by viewModels()

    @OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val windowSizeClass = calculateWindowSizeClass(this)
            GlasslyRootApp(
                viewModel = viewModel,
                isWideLayout = windowSizeClass.widthSizeClass != WindowWidthSizeClass.Compact,
                onToggleSecureFlag = { secure ->
                    if (secure) {
                        window.setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE)
                    } else {
                        window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
                    }
                },
                onTriggerBiometric = { onUnlocked ->
                    launchBiometricUnlock(
                        onSuccess = onUnlocked,
                        onError = { msg -> viewModel.showToast(msg) }
                    )
                },
                onRequestPip = {
                    val params = PictureInPictureParams.Builder()
                        .setAspectRatio(Rational(16, 9))
                        .build()
                    runCatching { enterPictureInPictureMode(params) }
                },
                onToggleOrientation = {
                    requestedOrientation = if (requestedOrientation == ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE) {
                        ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
                    } else {
                        ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
                    }
                }
            )
        }
    }

    private fun launchBiometricUnlock(
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        val manager = BiometricManager.from(this)
        val canAuth = manager.canAuthenticate(
            BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.DEVICE_CREDENTIAL
        )
        if (canAuth != BiometricManager.BIOMETRIC_SUCCESS) {
            onError("Biometric unavailable — enter Vault PIN")
            return
        }
        val executor = ContextCompat.getMainExecutor(this)
        val prompt = BiometricPrompt(
            this,
            executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    onSuccess()
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    onError(errString.toString())
                }
            }
        )
        val info = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Unlock Hidden Album")
            .setSubtitle("Authenticate to view your hidden photos and videos")
            .setAllowedAuthenticators(
                BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.DEVICE_CREDENTIAL
            )
            .build()
        prompt.authenticate(info)
    }
}

@Composable
fun GlasslyRootApp(
    viewModel: GlasslyViewModel,
    isWideLayout: Boolean = false,
    onToggleSecureFlag: (Boolean) -> Unit = {},
    onTriggerBiometric: (onUnlocked: () -> Unit) -> Unit = {},
    onRequestPip: () -> Unit = {},
    onToggleOrientation: () -> Unit = {}
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val glassConfig by viewModel.glassConfig.collectAsStateWithLifecycle()
    val permissionState by viewModel.permissionState.collectAsStateWithLifecycle()
    val libraryUiState by viewModel.libraryUiState.collectAsStateWithLifecycle()
    val libraryMedia by viewModel.libraryMedia.collectAsStateWithLifecycle()
    val allVisibleMedia by viewModel.allVisibleMedia.collectAsStateWithLifecycle()
    val gridColumns by viewModel.gridColumns.collectAsStateWithLifecycle()
    val activeTimeSegment by viewModel.activeTimeSegment.collectAsStateWithLifecycle()
    val activeFilters by viewModel.activeFilters.collectAsStateWithLifecycle()
    val isSelectionMode by viewModel.isSelectionMode.collectAsStateWithLifecycle()
    val selectedIds by viewModel.selectedMediaIds.collectAsStateWithLifecycle()
    val isRefreshing by viewModel.isRefreshing.collectAsStateWithLifecycle()
    val albums by viewModel.albumsList.collectAsStateWithLifecycle()
    val trashedMedia by viewModel.trashedMedia.collectAsStateWithLifecycle()
    val hiddenMedia by viewModel.hiddenMedia.collectAsStateWithLifecycle()
    val isVaultUnlocked by viewModel.isHiddenVaultUnlocked.collectAsStateWithLifecycle()
    val duplicateGroups by viewModel.duplicateGroups.collectAsStateWithLifecycle()
    val largeFiles by viewModel.largeFilesMedia.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val searchResults by viewModel.searchResults.collectAsStateWithLifecycle()
    val recentViewed by viewModel.recentViewedMedia.collectAsStateWithLifecycle()
    val recentEdited by viewModel.recentEditedMedia.collectAsStateWithLifecycle()
    val recentShared by viewModel.recentSharedMedia.collectAsStateWithLifecycle()
    val searchHistory by viewModel.searchHistory.collectAsStateWithLifecycle()
    val copiedEditState by viewModel.copiedEditState.collectAsStateWithLifecycle()
    val toastMessage by viewModel.toastMessage.collectAsStateWithLifecycle()

    LaunchedEffect(settings.secureScreen, settings.hidePreviewsInRecents) {
        onToggleSecureFlag(settings.secureScreen || settings.hidePreviewsInRecents)
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) {
        viewModel.refreshPermissionState()
    }

    fun requestMediaPermissions() {
        val perms = when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE -> arrayOf(
                Manifest.permission.READ_MEDIA_IMAGES,
                Manifest.permission.READ_MEDIA_VIDEO,
                Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED
            )
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU -> arrayOf(
                Manifest.permission.READ_MEDIA_IMAGES,
                Manifest.permission.READ_MEDIA_VIDEO
            )
            else -> arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
        }
        permissionLauncher.launch(perms)
    }

    val appStrings = if (settings.language == AppLanguage.BANGLA) BanglaStrings else EnglishStrings
    val tiltLight = rememberTiltLightState(enabled = !glassConfig.reduceMotion)

    var showSplash by remember { mutableStateOf(true) }
    var activeTab by remember { mutableStateOf(MainNavTab.LIBRARY) }
    var activeOverlay by remember { mutableStateOf<AppOverlay>(AppOverlay.None) }
    var overlayHistory by remember { mutableStateOf<List<AppOverlay>>(emptyList()) }
    var isCompactBarOnScroll by remember { mutableStateOf(false) }
    var showTimeSegmentsInBar by remember { mutableStateOf(false) }

    // Shared dialogs/sheets driven by Library multi-select or Viewer
    var showBatchDeleteConfirm by remember { mutableStateOf(false) }
    var addToAlbumTargetIds by remember { mutableStateOf<List<String>?>(null) }
    var adjustDateLocTargetIds by remember { mutableStateOf<List<String>?>(null) }

    fun openOverlay(next: AppOverlay) {
        if (activeOverlay != AppOverlay.None) {
            overlayHistory = overlayHistory + activeOverlay
        }
        activeOverlay = next
    }

    fun popOverlay() {
        if (overlayHistory.isNotEmpty()) {
            activeOverlay = overlayHistory.last()
            overlayHistory = overlayHistory.dropLast(1)
        } else {
            activeOverlay = AppOverlay.None
        }
    }

    val libraryGridState = rememberLazyGridState()

    val visibleOnly = remember(allVisibleMedia) {
        allVisibleMedia.filter { !it.isTrashed && !it.isHidden }
    }
    val smartCollections = remember(visibleOnly) {
        viewModel.buildSmartCollections(visibleOnly)
    }
    val pinnedEntries = remember(settings.pinnedIds, visibleOnly, albums) {
        viewModel.resolvePinnedEntries(settings.pinnedIds, visibleOnly, albums)
    }

    fun openSmartGroup(groupId: String) {
        val matched = when (groupId) {
            "favorites" -> visibleOnly.filter { it.isFavorite }
            "videos" -> visibleOnly.filter { it.mediaType == MediaType.VIDEO }
            "screenshots" -> visibleOnly.filter { it.isScreenshot || it.mediaType == MediaType.SCREENSHOT }
            "selfies" -> visibleOnly.filter { it.isSelfie }
            "raw" -> visibleOnly.filter { it.mediaType == MediaType.RAW }
            "gifs" -> visibleOnly.filter { it.mediaType == MediaType.GIF }
            "motion" -> visibleOnly.filter { it.mediaType == MediaType.MOTION_PHOTO }
            else -> visibleOnly
        }
        val title = smartCollections.find { it.id == groupId }?.title ?: groupId.replaceFirstChar { it.uppercase() }
        openOverlay(
            AppOverlay.AlbumDetail(
                album = AlbumModel(
                    id = "smart_$groupId",
                    title = title,
                    coverUri = matched.lastOrNull()?.uriString,
                    coverSampleRes = matched.lastOrNull()?.sampleDrawableRes,
                    itemCount = matched.size
                ),
                overrideItems = matched
            )
        )
    }

    val isDark = when (settings.themeMode) {
        AppThemeMode.DARK -> true
        AppThemeMode.LIGHT -> false
        AppThemeMode.SYSTEM -> isSystemInDarkTheme()
    }
    GlasslyTheme(
        darkTheme = isDark,
        accentColor = settings.accentPreset.resolve(isDark)
    ) {
        val glassColors = LocalGlassColors.current
        CompositionLocalProvider(
            LocalAppStrings provides appStrings,
            LocalGlassConfig provides glassConfig,
            LocalTiltLightState provides tiltLight,
            LocalLayoutDirection provides LayoutDirection.Ltr
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                when {
                    showSplash -> {
                        GlasslySplashScreen(onFinished = { showSplash = false })
                    }

                    !settings.onboardingCompleted -> {
                        WelcomeTourAndPermissionScreen(
                            permissionState = permissionState,
                            onRequestMediaPermissions = { requestMediaPermissions() },
                            onContinueToLibrary = { viewModel.completeOnboarding() }
                        )
                    }

                    else -> {
                        val libraryBackdropState = rememberGlassBackdropState()
                        val collectionsBackdropState = rememberGlassBackdropState()
                        val activeTabBackdrop = if (activeOverlay == AppOverlay.None) {
                            if (activeTab == MainNavTab.LIBRARY) libraryBackdropState else collectionsBackdropState
                        } else {
                            null
                        }
                        CompositionLocalProvider(LocalGlassBackdrop provides activeTabBackdrop) {
                            Row(modifier = Modifier.fillMaxSize()) {
                                if (isWideLayout) {
                                    NavigationRail(
                                        containerColor = glassColors.secondaryBackground,
                                        modifier = Modifier.fillMaxHeight()
                                    ) {
                                        NavigationRailItem(
                                            selected = activeTab == MainNavTab.LIBRARY,
                                            onClick = { activeTab = MainNavTab.LIBRARY },
                                            icon = { Icon(Icons.Rounded.PhotoLibrary, contentDescription = appStrings.tabLibrary) },
                                            label = { Text(appStrings.tabLibrary) }
                                        )
                                        NavigationRailItem(
                                            selected = activeTab == MainNavTab.COLLECTIONS,
                                            onClick = { activeTab = MainNavTab.COLLECTIONS },
                                            icon = { Icon(Icons.Rounded.Collections, contentDescription = appStrings.tabCollections) },
                                            label = { Text(appStrings.tabCollections) }
                                        )
                                        NavigationRailItem(
                                            selected = activeOverlay is AppOverlay.Search,
                                            onClick = { openOverlay(AppOverlay.Search) },
                                            icon = { Icon(Icons.Rounded.Search, contentDescription = appStrings.tabSearch) },
                                            label = { Text(appStrings.tabSearch) }
                                        )
                                        NavigationRailItem(
                                            selected = activeOverlay is AppOverlay.Settings,
                                            onClick = { openOverlay(AppOverlay.Settings) },
                                            icon = { Icon(Icons.Rounded.Settings, contentDescription = appStrings.tabSettings) },
                                            label = { Text(appStrings.tabSettings) }
                                        )
                                    }
                                }

                                Box(modifier = Modifier.weight(1f).fillMaxSize()) {
                                    Box(modifier = Modifier.fillMaxSize()) {
                                        AuroraMeshBackdrop()
                                        AnimatedContent(
                                            targetState = activeTab,
                                            transitionSpec = { fadeIn() togetherWith fadeOut() },
                                            label = "mainTabSwitch"
                                        ) { tab ->
                                            when (tab) {
                                                MainNavTab.LIBRARY -> {
                                                    LibraryScreen(
                                                        uiState = libraryUiState,
                                                        settings = settings,
                                                        permissionState = permissionState,
                                                        gridColumns = gridColumns,
                                                        activeTimeSegment = activeTimeSegment,
                                                        activeFilters = activeFilters,
                                                        isSelectionMode = isSelectionMode,
                                                        selectedIds = selectedIds,
                                                        isRefreshing = isRefreshing,
                                                        gridState = libraryGridState,
                                                        backdropModifier = Modifier.glassBackdropSource(libraryBackdropState),
                                                        onScrollDirectionChanged = { down -> isCompactBarOnScroll = down },
                                                        onRefresh = { viewModel.pullToRefresh() },
                                                        onRetry = { viewModel.refreshPermissionState() },
                                                        onRequestPermissions = { requestMediaPermissions() },
                                                        onOpenMedia = { media ->
                                                            viewModel.markViewed(media.id)
                                                            openOverlay(AppOverlay.PhotoViewer(libraryMedia, media.id))
                                                        },
                                                        onSetGridColumns = { cols -> viewModel.setGridColumns(cols) },
                                                        onSetSortOrder = { order -> viewModel.setSortOrder(order) },
                                                        onToggleFilter = { filter -> viewModel.toggleFilter(filter) },
                                                        onClearFilters = { viewModel.clearAllFilters() },
                                                        onToggleSquareGrid = { sq -> viewModel.toggleSquareGrid(sq) },
                                                        onToggleIncludeScreenshots = { viewModel.toggleIncludeScreenshots() },
                                                        onToggleIncludeShared = { viewModel.toggleIncludeSharedWithYou() },
                                                        onToggleSelectionMode = { enabled -> viewModel.setSelectionMode(enabled) },
                                                        onToggleItemSelected = { id -> viewModel.toggleMediaSelection(id) },
                                                        onSelectAll = { list -> viewModel.selectAllVisible(list) },
                                                        onBatchCopy = { viewModel.copyMediaToClipboard(selectedIds.toList()) },
                                                        onBatchFavorite = { viewModel.batchFavoriteSelected() },
                                                        onBatchDuplicate = { viewModel.duplicateMedia(selectedIds.toList()) },
                                                        onBatchHide = { viewModel.batchHideSelected() },
                                                        onBatchSlideshow = {
                                                            val chosen = libraryMedia.filter { it.id in selectedIds }.ifEmpty { libraryMedia }
                                                            openOverlay(AppOverlay.Slideshow(chosen))
                                                        },
                                                        onBatchAddToAlbum = { addToAlbumTargetIds = selectedIds.toList() },
                                                        onBatchAdjustDate = { adjustDateLocTargetIds = selectedIds.toList() },
                                                        onBatchAdjustLocation = { adjustDateLocTargetIds = selectedIds.toList() },
                                                        onBatchDeleteConfirm = { viewModel.batchTrashSelected() },
                                                        onToggleTimeSegmentsBar = { showTimeSegmentsInBar = !showTimeSegmentsInBar }
                                                    )
                                                }

                                                MainNavTab.COLLECTIONS -> {
                                                    CollectionsScreen(
                                                        settings = settings,
                                                        visibleMedia = visibleOnly,
                                                        albums = albums,
                                                        smartCollections = smartCollections,
                                                        pinnedEntries = pinnedEntries,
                                                        trashedCount = trashedMedia.size,
                                                        hiddenCount = hiddenMedia.size,
                                                        duplicatesCount = duplicateGroups.size,
                                                        largeFilesCount = largeFiles.size,
                                                        onSetLayout = { layout -> viewModel.setCollectionLayout(layout) },
                                                        onToggleSectionCollapsed = { sec -> viewModel.toggleSectionCollapsed(sec) },
                                                        onShowAllSections = { viewModel.showAllSections() },
                                                        onCollapseAllSections = { viewModel.collapseAllSections() },
                                                        onMoveSection = { f, t -> viewModel.moveCollectionSection(f, t) },
                                                        onTogglePin = { id -> viewModel.togglePinItem(id) },
                                                        onMovePinned = { f, t -> viewModel.movePinnedItem(f, t) },
                                                        onCreateAlbum = { name -> viewModel.createAlbum(name) },
                                                        onRenameAlbum = { id, name -> viewModel.renameAlbum(id, name) },
                                                        onDeleteAlbum = { id -> viewModel.deleteAlbum(id) },
                                                        onOpenAlbum = { alb ->
                                                            val albumItems = visibleOnly.filter { it.id in alb.mediaIds }
                                                            openOverlay(AppOverlay.AlbumDetail(alb, albumItems))
                                                        },
                                                        onOpenSmartGroup = { id -> openSmartGroup(id) },
                                                        onOpenTrash = { openOverlay(AppOverlay.Trash) },
                                                        onOpenHidden = { openOverlay(AppOverlay.HiddenVault) },
                                                        onOpenDuplicates = { openOverlay(AppOverlay.Duplicates) },
                                                        onOpenLargeFiles = { openOverlay(AppOverlay.LargeFiles) },
                                                        onOpenSettings = { openOverlay(AppOverlay.Settings) },
                                                        onOpenMedia = { media ->
                                                            viewModel.markViewed(media.id)
                                                            openOverlay(AppOverlay.PhotoViewer(visibleOnly, media.id))
                                                        },
                                                        backdropModifier = Modifier.glassBackdropSource(collectionsBackdropState)
                                                    )
                                                }
                                            }
                                        }
                                    }

                                    // Floating Split Liquid Glass Bottom Bar (hidden on wide rail or when full-screen overlay active)
                                    if (!isWideLayout && activeOverlay == AppOverlay.None) {
                                        GlassSplitBottomBar(
                                            activeTab = activeTab,
                                            isCompactOnScroll = isCompactBarOnScroll,
                                            showTimeSegmentsInBar = showTimeSegmentsInBar,
                                            activeTimeSegment = activeTimeSegment,
                                            isSelectionMode = isSelectionMode,
                                            selectedCount = selectedIds.size,
                                            onTabSelected = { targetTab ->
                                                if (targetTab == MainNavTab.LIBRARY && activeTab == MainNavTab.LIBRARY) {
                                                    // Tapping Library while in Library jumps smoothly back to latest photos at bottom (video 2:24)
                                                    val groups = (libraryUiState as? UiState.Success)?.data.orEmpty()
                                                    val total = groups.sumOf { 1 + it.items.size }
                                                    if (total > 0) {
                                                        scope.launch { libraryGridState.animateScrollToItem(total - 1) }
                                                    }
                                                } else {
                                                    activeTab = targetTab
                                                    isCompactBarOnScroll = false
                                                }
                                            },
                                            onExpandBarTap = { isCompactBarOnScroll = false },
                                            onToggleTimeSegmentsBar = { showTimeSegmentsInBar = !showTimeSegmentsInBar },
                                            onTimeSegmentSelected = { seg -> viewModel.setTimeSegment(seg) },
                                            onSearchClick = { openOverlay(AppOverlay.Search) },
                                            onShareSelected = {
                                                val chosen = libraryMedia.filter { it.id in selectedIds }
                                                viewModel.markShared(selectedIds.toList())
                                                shareMediaItems(context, chosen) { msg -> viewModel.showToast(msg) }
                                            },
                                            onDeleteSelected = { showBatchDeleteConfirm = true },
                                            modifier = Modifier.align(Alignment.BottomCenter)
                                        )
                                    }

                                    // Full-screen Overlay Routing with BackHandler support
                                    val overlay = activeOverlay
                                    if (overlay != AppOverlay.None) {
                                        BackHandler { popOverlay() }
                                    } else if (isSelectionMode) {
                                        BackHandler { viewModel.setSelectionMode(false) }
                                    }

                                    when (overlay) {
                                        AppOverlay.None -> Unit
                                        AppOverlay.Search -> {
                                            SearchOverlayScreen(
                                                query = searchQuery,
                                                searchResults = searchResults,
                                                albums = albums,
                                                recentViewed = recentViewed,
                                                recentEdited = recentEdited,
                                                recentShared = recentShared,
                                                history = searchHistory,
                                                onQueryChange = { q -> viewModel.setSearchQuery(q) },
                                                onSubmitQuery = { q -> viewModel.submitSearch(q) },
                                                onClearHistory = { viewModel.clearSearchHistory() },
                                                onOpenMedia = { media ->
                                                    viewModel.markViewed(media.id)
                                                    openOverlay(AppOverlay.PhotoViewer(searchResults.ifEmpty { visibleOnly }, media.id))
                                                },
                                                onOpenAlbum = { alb ->
                                                    val albumItems = visibleOnly.filter { it.id in alb.mediaIds }
                                                    openOverlay(AppOverlay.AlbumDetail(alb, albumItems))
                                                },
                                                onDismissSearch = { popOverlay() }
                                            )
                                        }
                                        AppOverlay.Settings -> {
                                            SettingsScreen(
                                                settings = settings,
                                                permissionState = permissionState,
                                                cacheSizeLabel = "14.2 MB cached thumbnails",
                                                onUpdateSettings = { transform -> viewModel.updateSettings(transform) },
                                                onResetAllSettings = { viewModel.resetAllSettings() },
                                                onClearCache = {
                                                    runCatching { context.cacheDir.deleteRecursively() }
                                                    viewModel.showToast("Thumbnail cache cleared")
                                                },
                                                onRequestPermissions = { requestMediaPermissions() },
                                                onOpenDuplicates = { openOverlay(AppOverlay.Duplicates) },
                                                onOpenLargeFiles = { openOverlay(AppOverlay.LargeFiles) },
                                                onOpenGlassGalleryDebug = { openOverlay(AppOverlay.GlassGalleryDebug) },
                                                onShowToast = { msg -> viewModel.showToast(msg) },
                                                onBack = { popOverlay() }
                                            )
                                        }
                                        AppOverlay.GlassGalleryDebug -> {
                                            GlassGalleryDebugScreen(onBack = { popOverlay() })
                                        }
                                        AppOverlay.Trash -> {
                                            TrashScreen(
                                                trashedItems = trashedMedia,
                                                retentionDays = settings.trashRetentionDays,
                                                onRestore = { id -> viewModel.restoreFromTrash(id) },
                                                onDeleteForever = { id -> viewModel.deleteForever(id) },
                                                onEmptyTrash = { viewModel.emptyTrash() },
                                                onBack = { popOverlay() }
                                            )
                                        }
                                        AppOverlay.HiddenVault -> {
                                            HiddenVaultScreen(
                                                settings = settings,
                                                isUnlocked = isVaultUnlocked,
                                                hiddenItems = hiddenMedia,
                                                onUnlockSuccess = { viewModel.unlockVault() },
                                                onLockVault = { viewModel.lockVault() },
                                                onTriggerBiometric = { onTriggerBiometric { viewModel.unlockVault() } },
                                                onUnhide = { id -> viewModel.unhideSingle(id) },
                                                onOpenMedia = { media -> openOverlay(AppOverlay.PhotoViewer(hiddenMedia, media.id)) },
                                                onBack = { popOverlay() }
                                            )
                                        }
                                        AppOverlay.Duplicates -> {
                                            DuplicatesScreen(
                                                duplicateGroups = duplicateGroups,
                                                onTrashDuplicate = { id -> viewModel.trashSingle(id) },
                                                onOpenMedia = { media -> openOverlay(AppOverlay.PhotoViewer(visibleOnly, media.id)) },
                                                onBack = { popOverlay() }
                                            )
                                        }
                                        AppOverlay.LargeFiles -> {
                                            LargeFilesScreen(
                                                largeFiles = largeFiles,
                                                onTrashItem = { id -> viewModel.trashSingle(id) },
                                                onOpenMedia = { media -> openOverlay(AppOverlay.PhotoViewer(largeFiles, media.id)) },
                                                onBack = { popOverlay() }
                                            )
                                        }
                                        is AppOverlay.AlbumDetail -> {
                                            val items = overlay.overrideItems ?: visibleOnly.filter { it.id in overlay.album.mediaIds }
                                            AlbumDetailScreen(
                                                album = overlay.album,
                                                items = items,
                                                onBack = { popOverlay() },
                                                onOpenMedia = { media ->
                                                    viewModel.markViewed(media.id)
                                                    openOverlay(AppOverlay.PhotoViewer(items, media.id))
                                                }
                                            )
                                        }
                                        is AppOverlay.PhotoViewer -> {
                                            val refreshedItems = remember(overlay.items, allVisibleMedia) {
                                                val map = allVisibleMedia.associateBy { it.id }
                                                overlay.items.mapNotNull { old -> map[old.id] }.filter { !it.isTrashed }
                                            }
                                            PhotoViewerScreen(
                                                items = refreshedItems.ifEmpty { overlay.items },
                                                initialMediaId = overlay.initialMediaId,
                                                settings = settings,
                                                repository = viewModel.repository,
                                                onBack = { popOverlay() },
                                                onShare = { media ->
                                                    viewModel.markShared(listOf(media.id))
                                                    shareMediaItems(context, listOf(media)) { msg -> viewModel.showToast(msg) }
                                                },
                                                onToggleFavorite = { id -> viewModel.toggleFavorite(id) },
                                                onOpenEditor = { media -> openOverlay(AppOverlay.PhotoEditor(media)) },
                                                onDeleteConfirm = { id -> viewModel.trashSingle(id) },
                                                onHide = { id -> viewModel.hideSingle(id) },
                                                onStartSlideshow = { list -> openOverlay(AppOverlay.Slideshow(list)) },
                                                onAdjustDateLocation = { media -> adjustDateLocTargetIds = listOf(media.id) },
                                                onRequestPip = onRequestPip,
                                                onToggleFullscreenOrientation = onToggleOrientation,
                                                onShowToast = { msg -> viewModel.showToast(msg) }
                                            )
                                        }
                                        is AppOverlay.PhotoEditor -> {
                                            PhotoEditorScreen(
                                                item = overlay.item,
                                                settings = settings,
                                                copiedEditState = copiedEditState,
                                                onSetEditorAppearance = { mode -> viewModel.setEditorAppearanceOverride(mode) },
                                                onCopyEdits = { state -> viewModel.copyEdits(state) },
                                                onSaveEdits = { state, asCopy -> viewModel.saveEdits(overlay.item.id, state, asCopy) },
                                                onShowToast = { msg -> viewModel.showToast(msg) },
                                                onCancel = { popOverlay() }
                                            )
                                        }
                                        is AppOverlay.Slideshow -> {
                                            SlideshowScreen(items = overlay.items, onBack = { popOverlay() })
                                        }
                                    }

                                    // Expanding Batch Delete Confirmation Alert
                                    GlassAlertDialog(
                                        visible = showBatchDeleteConfirm,
                                        title = "Delete ${selectedIds.size} selected items?",
                                        message = "Moved to Recently Deleted for ${settings.trashRetentionDays} days.",
                                        confirmLabel = appStrings.delete,
                                        cancelLabel = appStrings.cancel,
                                        isDestructive = true,
                                        origin = TransformOrigin(0.85f, 0.92f),
                                        onConfirm = {
                                            showBatchDeleteConfirm = false
                                            viewModel.batchTrashSelected()
                                        },
                                        onDismiss = { showBatchDeleteConfirm = false }
                                    )

                                    AddToAlbumSheet(
                                        visible = addToAlbumTargetIds != null,
                                        albums = albums,
                                        onSelectAlbum = { albId ->
                                            addToAlbumTargetIds?.let { ids -> viewModel.addMediaToAlbum(albId, ids) }
                                            addToAlbumTargetIds = null
                                        },
                                        onCreateAndAdd = { title ->
                                            val ids = addToAlbumTargetIds.orEmpty()
                                            viewModel.createAlbum(title, ids)
                                            addToAlbumTargetIds = null
                                        },
                                        onDismiss = { addToAlbumTargetIds = null }
                                    )

                                    AdjustDateLocationSheet(
                                        visible = adjustDateLocTargetIds != null,
                                        initialLocation = visibleOnly.firstOrNull { it.id in adjustDateLocTargetIds.orEmpty() }?.locationName.orEmpty(),
                                        onApply = { offsetDays, loc ->
                                            val targetIds = adjustDateLocTargetIds.orEmpty()
                                            val firstItem = visibleOnly.firstOrNull { it.id in targetIds }
                                            val newDate = if (offsetDays != 0 && firstItem != null) {
                                                firstItem.dateTakenMillis + offsetDays * 86_400_000L
                                            } else null
                                            viewModel.adjustDateAndLocation(targetIds, newDate, loc)
                                            adjustDateLocTargetIds = null
                                        },
                                        onDismiss = { adjustDateLocTargetIds = null }
                                    )
                                }
                            }
                        }
                    }
                }

                GlassToastBanner(
                    message = toastMessage,
                    modifier = Modifier.align(Alignment.TopCenter)
                )
            }
        }
    }
}
