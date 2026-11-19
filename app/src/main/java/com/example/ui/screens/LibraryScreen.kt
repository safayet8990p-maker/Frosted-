package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.AddPhotoAlternate
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.EditCalendar
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.FileCopy
import androidx.compose.material.icons.rounded.FilterList
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.MoreHoriz
import androidx.compose.material.icons.rounded.MotionPhotosOn
import androidx.compose.material.icons.rounded.PersonOff
import androidx.compose.material.icons.rounded.PhotoLibrary
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Slideshow
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material.icons.rounded.ZoomIn
import androidx.compose.material.icons.rounded.ZoomOut
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.example.data.model.LibraryFilterType
import com.example.data.model.LibrarySortOrder
import com.example.data.model.MediaItem
import com.example.data.model.MediaPermissionState
import com.example.data.model.MediaType
import com.example.data.model.SettingsState
import com.example.data.model.TimeSegment
import com.example.data.model.UiState
import com.example.data.repository.MediaRepository
import com.example.glass.GlassAlertDialog
import com.example.glass.GlassBadge
import com.example.glass.GlassButton
import com.example.glass.GlassCard
import com.example.glass.GlassChip
import com.example.glass.GlassIconButton
import com.example.glass.GlassMenuRow
import com.example.glass.GlassPopoverMenu
import com.example.glass.GlassProgressLoader
import com.example.glass.GlassScrubberBubble
import com.example.glass.GlassVariant
import com.example.glass.glassElevationShadow
import com.example.glass.liquidGlass
import com.example.ui.components.MediaThumbnail
import com.example.ui.components.ShimmerSkeletonBox
import com.example.ui.i18n.LocalAppStrings
import com.example.ui.theme.CornerRadius
import com.example.ui.theme.LocalGlassColors
import com.example.ui.theme.Spacing
import com.example.viewmodel.LibraryGroup
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

private enum class FilterSubmenuPage { ROOT, FILTER, VIEW_OPTIONS }

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun LibraryScreen(
    uiState: UiState<List<LibraryGroup>>,
    settings: SettingsState,
    permissionState: MediaPermissionState,
    gridColumns: Int,
    activeTimeSegment: TimeSegment,
    activeFilters: Set<LibraryFilterType>,
    isSelectionMode: Boolean,
    selectedIds: Set<String>,
    isRefreshing: Boolean,
    gridState: LazyGridState = rememberLazyGridState(),
    backdropModifier: Modifier = Modifier,
    onScrollDirectionChanged: (isScrollingDown: Boolean) -> Unit,
    onRefresh: () -> Unit,
    onRetry: () -> Unit,
    onRequestPermissions: () -> Unit,
    onOpenMedia: (MediaItem) -> Unit,
    onSetGridColumns: (Int) -> Unit,
    onSetSortOrder: (LibrarySortOrder) -> Unit,
    onToggleFilter: (LibraryFilterType) -> Unit,
    onClearFilters: () -> Unit,
    onToggleSquareGrid: (Boolean) -> Unit,
    onToggleIncludeScreenshots: () -> Unit,
    onToggleIncludeShared: () -> Unit,
    onToggleSelectionMode: (Boolean) -> Unit,
    onToggleItemSelected: (String) -> Unit,
    onSelectAll: (List<MediaItem>) -> Unit,
    onBatchCopy: () -> Unit,
    onBatchFavorite: () -> Unit,
    onBatchDuplicate: () -> Unit,
    onBatchHide: () -> Unit,
    onBatchSlideshow: () -> Unit,
    onBatchAddToAlbum: () -> Unit,
    onBatchAdjustDate: () -> Unit,
    onBatchAdjustLocation: () -> Unit,
    onBatchDeleteConfirm: () -> Unit,
    onToggleTimeSegmentsBar: () -> Unit
) {
    val strings = LocalAppStrings.current
    val glassColors = LocalGlassColors.current
    val scope = rememberCoroutineScope()

    var filterMenuOpen by remember { mutableStateOf(false) }
    var filterSubmenuPage by remember { mutableStateOf(FilterSubmenuPage.ROOT) }
    var batchMenuOpen by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var didInitialScroll by remember { mutableStateOf(false) }
    var isScrubbingFast by remember { mutableStateOf(false) }
    var scrubProgress by remember { mutableFloatStateOf(1f) }
    var pinchAccumulator by remember { mutableFloatStateOf(1f) }

    // Track scroll direction for bottom bar shrink/expand
    var prevIndex by remember { mutableStateOf(0) }
    var prevOffset by remember { mutableStateOf(0) }
    LaunchedEffect(gridState.firstVisibleItemIndex, gridState.firstVisibleItemScrollOffset) {
        val currIdx = gridState.firstVisibleItemIndex
        val currOff = gridState.firstVisibleItemScrollOffset
        if (currIdx > prevIndex || (currIdx == prevIndex && currOff > prevOffset + 12)) {
            onScrollDirectionChanged(true)
        } else if (currIdx < prevIndex || (currIdx == prevIndex && currOff < prevOffset - 12)) {
            onScrollDirectionChanged(false)
        }
        prevIndex = currIdx
        prevOffset = currOff
    }

    val flatItems = remember(uiState) {
        (uiState as? UiState.Success)?.data?.flatMap { it.items }.orEmpty()
    }

    // Open at latest photos at bottom like iOS Photos
    LaunchedEffect(uiState, settings.openAtLatest) {
        if (!didInitialScroll && settings.openAtLatest && uiState is UiState.Success && flatItems.isNotEmpty()) {
            val totalCells = uiState.data.sumOf { 1 + it.items.size }
            if (totalCells > 0) {
                gridState.scrollToItem((totalCells - 1).coerceAtLeast(0))
            }
            didInitialScroll = true
        }
    }

    val currentVisibleGroupTitle by remember(uiState, gridState.firstVisibleItemIndex) {
        derivedStateOf {
            val groups = (uiState as? UiState.Success)?.data.orEmpty()
            var cursor = 0
            var activeTitle = groups.firstOrNull()?.title ?: ""
            for (g in groups) {
                if (gridState.firstVisibleItemIndex >= cursor) {
                    activeTitle = g.title
                }
                cursor += 1 + g.items.size
            }
            activeTitle
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(gridColumns) {
                detectTransformGestures { _, _, zoom, _ ->
                    if (zoom != 1f) {
                        pinchAccumulator *= zoom
                        if (pinchAccumulator > 1.24f) {
                            onSetGridColumns(gridColumns - 1)
                            pinchAccumulator = 1f
                        } else if (pinchAccumulator < 0.80f) {
                            onSetGridColumns(gridColumns + 1)
                            pinchAccumulator = 1f
                        }
                    }
                }
            }
    ) {
        PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = onRefresh,
            modifier = Modifier
                .fillMaxSize()
                .then(backdropModifier)
        ) {
            CompositionLocalProvider(com.example.glass.LocalGlassBackdrop provides null) {
            when (uiState) {
                is UiState.Loading -> {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(gridColumns),
                        contentPadding = PaddingValues(top = 112.dp, bottom = 110.dp, start = 2.dp, end = 2.dp),
                        horizontalArrangement = Arrangement.spacedBy(2.dp),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        items(18) {
                            ShimmerSkeletonBox(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .aspectRatio(1f)
                            )
                        }
                    }
                }
                is UiState.Error -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        GlassCard(modifier = Modifier.padding(Spacing.xl)) {
                            Text("Unable to load library", style = MaterialTheme.typography.titleLarge, color = glassColors.textPrimary)
                            Spacer(Modifier.height(Spacing.sm))
                            Text(uiState.message, style = MaterialTheme.typography.bodyMedium, color = glassColors.textSecondary)
                            Spacer(Modifier.height(Spacing.lg))
                            GlassButton(onClick = onRetry, variant = GlassVariant.PROMINENT, tintOverride = glassColors.accentBlue) {
                                Text(strings.retry)
                            }
                        }
                    }
                }
                is UiState.Empty -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        GlassCard(modifier = Modifier.padding(Spacing.xl)) {
                            Text(strings.emptyLibraryTitle, style = MaterialTheme.typography.titleLarge, color = glassColors.textPrimary)
                            Spacer(Modifier.height(Spacing.sm))
                            Text(strings.emptyLibraryBody, style = MaterialTheme.typography.bodyMedium, color = glassColors.textSecondary)
                            Spacer(Modifier.height(Spacing.lg))
                            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                                if (activeFilters.isNotEmpty()) {
                                    GlassButton(onClick = onClearFilters, variant = GlassVariant.TINTED) {
                                        Text("Clear Filters")
                                    }
                                }
                                if (permissionState != MediaPermissionState.FULL) {
                                    GlassButton(onClick = onRequestPermissions, variant = GlassVariant.PROMINENT, tintOverride = glassColors.accentBlue) {
                                        Text(strings.allowAccess)
                                    }
                                }
                            }
                        }
                    }
                }
                is UiState.Success -> {
                    LazyVerticalGrid(
                        state = gridState,
                        columns = GridCells.Fixed(gridColumns),
                        contentPadding = PaddingValues(top = 104.dp, bottom = 128.dp, start = 2.dp, end = 2.dp),
                        horizontalArrangement = Arrangement.spacedBy(2.dp),
                        verticalArrangement = Arrangement.spacedBy(2.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        if (permissionState == MediaPermissionState.PARTIAL) {
                            item(span = { GridItemSpan(maxLineSpan) }) {
                                GlassCard(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = Spacing.md, vertical = Spacing.sm),
                                    cornerRadius = CornerRadius.md
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Column(modifier = Modifier.weight(1f).padding(end = Spacing.sm)) {
                                            Text("Selected Photos Only", style = MaterialTheme.typography.titleSmall, color = glassColors.textPrimary)
                                            Text("Grant full access or pick more items.", style = MaterialTheme.typography.bodySmall, color = glassColors.textSecondary)
                                        }
                                        GlassButton(onClick = onRequestPermissions, variant = GlassVariant.TINTED) {
                                            Text(strings.manageAccess)
                                        }
                                    }
                                }
                            }
                        }

                        uiState.data.forEach { group ->
                            item(
                                key = "header_${group.key}",
                                span = { GridItemSpan(maxLineSpan) }
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = Spacing.md, vertical = Spacing.sm),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(
                                            text = group.title,
                                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                            color = glassColors.textPrimary
                                        )
                                        if (group.subtitleLocation != null) {
                                            Text(
                                                text = group.subtitleLocation,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = glassColors.textSecondary
                                            )
                                        }
                                    }
                                    Text(
                                        text = "${group.items.size}",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = glassColors.textTertiary
                                    )
                                }
                            }

                            items(
                                items = group.items,
                                key = { it.id }
                            ) { media ->
                                val isSelected = media.id in selectedIds
                                val ratio = if (settings.squareGrid) 1f else media.aspectRatio
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .aspectRatio(ratio)
                                        .clip(RoundedCornerShape(if (isSelected) 8.dp else 0.dp))
                                        .border(
                                            width = if (isSelected) 2.5.dp else 0.dp,
                                            color = if (isSelected) glassColors.accentBlue else Color.Transparent,
                                            shape = RoundedCornerShape(if (isSelected) 8.dp else 0.dp)
                                        )
                                        .combinedClickable(
                                            onClick = {
                                                if (isSelectionMode) onToggleItemSelected(media.id)
                                                else onOpenMedia(media)
                                            },
                                            onLongClick = {
                                                if (!isSelectionMode) onToggleSelectionMode(true)
                                                onToggleItemSelected(media.id)
                                            }
                                        )
                                        .testTag("library_media_${media.id}")
                                ) {
                                    MediaThumbnail(
                                        item = media,
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Crop
                                    )

                                    if (settings.showBadges) {
                                        // Top-left favorite heart or motion/RAW/GIF badge
                                        Row(
                                            modifier = Modifier
                                                .align(Alignment.TopStart)
                                                .padding(4.dp),
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            if (media.isFavorite) {
                                                GlassBadge(
                                                    icon = Icons.Rounded.Favorite,
                                                    iconTint = Color(0xFFFF453A),
                                                    contentDescription = "Favourite"
                                                )
                                            }
                                            when (media.mediaType) {
                                                MediaType.RAW -> GlassBadge(text = "RAW")
                                                MediaType.GIF -> GlassBadge(text = "GIF")
                                                MediaType.MOTION_PHOTO -> GlassBadge(icon = Icons.Rounded.MotionPhotosOn)
                                                MediaType.SCREENSHOT -> GlassBadge(text = "SCR")
                                                else -> Unit
                                            }
                                        }
                                        if (media.mediaType == MediaType.VIDEO) {
                                            GlassBadge(
                                                text = MediaRepository.formatDuration(media.durationMs),
                                                icon = Icons.Rounded.PlayArrow,
                                                modifier = Modifier
                                                    .align(Alignment.BottomEnd)
                                                    .padding(4.dp)
                                            )
                                        }
                                    }

                                    if (isSelectionMode) {
                                        Box(
                                            modifier = Modifier
                                                .align(Alignment.BottomEnd)
                                                .padding(6.dp)
                                                .size(24.dp)
                                                .clip(CircleShape)
                                                .background(
                                                    if (isSelected) glassColors.accentBlue
                                                    else Color.Black.copy(alpha = 0.45f)
                                                )
                                                .border(1.5.dp, Color.White, CircleShape),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            if (isSelected) {
                                                Icon(
                                                    imageVector = Icons.Rounded.Check,
                                                    contentDescription = "Selected",
                                                    tint = Color.White,
                                                    modifier = Modifier.size(14.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Fast Scroll Bar with Liquid Glass Date Bubble
                    if (flatItems.size > 6) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.CenterEnd)
                                .padding(end = 6.dp, top = 110.dp, bottom = 120.dp)
                                .width(28.dp)
                                .fillMaxHeight()
                                .pointerInput(flatItems.size) {
                                    detectDragGestures(
                                        onDragStart = { isScrubbingFast = true },
                                        onDragEnd = { isScrubbingFast = false },
                                        onDragCancel = { isScrubbingFast = false }
                                    ) { change, _ ->
                                        change.consume()
                                        val frac = (change.position.y / size.height.toFloat()).coerceIn(0f, 1f)
                                        scrubProgress = frac
                                        val groups = uiState.data
                                        val totalCells = groups.sumOf { 1 + it.items.size }.coerceAtLeast(1)
                                        val target = (frac * (totalCells - 1)).roundToInt()
                                        scope.launch { gridState.scrollToItem(target) }
                                    }
                                },
                            contentAlignment = Alignment.TopEnd
                        ) {
                            Box(
                                modifier = Modifier
                                    .width(4.dp)
                                    .height(38.dp)
                                    .offset { IntOffset(0, ((scrubProgress * 280f).roundToInt())) }
                                    .clip(CircleShape)
                                    .background(glassColors.textPrimary.copy(alpha = 0.35f))
                            )
                        }
                    }
                }
            }
            }
        }

        // Top Floating Liquid Glass Header Bar (matches PDF page 43 & 48)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            glassColors.background.copy(alpha = 0.78f),
                            Color.Transparent
                        )
                    )
                )
                .statusBarsPadding()
                .padding(horizontal = Spacing.lg, vertical = Spacing.sm)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Tapping top-left Library/date jumps straight to oldest photos at top (video 2:11)
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) {
                            scope.launch { gridState.animateScrollToItem(0) }
                        }
                        .testTag("library_oldest_jump_header")
                ) {
                    Text(
                        text = if (isSelectionMode) "Select Items" else strings.tabLibrary,
                        style = MaterialTheme.typography.headlineLarge,
                        color = glassColors.textPrimary
                    )
                    val sub = when {
                        isSelectionMode -> "${selectedIds.size} selected"
                        activeFilters.isNotEmpty() -> "Filter: ${activeFilters.joinToString(", ") { it.name.lowercase().replaceFirstChar { c -> c.uppercase() } }}"
                        else -> "${flatItems.size} items • ${currentVisibleGroupTitle.ifBlank { "All Photos" }}"
                    }
                    Text(
                        text = sub,
                        style = MaterialTheme.typography.labelMedium,
                        color = if (activeFilters.isNotEmpty()) glassColors.accentBlue else glassColors.textSecondary
                    )
                }

                if (isSelectionMode) {
                    Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm), verticalAlignment = Alignment.CenterVertically) {
                        GlassIconButton(
                            icon = Icons.Rounded.MoreHoriz,
                            contentDescription = "Batch actions",
                            onClick = { batchMenuOpen = !batchMenuOpen },
                            size = 44.dp,
                            testTag = "library_batch_more_button"
                        )
                        GlassButton(
                            onClick = { onToggleSelectionMode(false) },
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
                            testTag = "library_cancel_select_button"
                        ) {
                            Text(strings.cancel, style = MaterialTheme.typography.labelLarge)
                        }
                    }
                } else {
                    Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm), verticalAlignment = Alignment.CenterVertically) {
                        GlassIconButton(
                            icon = Icons.Rounded.Tune,
                            contentDescription = "Filter and Sort",
                            onClick = {
                                filterSubmenuPage = FilterSubmenuPage.ROOT
                                filterMenuOpen = !filterMenuOpen
                            },
                            selected = activeFilters.isNotEmpty(),
                            tintOverride = if (activeFilters.isNotEmpty()) glassColors.accentBlue else null,
                            iconTint = if (activeFilters.isNotEmpty()) Color.White else null,
                            size = 44.dp,
                            testTag = "library_filter_button"
                        )
                        GlassButton(
                            onClick = { onToggleSelectionMode(true) },
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
                            testTag = "library_select_button"
                        ) {
                            Text(strings.select, style = MaterialTheme.typography.labelLarge)
                        }
                    }
                }
            }
        }

        // Fast scrub floating glass date bubble
        AnimatedVisibility(
            visible = isScrubbingFast && currentVisibleGroupTitle.isNotBlank(),
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 44.dp)
        ) {
            GlassScrubberBubble(label = currentVisibleGroupTitle)
        }

        // Dismiss scrim for menus
        if (filterMenuOpen || batchMenuOpen) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) {
                        filterMenuOpen = false
                        batchMenuOpen = false
                    }
            )
        }

        // Exact iOS 26 Filter, Sort & View Options Liquid Glass Popover (PDF page 43 & video 1:48–3:37)
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .statusBarsPadding()
                .padding(top = 64.dp, end = Spacing.lg)
        ) {
            GlassPopoverMenu(
                expanded = filterMenuOpen,
                onDismissRequest = { filterMenuOpen = false },
                origin = TransformOrigin(0.85f, 0f),
                width = 285.dp
            ) {
                when (filterSubmenuPage) {
                    FilterSubmenuPage.ROOT -> {
                        GlassMenuRow(
                            label = strings.sortByRecentlyAdded,
                            checked = settings.sortOrder == LibrarySortOrder.RECENTLY_ADDED,
                            onClick = { onSetSortOrder(LibrarySortOrder.RECENTLY_ADDED) },
                            testTag = "menu_sort_recently_added"
                        )
                        GlassMenuRow(
                            label = strings.sortByDateCaptured,
                            checked = settings.sortOrder == LibrarySortOrder.DATE_CAPTURED,
                            onClick = { onSetSortOrder(LibrarySortOrder.DATE_CAPTURED) },
                            testTag = "menu_sort_date_captured"
                        )
                        HorizontalDivider(color = glassColors.divider, modifier = Modifier.padding(vertical = 4.dp))
                        GlassMenuRow(
                            label = strings.filterTitle,
                            subtitle = if (activeFilters.isEmpty()) strings.allItems else "${activeFilters.size} active",
                            leadingIcon = Icons.Rounded.FilterList,
                            trailingIcon = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                            onClick = { filterSubmenuPage = FilterSubmenuPage.FILTER },
                            testTag = "menu_open_filter_submenu"
                        )
                        if (activeFilters.isNotEmpty()) {
                            GlassMenuRow(
                                label = "Remove Filter",
                                leadingIcon = Icons.Rounded.Close,
                                isDestructive = true,
                                onClick = {
                                    onClearFilters()
                                    filterMenuOpen = false
                                }
                            )
                        }
                        HorizontalDivider(color = glassColors.divider, modifier = Modifier.padding(vertical = 4.dp))
                        GlassMenuRow(
                            label = strings.viewOptions,
                            leadingIcon = Icons.Rounded.Tune,
                            trailingIcon = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                            onClick = { filterSubmenuPage = FilterSubmenuPage.VIEW_OPTIONS },
                            testTag = "menu_open_view_options"
                        )
                    }
                    FilterSubmenuPage.FILTER -> {
                        GlassMenuRow(
                            label = "← ${strings.filterTitle} (Multi-Select)",
                            onClick = { filterSubmenuPage = FilterSubmenuPage.ROOT }
                        )
                        HorizontalDivider(color = glassColors.divider)
                        GlassMenuRow(
                            label = strings.allItems,
                            checked = activeFilters.isEmpty(),
                            onClick = onClearFilters
                        )
                        val options = listOf(
                            LibraryFilterType.FAVORITES to strings.favorites,
                            LibraryFilterType.EDITED to strings.edited,
                            LibraryFilterType.PHOTOS to strings.photos,
                            LibraryFilterType.VIDEOS to strings.videos,
                            LibraryFilterType.SCREENSHOTS to strings.screenshots,
                            LibraryFilterType.SHARED_WITH_YOU to strings.sharedWithYou,
                            LibraryFilterType.NOT_IN_ALBUM to strings.notInAlbum
                        )
                        options.forEach { (type, label) ->
                            GlassMenuRow(
                                label = label,
                                checked = type in activeFilters,
                                onClick = { onToggleFilter(type) },
                                testTag = "filter_option_${type.name.lowercase()}"
                            )
                        }
                    }
                    FilterSubmenuPage.VIEW_OPTIONS -> {
                        GlassMenuRow(
                            label = "← ${strings.viewOptions}",
                            onClick = { filterSubmenuPage = FilterSubmenuPage.ROOT }
                        )
                        HorizontalDivider(color = glassColors.divider)
                        GlassMenuRow(
                            label = strings.zoomIn,
                            leadingIcon = Icons.Rounded.ZoomIn,
                            onClick = { onSetGridColumns(gridColumns - 1) }
                        )
                        GlassMenuRow(
                            label = strings.zoomOut,
                            leadingIcon = Icons.Rounded.ZoomOut,
                            onClick = { onSetGridColumns(gridColumns + 1) }
                        )
                        HorizontalDivider(color = glassColors.divider)
                        GlassMenuRow(
                            label = strings.squareGrid,
                            checked = settings.squareGrid,
                            onClick = { onToggleSquareGrid(true) }
                        )
                        GlassMenuRow(
                            label = strings.aspectGrid,
                            checked = !settings.squareGrid,
                            onClick = { onToggleSquareGrid(false) }
                        )
                        HorizontalDivider(color = glassColors.divider)
                        GlassMenuRow(
                            label = "${strings.showSection}: ${strings.screenshots}",
                            checked = settings.includeScreenshotsInLibrary,
                            onClick = onToggleIncludeScreenshots
                        )
                        GlassMenuRow(
                            label = "${strings.showSection}: ${strings.sharedWithYou}",
                            checked = settings.includeSharedWithYouInLibrary,
                            onClick = onToggleIncludeShared
                        )
                        HorizontalDivider(color = glassColors.divider)
                        GlassMenuRow(
                            label = "${strings.years} / ${strings.months} / ${strings.all}",
                            leadingIcon = Icons.Rounded.Schedule,
                            onClick = {
                                filterMenuOpen = false
                                onToggleTimeSegmentsBar()
                            },
                            testTag = "view_options_time_segments"
                        )
                    }
                }
            }
        }

        // Exact Batch Selection Ellipsis Menu (video 3:38–3:59)
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .statusBarsPadding()
                .padding(top = 64.dp, end = Spacing.lg)
        ) {
            GlassPopoverMenu(
                expanded = batchMenuOpen,
                onDismissRequest = { batchMenuOpen = false },
                origin = TransformOrigin(0.8f, 0f),
                width = 260.dp
            ) {
                GlassMenuRow(strings.copy, leadingIcon = Icons.Rounded.ContentCopy, onClick = { batchMenuOpen = false; onBatchCopy() })
                GlassMenuRow(strings.addToFavorites, leadingIcon = Icons.Rounded.FavoriteBorder, onClick = { batchMenuOpen = false; onBatchFavorite() })
                GlassMenuRow(strings.duplicate, leadingIcon = Icons.Rounded.FileCopy, onClick = { batchMenuOpen = false; onBatchDuplicate() })
                GlassMenuRow(strings.hide, leadingIcon = Icons.Rounded.VisibilityOff, onClick = { batchMenuOpen = false; onBatchHide() })
                GlassMenuRow(strings.playAsSlideshow, leadingIcon = Icons.Rounded.Slideshow, onClick = { batchMenuOpen = false; onBatchSlideshow() })
                GlassMenuRow(strings.addToAlbum, leadingIcon = Icons.Rounded.AddPhotoAlternate, onClick = { batchMenuOpen = false; onBatchAddToAlbum() })
                GlassMenuRow(strings.adjustDateTime, leadingIcon = Icons.Rounded.EditCalendar, onClick = { batchMenuOpen = false; onBatchAdjustDate() })
                GlassMenuRow(strings.adjustLocation, leadingIcon = Icons.Rounded.LocationOn, onClick = { batchMenuOpen = false; onBatchAdjustLocation() })
                HorizontalDivider(color = glassColors.divider)
                GlassMenuRow("Select All (${flatItems.size})", leadingIcon = Icons.Rounded.PhotoLibrary, onClick = { batchMenuOpen = false; onSelectAll(flatItems) })
                GlassMenuRow(strings.dontFeaturePerson, leadingIcon = Icons.Rounded.PersonOff, isDestructive = true, onClick = { batchMenuOpen = false })
            }
        }

        // In-place Delete Confirmation Alert (matches PDF page 47)
        GlassAlertDialog(
            visible = showDeleteConfirm,
            title = "Delete ${selectedIds.size} selected items?",
            message = "You can go to \"Recently Deleted\" to recover or permanently delete them within ${settings.trashRetentionDays} days.",
            confirmLabel = strings.delete,
            cancelLabel = strings.cancel,
            isDestructive = true,
            origin = TransformOrigin(0.85f, 0.92f),
            onConfirm = {
                showDeleteConfirm = false
                onBatchDeleteConfirm()
            },
            onDismiss = { showDeleteConfirm = false }
        )

        // Expose delete trigger when parent bottom bar clicks delete
        LaunchedEffect(Unit) {
            // Handled cleanly via callback in MainActivity
        }
    }
}
