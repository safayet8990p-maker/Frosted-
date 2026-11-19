package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items as gridItems
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Apps
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.DragHandle
import androidx.compose.material.icons.rounded.DriveFileRenameOutline
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.MoreHoriz
import androidx.compose.material.icons.rounded.PushPin
import androidx.compose.material.icons.rounded.RemoveCircle
import androidx.compose.material.icons.rounded.AddCircle
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.ViewAgenda
import androidx.compose.material.icons.rounded.ViewQuilt
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.model.AlbumModel
import com.example.data.model.CollectionSectionId
import com.example.data.model.CollectionTileLayout
import com.example.data.model.MediaItem
import com.example.data.model.PinnedEntry
import com.example.data.model.SettingsState
import com.example.glass.GlassAlertDialog
import com.example.glass.GlassBottomSheetOverlay
import com.example.glass.GlassButton
import com.example.glass.GlassCard
import com.example.glass.GlassFlatSheetRow
import com.example.glass.GlassIconButton
import com.example.glass.GlassMenuRow
import com.example.glass.GlassPopoverMenu
import com.example.glass.GlassVariant
import com.example.ui.components.MediaThumbnail
import com.example.ui.i18n.LocalAppStrings
import com.example.ui.theme.CornerRadius
import com.example.ui.theme.LocalGlassColors
import com.example.ui.theme.Spacing
import com.example.ui.theme.SystemGreen
import com.example.ui.theme.SystemRed
import com.example.viewmodel.SmartCollectionSummary

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun CollectionsScreen(
    settings: SettingsState,
    visibleMedia: List<MediaItem>,
    albums: List<AlbumModel>,
    smartCollections: List<SmartCollectionSummary>,
    pinnedEntries: List<PinnedEntry>,
    trashedCount: Int,
    hiddenCount: Int,
    duplicatesCount: Int,
    largeFilesCount: Int,
    onSetLayout: (CollectionTileLayout) -> Unit,
    onToggleSectionCollapsed: (CollectionSectionId) -> Unit,
    onShowAllSections: () -> Unit,
    onCollapseAllSections: () -> Unit,
    onMoveSection: (Int, Int) -> Unit,
    onTogglePin: (String) -> Unit,
    onMovePinned: (Int, Int) -> Unit,
    onCreateAlbum: (String) -> Unit,
    onRenameAlbum: (String, String) -> Unit,
    onDeleteAlbum: (String) -> Unit,
    onOpenAlbum: (AlbumModel) -> Unit,
    onOpenSmartGroup: (String) -> Unit,
    onOpenTrash: () -> Unit,
    onOpenHidden: () -> Unit,
    onOpenDuplicates: () -> Unit,
    onOpenLargeFiles: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenMedia: (MediaItem) -> Unit,
    backdropModifier: Modifier = Modifier
) {
    val strings = LocalAppStrings.current
    val glassColors = LocalGlassColors.current

    var moreMenuOpen by remember { mutableStateOf(false) }
    var isReorderMode by remember { mutableStateOf(false) }
    var showPinnedSheet by remember { mutableStateOf(false) }
    var showCreateAlbumDialog by remember { mutableStateOf(false) }
    var newAlbumName by remember { mutableStateOf("") }
    var renameTargetAlbum by remember { mutableStateOf<AlbumModel?>(null) }
    var renameInput by remember { mutableStateOf("") }
    var contextMenuTarget by remember { mutableStateOf<Pair<String, String>?>(null) } // id to title
    var contextAlbumTarget by remember { mutableStateOf<AlbumModel?>(null) }

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .then(backdropModifier),
            contentPadding = PaddingValues(top = 104.dp, bottom = 124.dp, start = Spacing.lg, end = Spacing.lg),
            verticalArrangement = Arrangement.spacedBy(Spacing.lg)
        ) {
            if (isReorderMode) {
                item {
                    Text(
                        text = "Drag or use arrows to reorder Collections sections",
                        style = MaterialTheme.typography.bodyMedium,
                        color = glassColors.textSecondary
                    )
                }
                itemsIndexed(settings.sectionOrder, key = { _, s -> s.name }) { idx, section ->
                    androidx.compose.runtime.CompositionLocalProvider(com.example.glass.LocalGlassBackdrop provides null) {
                        GlassCard(
                            modifier = Modifier.fillMaxWidth(),
                            cornerRadius = CornerRadius.md,
                            contentPadding = PaddingValues(horizontal = Spacing.md, vertical = Spacing.sm)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = section.title,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = glassColors.textPrimary
                                )
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    GlassIconButton(
                                        icon = Icons.Rounded.ArrowUpward,
                                        contentDescription = "Move up",
                                        onClick = { if (idx > 0) onMoveSection(idx, idx - 1) },
                                        enabled = idx > 0,
                                        variant = GlassVariant.CLEAR,
                                        size = 36.dp
                                    )
                                    GlassIconButton(
                                        icon = Icons.Rounded.ArrowDownward,
                                        contentDescription = "Move down",
                                        onClick = { if (idx < settings.sectionOrder.lastIndex) onMoveSection(idx, idx + 1) },
                                        enabled = idx < settings.sectionOrder.lastIndex,
                                        variant = GlassVariant.CLEAR,
                                        size = 36.dp
                                    )
                                    Icon(Icons.Rounded.DragHandle, contentDescription = null, tint = glassColors.textTertiary)
                                }
                            }
                        }
                    }
                }
            } else {
                settings.sectionOrder.forEach { sectionId ->
                    val isCollapsed = sectionId in settings.collapsedSections
                    item(key = "sec_header_${sectionId.name}") {
                        CollectionSectionHeader(
                            title = sectionId.title,
                            isCollapsed = isCollapsed,
                            trailingActionLabel = if (sectionId == CollectionSectionId.PINNED) strings.edit else if (sectionId == CollectionSectionId.ALBUMS) "+" else null,
                            onTrailingActionClick = {
                                when (sectionId) {
                                    CollectionSectionId.PINNED -> showPinnedSheet = true
                                    CollectionSectionId.ALBUMS -> {
                                        newAlbumName = ""
                                        showCreateAlbumDialog = true
                                    }
                                    else -> Unit
                                }
                            },
                            onToggleCollapse = { onToggleSectionCollapsed(sectionId) }
                        )
                    }

                    if (!isCollapsed) {
                        item(key = "sec_body_${sectionId.name}") {
                            androidx.compose.runtime.CompositionLocalProvider(com.example.glass.LocalGlassBackdrop provides null) {
                            when (sectionId) {
                                CollectionSectionId.PINNED -> {
                                    CollectionTileStrip(
                                        layout = settings.collectionTileLayout,
                                        entries = pinnedEntries.map {
                                            TileDisplay(it.id, it.title, it.subtitle, it.coverUri, it.coverSampleRes)
                                        },
                                        onClick = { tile ->
                                            when (tile.id) {
                                                "recently_deleted" -> onOpenTrash()
                                                "hidden" -> onOpenHidden()
                                                "duplicates" -> onOpenDuplicates()
                                                else -> {
                                                    val alb = albums.find { it.id == tile.id }
                                                    if (alb != null) onOpenAlbum(alb) else onOpenSmartGroup(tile.id)
                                                }
                                            }
                                        },
                                        onLongClick = { tile ->
                                            contextAlbumTarget = null
                                            contextMenuTarget = tile.id to tile.title
                                        }
                                    )
                                }
                                CollectionSectionId.ALBUMS -> {
                                    CollectionTileStrip(
                                        layout = settings.collectionTileLayout,
                                        entries = albums.map {
                                            TileDisplay(it.id, it.title, "${it.itemCount}", it.coverUri, it.coverSampleRes)
                                        },
                                        onClick = { tile ->
                                            albums.find { it.id == tile.id }?.let(onOpenAlbum)
                                        },
                                        onLongClick = { tile ->
                                            contextAlbumTarget = albums.find { it.id == tile.id }
                                            contextMenuTarget = tile.id to tile.title
                                        }
                                    )
                                }
                                CollectionSectionId.MEDIA_TYPES -> {
                                    GlassCard(cornerRadius = CornerRadius.lg) {
                                        smartCollections.filter { it.id in listOf("videos", "selfies", "screenshots", "raw", "gifs", "motion") }
                                            .forEachIndexed { i, sc ->
                                                if (i > 0) HorizontalDivider(color = glassColors.divider)
                                                UtilityCollectionRow(
                                                    title = sc.title,
                                                    count = sc.count,
                                                    onClick = { onOpenSmartGroup(sc.id) },
                                                    onLongClick = { contextMenuTarget = sc.id to sc.title }
                                                )
                                            }
                                    }
                                }
                                CollectionSectionId.UTILITIES -> {
                                    GlassCard(cornerRadius = CornerRadius.lg) {
                                        UtilityCollectionRow(
                                            title = strings.hiddenAlbum,
                                            count = hiddenCount,
                                            leadingIcon = Icons.Rounded.Lock,
                                            onClick = onOpenHidden,
                                            onLongClick = { contextMenuTarget = "hidden" to strings.hiddenAlbum }
                                        )
                                        HorizontalDivider(color = glassColors.divider)
                                        UtilityCollectionRow(
                                            title = strings.recentlyDeleted,
                                            count = trashedCount,
                                            leadingIcon = Icons.Rounded.DeleteOutline,
                                            onClick = onOpenTrash,
                                            onLongClick = { contextMenuTarget = "recently_deleted" to strings.recentlyDeleted }
                                        )
                                        HorizontalDivider(color = glassColors.divider)
                                        UtilityCollectionRow(
                                            title = strings.duplicatesTitle,
                                            count = duplicatesCount,
                                            onClick = onOpenDuplicates,
                                            onLongClick = { contextMenuTarget = "duplicates" to strings.duplicatesTitle }
                                        )
                                        HorizontalDivider(color = glassColors.divider)
                                        UtilityCollectionRow(
                                            title = strings.largeFilesTitle,
                                            count = largeFilesCount,
                                            onClick = onOpenLargeFiles
                                        )
                                    }
                                }
                                CollectionSectionId.MEMORIES, CollectionSectionId.TRIPS, CollectionSectionId.FEATURED_PHOTOS -> {
                                    val highlight = visibleMedia.lastOrNull()
                                    if (highlight != null) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(190.dp)
                                                .clip(RoundedCornerShape(CornerRadius.lg))
                                                .combinedClickable(
                                                    onClick = { onOpenMedia(highlight) },
                                                    onLongClick = { contextMenuTarget = sectionId.name.lowercase() to sectionId.title }
                                                )
                                        ) {
                                            MediaThumbnail(item = highlight, modifier = Modifier.fillMaxSize())
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxSize()
                                                    .background(
                                                        Brush.verticalGradient(
                                                            listOf(Color.Transparent, Color.Black.copy(alpha = 0.65f))
                                                        )
                                                    )
                                            )
                                            Column(
                                                modifier = Modifier
                                                    .align(Alignment.BottomStart)
                                                    .padding(Spacing.lg)
                                            ) {
                                                Text(
                                                    text = highlight.bucketName,
                                                    style = MaterialTheme.typography.headlineMedium,
                                                    color = Color.White
                                                )
                                                Text(
                                                    text = highlight.locationName ?: "${visibleMedia.size} moments",
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    color = Color.White.copy(alpha = 0.85f)
                                                )
                                            }
                                        }
                                    }
                                }
                                else -> {
                                    CollectionTileStrip(
                                        layout = settings.collectionTileLayout,
                                        entries = visibleMedia.takeLast(4).map {
                                            TileDisplay(it.id, it.bucketName, it.locationName ?: it.mediaType.name, it.uriString, it.sampleDrawableRes)
                                        },
                                        onClick = { tile ->
                                            visibleMedia.find { it.id == tile.id }?.let(onOpenMedia)
                                        },
                                        onLongClick = { tile ->
                                            contextMenuTarget = sectionId.name.lowercase() to sectionId.title
                                        }
                                    )
                                }
                            }
                            }
                        }
                    }
                }

                // Bottom Reorder pill matching video 6:10 (decoupled from backdrop source)
                item {
                    androidx.compose.runtime.CompositionLocalProvider(com.example.glass.LocalGlassBackdrop provides null) {
                        Box(
                            modifier = Modifier.fillMaxWidth().padding(top = Spacing.sm),
                            contentAlignment = Alignment.Center
                        ) {
                            GlassButton(
                                onClick = { isReorderMode = true },
                                variant = GlassVariant.TINTED,
                                tintOverride = glassColors.accentBlue.copy(alpha = 0.22f),
                                testTag = "collections_bottom_reorder_button"
                            ) {
                                Text(strings.reorder, color = glassColors.accentBlue, style = MaterialTheme.typography.labelLarge)
                            }
                        }
                    }
                }
            }
        }

        // Top Floating Header (Collections + ... + PG Settings badge)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Brush.verticalGradient(listOf(glassColors.background.copy(alpha = 0.84f), Color.Transparent)))
                .statusBarsPadding()
                .padding(horizontal = Spacing.lg, vertical = Spacing.sm),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = strings.tabCollections,
                style = MaterialTheme.typography.headlineLarge,
                color = glassColors.textPrimary
            )
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm), verticalAlignment = Alignment.CenterVertically) {
                if (isReorderMode) {
                    GlassIconButton(
                        icon = Icons.Rounded.Check,
                        contentDescription = "Finish reordering",
                        onClick = { isReorderMode = false },
                        variant = GlassVariant.PROMINENT,
                        tintOverride = glassColors.accentBlue,
                        iconTint = Color.White,
                        size = 44.dp,
                        testTag = "collections_reorder_done_button"
                    )
                } else {
                    GlassIconButton(
                        icon = Icons.Rounded.MoreHoriz,
                        contentDescription = "Collections menu",
                        onClick = { moreMenuOpen = !moreMenuOpen },
                        size = 44.dp,
                        testTag = "collections_more_button"
                    )
                    // Circular Profile/Settings Badge (PG) matching video 5:02
                    GlassButton(
                        onClick = onOpenSettings,
                        isCircle = true,
                        variant = GlassVariant.TINTED,
                        tintOverride = glassColors.accentBlue.copy(alpha = 0.85f),
                        modifier = Modifier.size(44.dp),
                        contentDescription = strings.tabSettings,
                        testTag = "collections_profile_settings_button"
                    ) {
                        Text("PG", style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold), color = Color.White)
                    }
                }
            }
        }

        if (moreMenuOpen) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { moreMenuOpen = false }
            )
        }

        // Collections "..." Popover with 3-Icon Layout Switcher + Show All / Collapse All / Reorder (video 5:10–6:05)
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .statusBarsPadding()
                .padding(top = 64.dp, end = Spacing.lg)
        ) {
            GlassPopoverMenu(
                expanded = moreMenuOpen,
                onDismissRequest = { moreMenuOpen = false },
                origin = TransformOrigin(0.75f, 0f),
                width = 246.dp
            ) {
                // 3-Icon Layout Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val layouts = listOf(
                        Triple(CollectionTileLayout.TOP_ROW_EMPHASIS, Icons.Rounded.ViewQuilt, "Top Row"),
                        Triple(CollectionTileLayout.DENSE_EQUAL, Icons.Rounded.Apps, "Dense"),
                        Triple(CollectionTileLayout.LARGE_SQUARE, Icons.Rounded.ViewAgenda, "Large")
                    )
                    layouts.forEach { (layout, icon, label) ->
                        val active = settings.collectionTileLayout == layout
                        GlassIconButton(
                            icon = icon,
                            contentDescription = label,
                            onClick = { onSetLayout(layout) },
                            selected = active,
                            tintOverride = if (active) glassColors.accentBlue else null,
                            iconTint = if (active) Color.White else null,
                            modifier = Modifier.weight(1f),
                            size = 44.dp
                        )
                    }
                }
                HorizontalDivider(color = glassColors.divider, modifier = Modifier.padding(vertical = 4.dp))
                GlassMenuRow(strings.showAll, onClick = { onShowAllSections(); moreMenuOpen = false })
                GlassMenuRow(strings.collapseAll, onClick = { onCollapseAllSections(); moreMenuOpen = false })
                GlassMenuRow(strings.reorder, onClick = { isReorderMode = true; moreMenuOpen = false })
                HorizontalDivider(color = glassColors.divider, modifier = Modifier.padding(vertical = 4.dp))
                GlassMenuRow("New Album", leadingIcon = Icons.Rounded.Add, onClick = { moreMenuOpen = false; newAlbumName = ""; showCreateAlbumDialog = true })
                GlassMenuRow(strings.tabSettings, leadingIcon = Icons.Rounded.Settings, onClick = { moreMenuOpen = false; onOpenSettings() })
            }
        }

        // Long-press Pin/Unpin & Album Context Menu
        GlassBottomSheetOverlay(
            visible = contextMenuTarget != null,
            title = contextMenuTarget?.second ?: "",
            onDismiss = { contextMenuTarget = null; contextAlbumTarget = null }
        ) {
            val targetId = contextMenuTarget?.first.orEmpty()
            val isPinned = targetId in settings.pinnedIds
            GlassFlatSheetRow(
                title = if (isPinned) "Unpin from Pinned" else "Pin to Pinned",
                leadingIcon = Icons.Rounded.PushPin,
                onClick = {
                    onTogglePin(targetId)
                    contextMenuTarget = null
                }
            )
            if (contextAlbumTarget?.isCustom == true) {
                Spacer(Modifier.height(Spacing.sm))
                GlassFlatSheetRow(
                    title = "Rename Album",
                    leadingIcon = Icons.Rounded.DriveFileRenameOutline,
                    onClick = {
                        renameTargetAlbum = contextAlbumTarget
                        renameInput = contextAlbumTarget?.title.orEmpty()
                        contextMenuTarget = null
                    }
                )
                Spacer(Modifier.height(Spacing.sm))
                GlassFlatSheetRow(
                    title = "Delete Album",
                    leadingIcon = Icons.Rounded.DeleteOutline,
                    onClick = {
                        contextAlbumTarget?.let { onDeleteAlbum(it.id) }
                        contextMenuTarget = null
                    }
                )
            }
        }

        // Pinned Section Management Sheet (video 6:55–8:20)
        GlassBottomSheetOverlay(
            visible = showPinnedSheet,
            title = strings.pinned,
            onDismiss = { showPinnedSheet = false },
            trailingHeaderAction = {
                GlassButton(onClick = { showPinnedSheet = false }, variant = GlassVariant.TINTED) {
                    Text(strings.done)
                }
            }
        ) {
            LazyColumn(
                modifier = Modifier.height(420.dp),
                verticalArrangement = Arrangement.spacedBy(Spacing.sm)
            ) {
                itemsIndexed(pinnedEntries, key = { _, e -> "pin_${e.id}" }) { idx, entry ->
                    GlassFlatSheetRow(
                        title = entry.title,
                        subtitle = entry.subtitle,
                        onClick = {},
                        leadingIcon = Icons.Rounded.RemoveCircle,
                        trailingContent = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                GlassIconButton(
                                    icon = Icons.Rounded.ArrowUpward,
                                    contentDescription = "Move up",
                                    onClick = { if (idx > 0) onMovePinned(idx, idx - 1) },
                                    enabled = idx > 0,
                                    size = 34.dp
                                )
                                GlassIconButton(
                                    icon = Icons.Rounded.DeleteOutline,
                                    contentDescription = "Unpin ${entry.title}",
                                    onClick = { onTogglePin(entry.id) },
                                    isDestructive = true,
                                    size = 34.dp
                                )
                            }
                        }
                    )
                }

                item {
                    Text(
                        text = strings.suggestions.uppercase(),
                        style = MaterialTheme.typography.labelMedium,
                        color = glassColors.textSecondary,
                        modifier = Modifier.padding(top = Spacing.md)
                    )
                }

                val suggestionPool = listOf(
                    "map" to "Map",
                    "recently_deleted" to strings.recentlyDeleted,
                    "hidden" to strings.hiddenAlbum,
                    "duplicates" to strings.duplicatesTitle,
                    "favorites" to strings.favorites,
                    "videos" to strings.videos,
                    "screenshots" to strings.screenshots,
                    "selfies" to "Selfies"
                ).filter { it.first !in settings.pinnedIds }

                items(suggestionPool, key = { "sug_${it.first}" }) { (id, title) ->
                    GlassFlatSheetRow(
                        title = title,
                        leadingIcon = Icons.Rounded.AddCircle,
                        onClick = { onTogglePin(id) }
                    )
                }

                item {
                    Text(
                        text = strings.anyCollectionOrAlbum.uppercase(),
                        style = MaterialTheme.typography.labelMedium,
                        color = glassColors.textSecondary,
                        modifier = Modifier.padding(top = Spacing.md)
                    )
                }

                items(albums.filter { it.id !in settings.pinnedIds }, key = { "pin_alb_${it.id}" }) { alb ->
                    GlassFlatSheetRow(
                        title = alb.title,
                        subtitle = "${alb.itemCount} items",
                        leadingIcon = Icons.Rounded.AddCircle,
                        onClick = { onTogglePin(alb.id) }
                    )
                }
            }
        }

        // Create Album Dialog
        if (showCreateAlbumDialog) {
            GlassBottomSheetOverlay(
                visible = true,
                title = "New Album",
                onDismiss = { showCreateAlbumDialog = false }
            ) {
                OutlinedTextField(
                    value = newAlbumName,
                    onValueChange = { newAlbumName = it },
                    label = { Text("Album Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(Spacing.md))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                    GlassButton(onClick = { showCreateAlbumDialog = false }, modifier = Modifier.weight(1f)) {
                        Text(strings.cancel)
                    }
                    GlassButton(
                        onClick = {
                            onCreateAlbum(newAlbumName)
                            showCreateAlbumDialog = false
                        },
                        variant = GlassVariant.PROMINENT,
                        tintOverride = glassColors.accentBlue,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Create")
                    }
                }
            }
        }

        // Rename Album Dialog
        if (renameTargetAlbum != null) {
            GlassBottomSheetOverlay(
                visible = true,
                title = "Rename Album",
                onDismiss = { renameTargetAlbum = null }
            ) {
                OutlinedTextField(
                    value = renameInput,
                    onValueChange = { renameInput = it },
                    label = { Text("Album Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(Spacing.md))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                    GlassButton(onClick = { renameTargetAlbum = null }, modifier = Modifier.weight(1f)) {
                        Text(strings.cancel)
                    }
                    GlassButton(
                        onClick = {
                            renameTargetAlbum?.let { onRenameAlbum(it.id, renameInput) }
                            renameTargetAlbum = null
                        },
                        variant = GlassVariant.PROMINENT,
                        tintOverride = glassColors.accentBlue,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Save")
                    }
                }
            }
        }
    }
}

private data class TileDisplay(
    val id: String,
    val title: String,
    val subtitle: String,
    val coverUri: String?,
    val coverSampleRes: Int?
)

@Composable
private fun CollectionSectionHeader(
    title: String,
    isCollapsed: Boolean,
    trailingActionLabel: String?,
    onTrailingActionClick: () -> Unit,
    onToggleCollapse: () -> Unit
) {
    val glassColors = LocalGlassColors.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onToggleCollapse
            )
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                color = glassColors.textPrimary
            )
            Box(
                modifier = Modifier
                    .size(22.dp)
                    .clip(CircleShape)
                    .background(glassColors.flatRowFill),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isCollapsed) Icons.AutoMirrored.Rounded.KeyboardArrowRight else Icons.Rounded.KeyboardArrowDown,
                    contentDescription = if (isCollapsed) "Expand $title" else "Collapse $title",
                    tint = glassColors.textSecondary,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
        if (trailingActionLabel != null) {
            Text(
                text = trailingActionLabel,
                style = MaterialTheme.typography.labelLarge,
                color = glassColors.accentBlue,
                modifier = Modifier
                    .clip(CircleShape)
                    .clickable(onClick = onTrailingActionClick)
                    .padding(horizontal = 10.dp, vertical = 4.dp)
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun CollectionTileStrip(
    layout: CollectionTileLayout,
    entries: List<TileDisplay>,
    onClick: (TileDisplay) -> Unit,
    onLongClick: (TileDisplay) -> Unit
) {
    val tileWidth = when (layout) {
        CollectionTileLayout.TOP_ROW_EMPHASIS -> 164.dp
        CollectionTileLayout.DENSE_EQUAL -> 124.dp
        CollectionTileLayout.LARGE_SQUARE -> 210.dp
    }
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(Spacing.md)
    ) {
        itemsIndexed(entries, key = { _, e -> e.id }) { index, tile ->
            val cardW = if (layout == CollectionTileLayout.TOP_ROW_EMPHASIS && index == 0) 204.dp else tileWidth
            Box(
                modifier = Modifier
                    .width(cardW)
                    .aspectRatio(1f)
                    .clip(RoundedCornerShape(CornerRadius.md))
                    .combinedClickable(
                        onClick = { onClick(tile) },
                        onLongClick = { onLongClick(tile) }
                    )
            ) {
                if (tile.coverSampleRes != null) {
                    androidx.compose.foundation.Image(
                        painter = painterResource(id = tile.coverSampleRes),
                        contentDescription = tile.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Box(modifier = Modifier.fillMaxSize().background(Color(0xFF1E293B)))
                }
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.65f)))
                        )
                )
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(Spacing.md)
                ) {
                    Text(
                        text = tile.title,
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = tile.subtitle,
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White.copy(alpha = 0.85f)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun UtilityCollectionRow(
    title: String,
    count: Int,
    leadingIcon: androidx.compose.ui.graphics.vector.ImageVector? = null,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null
) {
    val glassColors = LocalGlassColors.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.md)) {
            if (leadingIcon != null) {
                Icon(leadingIcon, contentDescription = null, tint = glassColors.accentBlue, modifier = Modifier.size(20.dp))
            }
            Text(title, style = MaterialTheme.typography.bodyLarge, color = glassColors.textPrimary)
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("$count", style = MaterialTheme.typography.bodyMedium, color = glassColors.textSecondary)
            Spacer(Modifier.width(4.dp))
            Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, contentDescription = null, tint = glassColors.textTertiary)
        }
    }
}

@Composable
fun AlbumDetailScreen(
    album: AlbumModel,
    items: List<MediaItem>,
    onBack: () -> Unit,
    onOpenMedia: (MediaItem) -> Unit
) {
    val glassColors = LocalGlassColors.current
    var sortNewestFirst by remember { mutableStateOf(true) }
    val sorted = remember(items, sortNewestFirst) {
        if (sortNewestFirst) items.sortedByDescending { it.dateTakenMillis }
        else items.sortedBy { it.dateTakenMillis }
    }

    Box(modifier = Modifier.fillMaxSize().background(glassColors.background)) {
        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            contentPadding = PaddingValues(top = 110.dp, bottom = 40.dp, start = 2.dp, end = 2.dp),
            horizontalArrangement = Arrangement.spacedBy(2.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            gridItems(sorted, key = { it.id }) { media ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1f)
                        .clickable { onOpenMedia(media) }
                ) {
                    MediaThumbnail(item = media, modifier = Modifier.fillMaxSize())
                }
            }
        }

        com.example.glass.GlassTopToolbar(
            title = album.title,
            subtitle = "${sorted.size} items",
            onBackClick = onBack,
            trailingContent = {
                GlassButton(onClick = { sortNewestFirst = !sortNewestFirst }, variant = GlassVariant.REGULAR) {
                    Text(if (sortNewestFirst) "Newest" else "Oldest")
                }
            }
        )
    }
}
