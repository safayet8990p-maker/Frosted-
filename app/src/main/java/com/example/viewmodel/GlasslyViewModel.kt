package com.example.viewmodel

import android.app.Application
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.R
import com.example.data.local.CustomAlbumEntity
import com.example.data.local.GlasslyDatabase
import com.example.data.local.SettingsDataStore
import com.example.data.model.AlbumModel
import com.example.data.model.AppLanguage
import com.example.data.model.AppThemeMode
import com.example.data.model.CollectionSectionId
import com.example.data.model.CollectionTileLayout
import com.example.data.model.LibraryFilterType
import com.example.data.model.LibrarySortOrder
import com.example.data.model.MediaItem
import com.example.data.model.MediaPermissionState
import com.example.data.model.MediaType
import com.example.data.model.PhotoEditState
import com.example.data.model.PinnedEntry
import com.example.data.model.SettingsState
import com.example.data.model.TimeSegment
import com.example.data.model.UiState
import com.example.data.repository.MediaRepository
import com.example.glass.GlassConfig
import com.example.glass.GlassStyle
import com.example.glass.isPowerSaveActive
import com.example.glass.isSystemAnimationsRemoved
import com.example.ui.theme.AccentPreset
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class LibraryGroup(
    val key: String,
    val title: String,
    val subtitleLocation: String?,
    val items: List<MediaItem>
)

data class SmartCollectionSummary(
    val id: String,
    val title: String,
    val count: Int,
    val coverUri: String?,
    val coverSampleRes: Int?,
    val iconKey: String
)

class GlasslyViewModel(application: Application) : AndroidViewModel(application) {

    private val db = GlasslyDatabase.getInstance(application)
    private val settingsStore = SettingsDataStore(application)
    val repository = MediaRepository(application, db.glasslyDao())

    private val _permissionState = MutableStateFlow(computePermissionState(application))
    val permissionState: StateFlow<MediaPermissionState> = _permissionState.asStateFlow()

    val settings: StateFlow<SettingsState> = settingsStore.settingsFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = SettingsState()
    )

    private val _allRawMedia = MutableStateFlow<List<MediaItem>>(emptyList())
    private val _isLoadingMedia = MutableStateFlow(true)
    private val _isRefreshing = MutableStateFlow(false)
    private val _mediaError = MutableStateFlow<String?>(null)

    private val _activeTimeSegment = MutableStateFlow(TimeSegment.ALL)
    val activeTimeSegment: StateFlow<TimeSegment> = _activeTimeSegment.asStateFlow()

    private val _activeFilters = MutableStateFlow<Set<LibraryFilterType>>(emptySet())
    val activeFilters: StateFlow<Set<LibraryFilterType>> = _activeFilters.asStateFlow()

    private val _gridColumnsOverride = MutableStateFlow<Int?>(null)
    val gridColumns: StateFlow<Int> = combine(settings, _gridColumnsOverride) { s, override ->
        (override ?: s.defaultGridColumns).coerceIn(2, 6)
    }.stateIn(viewModelScope, SharingStarted.Eagerly, 3)

    private val _isSelectionMode = MutableStateFlow(false)
    val isSelectionMode: StateFlow<Boolean> = _isSelectionMode.asStateFlow()

    private val _selectedMediaIds = MutableStateFlow<Set<String>>(emptySet())
    val selectedMediaIds: StateFlow<Set<String>> = _selectedMediaIds.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _recentViewedIds = MutableStateFlow<List<String>>(emptyList())
    private val _recentEditedIds = MutableStateFlow<List<String>>(emptyList())
    private val _recentSharedIds = MutableStateFlow<List<String>>(emptyList())

    private val _copiedEditState = MutableStateFlow<PhotoEditState?>(null)
    val copiedEditState: StateFlow<PhotoEditState?> = _copiedEditState.asStateFlow()

    private val _isHiddenVaultUnlocked = MutableStateFlow(false)
    val isHiddenVaultUnlocked: StateFlow<Boolean> = _isHiddenVaultUnlocked.asStateFlow()

    private val _toastMessage = MutableStateFlow<String?>(null)
    val toastMessage: StateFlow<String?> = _toastMessage.asStateFlow()

    private val _isRefreshingFlag = _isRefreshing.asStateFlow()
    val isRefreshing: StateFlow<Boolean> = _isRefreshingFlag

    val searchHistory = repository.searchHistoryFlow.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList()
    )

    val glassConfig: StateFlow<GlassConfig> = combine(settings) { (s) ->
        val ctx = getApplication<Application>()
        GlassConfig(
            style = s.glassStyle,
            intensity = s.glassIntensity,
            accentPreset = s.accentPreset,
            reduceGlass = s.reduceGlass,
            reduceMotion = s.reduceMotion || ctx.isSystemAnimationsRemoved(),
            highContrast = s.highContrastGlass,
            hapticsEnabled = s.hapticsEnabled,
            hapticStrength = s.hapticStrength,
            largerTouchTargets = s.largerTouchTargets,
            isPowerSaveFallback = ctx.isPowerSaveActive()
        )
    }.stateIn(viewModelScope, SharingStarted.Eagerly, GlassConfig())

    val customAlbums: StateFlow<List<CustomAlbumEntity>> = repository.customAlbumsFlow
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    private val albumCrossRefs = repository.albumCrossRefsFlow
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val allVisibleMedia: StateFlow<List<MediaItem>> = _allRawMedia

    val libraryMedia: StateFlow<List<MediaItem>> = combine(
        _allRawMedia,
        settings,
        _activeFilters,
        albumCrossRefs
    ) { items, s, filters, refs ->
        val inAnyAlbumIds = refs.map { it.mediaId }.toSet()
        val filtered = items.filter { item ->
            if (item.isTrashed || item.isHidden) return@filter false
            if (!s.includeScreenshotsInLibrary && item.isScreenshot) return@filter false
            if (!s.includeSharedWithYouInLibrary && item.isSharedWithYou) return@filter false

            if (filters.isEmpty()) true
            else filters.all { f ->
                when (f) {
                    LibraryFilterType.FAVORITES -> item.isFavorite
                    LibraryFilterType.EDITED -> item.isEdited
                    LibraryFilterType.PHOTOS -> item.mediaType != MediaType.VIDEO
                    LibraryFilterType.VIDEOS -> item.mediaType == MediaType.VIDEO
                    LibraryFilterType.SCREENSHOTS -> item.isScreenshot || item.mediaType == MediaType.SCREENSHOT
                    LibraryFilterType.SHARED_WITH_YOU -> item.isSharedWithYou
                    LibraryFilterType.NOT_IN_ALBUM -> item.id !in inAnyAlbumIds && item.bucketName == "Camera Roll"
                }
            }
        }

        // iOS Photos order: oldest at top, newest at bottom
        when (s.sortOrder) {
            LibrarySortOrder.DATE_CAPTURED -> filtered.sortedBy { it.dateTakenMillis }
            LibrarySortOrder.RECENTLY_ADDED -> filtered.sortedBy { it.dateAddedMillis }
        }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val libraryUiState: StateFlow<UiState<List<LibraryGroup>>> = combine(
        libraryMedia,
        _isLoadingMedia,
        _mediaError,
        _activeTimeSegment,
        settings
    ) { items, loading, error, segment, s ->
        when {
            loading -> UiState.Loading
            error != null -> UiState.Error(error)
            items.isEmpty() -> UiState.Empty("No matching photos or videos")
            else -> UiState.Success(groupLibraryItems(items, segment, s.showLocationHeaders))
        }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, UiState.Loading)

    val albumsList: StateFlow<List<AlbumModel>> = combine(
        _allRawMedia,
        customAlbums,
        albumCrossRefs,
        settings
    ) { items, customList, refs, s ->
        val visible = items.filter { !it.isTrashed && !it.isHidden }
        val customModels = customList.map { entity ->
            val mediaIds = refs.filter { it.albumId == entity.id }.map { it.mediaId }
            val cover = visible.firstOrNull { it.id in mediaIds }
            AlbumModel(
                id = entity.id,
                title = entity.title,
                coverUri = cover?.uriString,
                coverSampleRes = cover?.sampleDrawableRes ?: R.drawable.img_sample_aegean,
                itemCount = mediaIds.size,
                isCustom = true,
                isPinned = entity.id in s.pinnedIds,
                mediaIds = mediaIds
            )
        }
        val bucketModels = visible.groupBy { it.bucketId }.map { (bucketId, bucketItems) ->
            val latest = bucketItems.maxByOrNull { it.dateTakenMillis }
            AlbumModel(
                id = "bucket_$bucketId",
                title = latest?.bucketName ?: bucketId,
                coverUri = latest?.uriString,
                coverSampleRes = latest?.sampleDrawableRes,
                itemCount = bucketItems.size,
                isCustom = false,
                isPinned = "bucket_$bucketId" in s.pinnedIds,
                mediaIds = bucketItems.map { it.id }
            )
        }
        customModels + bucketModels
    }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val trashedMedia: StateFlow<List<MediaItem>> = combine(_allRawMedia, settings) { items, s ->
        val cutoff = System.currentTimeMillis() - s.trashRetentionDays * 86_400_000L
        items.filter { it.isTrashed && (it.trashedAtMillis == null || it.trashedAtMillis >= cutoff) }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val hiddenMedia: StateFlow<List<MediaItem>> = combine(_allRawMedia, _isHiddenVaultUnlocked) { items, _ ->
        items.filter { it.isHidden && !it.isTrashed }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val duplicateGroups: StateFlow<List<List<MediaItem>>> = combine(_allRawMedia) { (items) ->
        items.filter { !it.isTrashed && !it.isHidden }
            .groupBy { "${it.width}x${it.height}_${it.sizeBytes / 100_000L}" }
            .values
            .filter { it.size > 1 }
            .toList()
    }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val largeFilesMedia: StateFlow<List<MediaItem>> = combine(_allRawMedia) { (items) ->
        items.filter { !it.isTrashed && !it.isHidden && it.sizeBytes >= 5_000_000L }
            .sortedByDescending { it.sizeBytes }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val searchResults: StateFlow<List<MediaItem>> = combine(_allRawMedia, _searchQuery) { items, q ->
        val visible = items.filter { !it.isTrashed && !it.isHidden }
        repository.searchMediaNaturalLanguage(visible, q)
    }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val recentViewedMedia: StateFlow<List<MediaItem>> = combine(_allRawMedia, _recentViewedIds) { items, ids ->
        val visible = items.filter { !it.isTrashed && !it.isHidden }
        val byIds = ids.mapNotNull { id -> visible.find { it.id == id } }
        byIds.ifEmpty { visible.takeLast(4).reversed() }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val recentEditedMedia: StateFlow<List<MediaItem>> = combine(_allRawMedia, _recentEditedIds) { items, ids ->
        val visible = items.filter { !it.isTrashed && !it.isHidden }
        val edited = visible.filter { it.isEdited || it.id in ids }
        edited.ifEmpty { visible.take(2) }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val recentSharedMedia: StateFlow<List<MediaItem>> = combine(_allRawMedia, _recentSharedIds) { items, ids ->
        val visible = items.filter { !it.isTrashed && !it.isHidden }
        val shared = visible.filter { it.isSharedWithYou || it.id in ids }
        shared.ifEmpty { visible.take(2) }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    init {
        viewModelScope.launch {
            combine(
                repository.observeMediaStoreChanges().debounce(250),
                repository.mediaOverridesFlow,
                _permissionState
            ) { _, _, perm -> perm }.collect {
                reloadMedia(showSpinner = false)
            }
        }
        viewModelScope.launch {
            reloadMedia(showSpinner = true)
        }
    }

    fun refreshPermissionState() {
        _permissionState.value = computePermissionState(getApplication())
        viewModelScope.launch { reloadMedia(showSpinner = false) }
    }

    fun pullToRefresh() {
        viewModelScope.launch {
            _isRefreshing.value = true
            reloadMedia(showSpinner = false)
            delay(350)
            _isRefreshing.value = false
        }
    }

    private suspend fun reloadMedia(showSpinner: Boolean) {
        if (showSpinner) _isLoadingMedia.value = true
        _mediaError.value = null
        runCatching {
            repository.loadLibraryMedia(_permissionState.value)
        }.onSuccess { list ->
            _allRawMedia.value = list
            _isLoadingMedia.value = false
        }.onFailure { err ->
            _mediaError.value = err.localizedMessage ?: "Failed to load media"
            _isLoadingMedia.value = false
        }
    }

    fun showToast(message: String) {
        _toastMessage.value = message
        viewModelScope.launch {
            delay(2400)
            if (_toastMessage.value == message) _toastMessage.value = null
        }
    }

    // --- Library controls ---
    fun setTimeSegment(segment: TimeSegment) {
        _activeTimeSegment.value = segment
    }

    fun setSortOrder(order: LibrarySortOrder) {
        updateSettings { it.copy(sortOrder = order) }
    }

    fun toggleFilter(filter: LibraryFilterType) {
        _activeFilters.update { set ->
            if (filter in set) set - filter else set + filter
        }
    }

    fun clearAllFilters() {
        _activeFilters.value = emptySet()
    }

    fun zoomInGrid() {
        val current = gridColumns.value
        _gridColumnsOverride.value = (current - 1).coerceAtLeast(2)
    }

    fun zoomOutGrid() {
        val current = gridColumns.value
        _gridColumnsOverride.value = (current + 1).coerceAtMost(6)
    }

    fun setGridColumns(cols: Int) {
        _gridColumnsOverride.value = cols.coerceIn(2, 6)
    }

    fun toggleSquareGrid(square: Boolean) {
        updateSettings { it.copy(squareGrid = square) }
    }

    fun toggleIncludeScreenshots() {
        updateSettings { it.copy(includeScreenshotsInLibrary = !it.includeScreenshotsInLibrary) }
    }

    fun toggleIncludeSharedWithYou() {
        updateSettings { it.copy(includeSharedWithYouInLibrary = !it.includeSharedWithYouInLibrary) }
    }

    // --- Selection ---
    fun setSelectionMode(enabled: Boolean) {
        _isSelectionMode.value = enabled
        if (!enabled) _selectedMediaIds.value = emptySet()
    }

    fun toggleMediaSelection(id: String) {
        _selectedMediaIds.update { set ->
            if (id in set) set - id else set + id
        }
    }

    fun setMediaSelected(id: String, selected: Boolean) {
        _selectedMediaIds.update { set ->
            if (selected) set + id else set - id
        }
    }

    fun selectAllVisible(items: List<MediaItem>) {
        _selectedMediaIds.value = items.map { it.id }.toSet()
    }

    // --- Media actions ---
    fun toggleFavorite(mediaId: String) {
        val item = _allRawMedia.value.find { it.id == mediaId } ?: return
        viewModelScope.launch {
            repository.toggleFavorite(mediaId, !item.isFavorite)
            showToast(if (!item.isFavorite) "Added to Favourites" else "Removed from Favourites")
        }
    }

    fun batchFavoriteSelected() {
        val ids = _selectedMediaIds.value.toList()
        if (ids.isEmpty()) return
        viewModelScope.launch {
            ids.forEach { repository.toggleFavorite(it, true) }
            showToast("${ids.size} items added to Favourites")
            setSelectionMode(false)
        }
    }

    fun batchHideSelected() {
        val ids = _selectedMediaIds.value.toList()
        if (ids.isEmpty()) return
        viewModelScope.launch {
            ids.forEach { repository.setHidden(it, true) }
            showToast("${ids.size} items moved to Hidden")
            setSelectionMode(false)
        }
    }

    fun hideSingle(mediaId: String) {
        viewModelScope.launch {
            repository.setHidden(mediaId, true)
            showToast("Moved to Hidden")
        }
    }

    fun unhideSingle(mediaId: String) {
        viewModelScope.launch {
            repository.setHidden(mediaId, false)
            showToast("Restored from Hidden")
        }
    }

    fun batchTrashSelected() {
        val ids = _selectedMediaIds.value.toList()
        if (ids.isEmpty()) return
        viewModelScope.launch {
            ids.forEach { repository.moveToTrash(it) }
            showToast("${ids.size} items moved to Recently Deleted")
            setSelectionMode(false)
        }
    }

    fun trashSingle(mediaId: String) {
        viewModelScope.launch {
            repository.moveToTrash(mediaId)
            showToast("Moved to Recently Deleted")
        }
    }

    fun restoreFromTrash(mediaId: String) {
        viewModelScope.launch {
            repository.restoreFromTrash(mediaId)
            showToast("Restored to Library")
        }
    }

    fun deleteForever(mediaId: String) {
        viewModelScope.launch {
            repository.deletePermanently(mediaId)
            showToast("Deleted permanently")
        }
    }

    fun emptyTrash() {
        val items = trashedMedia.value
        viewModelScope.launch {
            items.forEach { repository.deletePermanently(it.id) }
            showToast("Trash emptied")
        }
    }

    fun copyMediaToClipboard(ids: List<String>) {
        val names = _allRawMedia.value.filter { it.id in ids }.joinToString(", ") { it.displayName }
        val cm = getApplication<Application>().getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
        cm?.setPrimaryClip(ClipData.newPlainText("Glassly Media", names.ifBlank { "Selected photos" }))
        showToast("Copied ${ids.size} item(s)")
    }

    fun duplicateMedia(ids: List<String>) {
        viewModelScope.launch {
            ids.forEach { id ->
                val src = _allRawMedia.value.find { it.id == id }
                if (src != null) {
                    val dupId = "dup_${src.id}_${System.currentTimeMillis()}"
                    val dup = src.copy(
                        id = dupId,
                        displayName = "Copy_${src.displayName}",
                        dateAddedMillis = System.currentTimeMillis()
                    )
                    _allRawMedia.update { it + dup }
                }
            }
            showToast("Duplicated ${ids.size} item(s)")
            setSelectionMode(false)
        }
    }

    fun adjustDateAndLocation(ids: List<String>, dateMillis: Long?, location: String?) {
        viewModelScope.launch {
            ids.forEach { repository.updateDateAndLocation(it, dateMillis, location) }
            showToast("Updated metadata for ${ids.size} item(s)")
            setSelectionMode(false)
        }
    }

    // --- Viewer & Editor ---
    fun markViewed(mediaId: String) {
        _recentViewedIds.update { (listOf(mediaId) + it).distinct().take(12) }
    }

    fun markShared(mediaIds: List<String>) {
        _recentSharedIds.update { (mediaIds + it).distinct().take(12) }
    }

    fun saveEdits(mediaId: String, state: PhotoEditState, saveAsCopy: Boolean) {
        viewModelScope.launch {
            if (saveAsCopy) {
                val src = _allRawMedia.value.find { it.id == mediaId }
                if (src != null) {
                    val copyId = "edit_${src.id}_${System.currentTimeMillis()}"
                    val copyItem = src.copy(
                        id = copyId,
                        displayName = "Edited_${src.displayName}",
                        dateAddedMillis = System.currentTimeMillis(),
                        isEdited = state.hasChanges,
                        editState = state
                    )
                    _allRawMedia.update { it + copyItem }
                    repository.saveEditState(copyId, state)
                    _recentEditedIds.update { (listOf(copyId) + it).distinct().take(12) }
                }
                showToast("Saved edited copy")
            } else {
                repository.saveEditState(mediaId, state)
                _recentEditedIds.update { (listOf(mediaId) + it).distinct().take(12) }
                showToast("Edits saved")
            }
        }
    }

    fun copyEdits(state: PhotoEditState) {
        _copiedEditState.value = state
        showToast("Edits copied")
    }

    fun setEditorAppearanceOverride(mode: AppThemeMode) {
        updateSettings { it.copy(editorAppearanceOverride = mode) }
    }

    // --- Collections, Albums & Pinned ---
    fun setCollectionLayout(layout: CollectionTileLayout) {
        updateSettings { it.copy(collectionTileLayout = layout) }
    }

    fun toggleSectionCollapsed(id: CollectionSectionId) {
        updateSettings { s ->
            val next = if (id in s.collapsedSections) s.collapsedSections - id else s.collapsedSections + id
            s.copy(collapsedSections = next)
        }
    }

    fun showAllSections() {
        updateSettings { it.copy(collapsedSections = emptySet()) }
    }

    fun collapseAllSections() {
        updateSettings { it.copy(collapsedSections = CollectionSectionId.entries.toSet()) }
    }

    fun moveCollectionSection(fromIndex: Int, toIndex: Int) {
        updateSettings { s ->
            val list = s.sectionOrder.toMutableList()
            if (fromIndex in list.indices && toIndex in list.indices) {
                val item = list.removeAt(fromIndex)
                list.add(toIndex, item)
            }
            s.copy(sectionOrder = list)
        }
    }

    fun togglePinItem(id: String) {
        updateSettings { s ->
            val list = s.pinnedIds.toMutableList()
            if (id in list) {
                list.remove(id)
                showToast("Unpinned")
            } else {
                list.add(id)
                showToast("Pinned")
            }
            s.copy(pinnedIds = list)
        }
    }

    fun movePinnedItem(fromIndex: Int, toIndex: Int) {
        updateSettings { s ->
            val list = s.pinnedIds.toMutableList()
            if (fromIndex in list.indices && toIndex in list.indices) {
                val item = list.removeAt(fromIndex)
                list.add(toIndex, item)
            }
            s.copy(pinnedIds = list)
        }
    }

    fun createAlbum(title: String, initialMediaIds: List<String> = emptyList()) {
        if (title.isBlank()) return
        viewModelScope.launch {
            val id = repository.createCustomAlbum(title)
            if (initialMediaIds.isNotEmpty()) {
                repository.addMediaToAlbum(id, initialMediaIds)
            }
            showToast("Created album \"$title\"")
        }
    }

    fun renameAlbum(albumId: String, newTitle: String) {
        if (newTitle.isBlank()) return
        viewModelScope.launch {
            repository.renameCustomAlbum(albumId, newTitle)
            showToast("Renamed album")
        }
    }

    fun deleteAlbum(albumId: String) {
        viewModelScope.launch {
            repository.deleteCustomAlbum(albumId)
            showToast("Deleted album")
        }
    }

    fun addMediaToAlbum(albumId: String, mediaIds: List<String>) {
        viewModelScope.launch {
            repository.addMediaToAlbum(albumId, mediaIds)
            showToast("Added ${mediaIds.size} item(s) to album")
            setSelectionMode(false)
        }
    }

    // --- Search ---
    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun submitSearch(query: String) {
        _searchQuery.value = query
        viewModelScope.launch { repository.recordSearch(query) }
    }

    fun clearSearchHistory() {
        viewModelScope.launch { repository.clearSearchHistory() }
    }

    // --- Vault ---
    fun unlockVault() {
        _isHiddenVaultUnlocked.value = true
    }

    fun lockVault() {
        _isHiddenVaultUnlocked.value = false
    }

    // --- Settings ---
    fun updateSettings(transform: (SettingsState) -> SettingsState) {
        viewModelScope.launch { settingsStore.update(transform) }
    }

    fun completeOnboarding() {
        updateSettings { it.copy(onboardingCompleted = true) }
    }

    fun resetAllSettings() {
        viewModelScope.launch {
            settingsStore.resetAllSettings()
            _gridColumnsOverride.value = null
            showToast("All settings reset to defaults")
        }
    }

    fun buildSmartCollections(visibleItems: List<MediaItem>): List<SmartCollectionSummary> {
        fun summary(id: String, title: String, iconKey: String, predicate: (MediaItem) -> Boolean): SmartCollectionSummary {
            val matching = visibleItems.filter(predicate)
            val cover = matching.lastOrNull()
            return SmartCollectionSummary(
                id = id,
                title = title,
                count = matching.size,
                coverUri = cover?.uriString,
                coverSampleRes = cover?.sampleDrawableRes ?: R.drawable.img_sample_aegean,
                iconKey = iconKey
            )
        }
        return listOf(
            summary("favorites", "Favourites", "favorite") { it.isFavorite },
            summary("videos", "Videos", "video") { it.mediaType == MediaType.VIDEO },
            summary("screenshots", "Screenshots", "screenshot") { it.isScreenshot || it.mediaType == MediaType.SCREENSHOT },
            summary("selfies", "Selfies", "selfie") { it.isSelfie },
            summary("recently_saved", "Recently Saved", "recents") { true },
            summary("raw", "RAW", "raw") { it.mediaType == MediaType.RAW },
            summary("gifs", "Animated", "gif") { it.mediaType == MediaType.GIF },
            summary("motion", "Live / Motion", "motion") { it.mediaType == MediaType.MOTION_PHOTO }
        )
    }

    fun resolvePinnedEntries(
        pinnedIds: List<String>,
        visibleItems: List<MediaItem>,
        albums: List<AlbumModel>
    ): List<PinnedEntry> {
        val smart = buildSmartCollections(visibleItems).associateBy { it.id }
        val albumMap = albums.associateBy { it.id }
        return pinnedIds.map { id ->
            val sm = smart[id]
            val al = albumMap[id]
            when {
                sm != null -> PinnedEntry(id, sm.title, "${sm.count} items", sm.iconKey, sm.coverUri, sm.coverSampleRes)
                al != null -> PinnedEntry(id, al.title, "${al.itemCount} items", "album", al.coverUri, al.coverSampleRes)
                id == "recently_deleted" -> PinnedEntry(id, "Recently Deleted", "${trashedMedia.value.size} items", "trash", null, R.drawable.img_sample_alpine)
                id == "hidden" -> PinnedEntry(id, "Hidden", "${hiddenMedia.value.size} items", "lock", null, R.drawable.img_backdrop_aurora)
                id == "map" -> PinnedEntry(id, "Map", "${visibleItems.count { it.locationName != null }} places", "map", null, R.drawable.img_sample_aegean)
                id == "duplicates" -> PinnedEntry(id, "Duplicates", "${duplicateGroups.value.size} sets", "duplicates", null, R.drawable.img_sample_poppy)
                else -> PinnedEntry(id, id.replaceFirstChar { it.uppercase() }, "Collection", "collection", null, R.drawable.img_sample_aegean)
            }
        }
    }

    private fun groupLibraryItems(
        items: List<MediaItem>,
        segment: TimeSegment,
        showLocation: Boolean
    ): List<LibraryGroup> {
        if (segment == TimeSegment.ALL) {
            return listOf(
                LibraryGroup(
                    key = "all_photos",
                    title = "All Photos",
                    subtitleLocation = if (showLocation) items.lastOrNull()?.locationName else null,
                    items = items
                )
            )
        }
        val fmt = when (segment) {
            TimeSegment.YEARS -> SimpleDateFormat("yyyy", Locale.getDefault())
            TimeSegment.MONTHS -> SimpleDateFormat("MMMM yyyy", Locale.getDefault())
            TimeSegment.DAYS -> SimpleDateFormat("EEE, MMM d, yyyy", Locale.getDefault())
            TimeSegment.ALL -> SimpleDateFormat("yyyy", Locale.getDefault())
        }
        return items.groupBy { fmt.format(Date(it.dateTakenMillis)) }
            .entries
            .map { (title, list) ->
                val loc = if (showLocation) list.mapNotNull { it.locationName }.distinct().take(2).joinToString(" • ").ifBlank { null } else null
                LibraryGroup(key = title, title = title, subtitleLocation = loc, items = list)
            }
    }

    companion object {
        fun computePermissionState(context: Context): MediaPermissionState {
            return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                val img = ContextCompat.checkSelfPermission(context, android.Manifest.permission.READ_MEDIA_IMAGES) == PackageManager.PERMISSION_GRANTED
                val vid = ContextCompat.checkSelfPermission(context, android.Manifest.permission.READ_MEDIA_VIDEO) == PackageManager.PERMISSION_GRANTED
                val partial = ContextCompat.checkSelfPermission(context, android.Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED) == PackageManager.PERMISSION_GRANTED
                when {
                    img && vid -> MediaPermissionState.FULL
                    partial || img || vid -> MediaPermissionState.PARTIAL
                    else -> MediaPermissionState.DENIED
                }
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                val img = ContextCompat.checkSelfPermission(context, android.Manifest.permission.READ_MEDIA_IMAGES) == PackageManager.PERMISSION_GRANTED
                val vid = ContextCompat.checkSelfPermission(context, android.Manifest.permission.READ_MEDIA_VIDEO) == PackageManager.PERMISSION_GRANTED
                when {
                    img && vid -> MediaPermissionState.FULL
                    img || vid -> MediaPermissionState.PARTIAL
                    else -> MediaPermissionState.DENIED
                }
            } else {
                val read = ContextCompat.checkSelfPermission(context, android.Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
                if (read) MediaPermissionState.FULL else MediaPermissionState.DENIED
            }
        }
    }
}
