package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.local.SearchHistoryEntity
import com.example.data.model.AlbumModel
import com.example.data.model.MediaItem
import com.example.data.model.MediaType
import com.example.data.repository.MediaRepository
import com.example.glass.GlassBadge
import com.example.glass.GlassButton
import com.example.glass.GlassCard
import com.example.glass.GlassChip
import com.example.glass.GlassIconButton
import com.example.glass.GlassSearchField
import com.example.glass.GlassSegmentedControl
import com.example.glass.GlassVariant
import com.example.ui.components.MediaThumbnail
import com.example.ui.i18n.LocalAppStrings
import com.example.ui.theme.CornerRadius
import com.example.ui.theme.LocalGlassColors
import com.example.ui.theme.Spacing

private enum class SearchResultTab { PHOTOS, COLLECTIONS }

@Composable
fun SearchOverlayScreen(
    query: String,
    searchResults: List<MediaItem>,
    albums: List<AlbumModel>,
    recentViewed: List<MediaItem>,
    recentEdited: List<MediaItem>,
    recentShared: List<MediaItem>,
    history: List<SearchHistoryEntity>,
    onQueryChange: (String) -> Unit,
    onSubmitQuery: (String) -> Unit,
    onClearHistory: () -> Unit,
    onOpenMedia: (MediaItem) -> Unit,
    onOpenAlbum: (AlbumModel) -> Unit,
    onDismissSearch: () -> Unit
) {
    val strings = LocalAppStrings.current
    val glassColors = LocalGlassColors.current
    var resultTab by remember { mutableStateOf(SearchResultTab.PHOTOS) }

    val suggestions = remember(query) {
        val base = listOf("videos last month", "favorites", "screenshots", "Aegean", "Spring", "RAW", "2026")
        if (query.isBlank()) base else base.filter { it.contains(query, ignoreCase = true) }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(glassColors.background)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .imePadding()
        ) {
            // Header row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Spacing.lg, vertical = Spacing.sm),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = strings.tabSearch,
                    style = MaterialTheme.typography.headlineLarge,
                    color = glassColors.textPrimary
                )
                GlassIconButton(
                    icon = Icons.Rounded.Close,
                    contentDescription = "Close Search",
                    onClick = onDismissSearch,
                    size = 44.dp,
                    testTag = "search_close_button"
                )
            }

            if (query.isBlank()) {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(horizontal = Spacing.lg, vertical = Spacing.sm),
                    verticalArrangement = Arrangement.spacedBy(Spacing.lg)
                ) {
                    // Horizontal Recents Cards (Recently Viewed, Recently Edited, Recently Shared)
                    item {
                        Text(
                            text = strings.recents,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = glassColors.textPrimary
                        )
                        Spacer(Modifier.height(Spacing.sm))
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(Spacing.md)) {
                            val cards = listOf(
                                Triple(strings.recentlyViewed, recentViewed.firstOrNull(), "${recentViewed.size} items"),
                                Triple(strings.recentlyEdited, recentEdited.firstOrNull(), "${recentEdited.size} items"),
                                Triple(strings.recentlyShared, recentShared.firstOrNull(), "${recentShared.size} items")
                            )
                            items(cards) { (title, media, sub) ->
                                Box(
                                    modifier = Modifier
                                        .size(width = 156.dp, height = 180.dp)
                                        .clip(RoundedCornerShape(CornerRadius.md))
                                        .clickable { media?.let(onOpenMedia) }
                                ) {
                                    if (media != null) {
                                        MediaThumbnail(item = media, modifier = Modifier.fillMaxSize())
                                    } else {
                                        Box(Modifier.fillMaxSize().background(glassColors.secondaryBackground))
                                    }
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.65f))))
                                    )
                                    Column(modifier = Modifier.align(Alignment.BottomStart).padding(Spacing.md)) {
                                        Text(title, style = MaterialTheme.typography.titleSmall, color = Color.White, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                        Text(sub, style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.8f))
                                    }
                                }
                            }
                        }
                    }

                    // Suggestion Chips
                    item {
                        Text(
                            text = "Try Searching",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = glassColors.textPrimary
                        )
                        Spacer(Modifier.height(Spacing.sm))
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                            items(suggestions) { s ->
                                GlassChip(
                                    label = s,
                                    leadingIcon = Icons.Rounded.AutoAwesome,
                                    onClick = { onSubmitQuery(s) }
                                )
                            }
                        }
                    }

                    // Recent Searches
                    if (history.isNotEmpty()) {
                        item {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Recent Searches", style = MaterialTheme.typography.titleMedium, color = glassColors.textPrimary)
                                GlassButton(onClick = onClearHistory, variant = GlassVariant.CLEAR) {
                                    Text("Clear")
                                }
                            }
                            Spacer(Modifier.height(Spacing.xs))
                            GlassCard(cornerRadius = CornerRadius.lg) {
                                history.forEach { entry ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable { onSubmitQuery(entry.query) }
                                            .padding(vertical = 10.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(Icons.Rounded.History, contentDescription = null, tint = glassColors.textSecondary, modifier = Modifier.size(18.dp))
                                        Spacer(Modifier.width(Spacing.md))
                                        Text(entry.query, style = MaterialTheme.typography.bodyLarge, color = glassColors.textPrimary)
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                // Results Tabs (Photos | Collections)
                Box(modifier = Modifier.padding(horizontal = Spacing.lg, vertical = Spacing.xs)) {
                    GlassSegmentedControl(
                        options = listOf(SearchResultTab.PHOTOS, SearchResultTab.COLLECTIONS),
                        selectedOption = resultTab,
                        onOptionSelected = { resultTab = it },
                        labelProvider = { if (it == SearchResultTab.PHOTOS) "${strings.photos} (${searchResults.size})" else strings.tabCollections }
                    )
                }

                if (resultTab == SearchResultTab.PHOTOS) {
                    if (searchResults.isEmpty()) {
                        Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                            GlassCard(modifier = Modifier.padding(Spacing.xl)) {
                                Text("No results for \"$query\"", style = MaterialTheme.typography.titleMedium, color = glassColors.textPrimary)
                                Spacer(Modifier.height(Spacing.xs))
                                Text("Try \"videos last month\", \"favorites\", or a year/place.", style = MaterialTheme.typography.bodySmall, color = glassColors.textSecondary)
                            }
                        }
                    } else {
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(3),
                            modifier = Modifier.weight(1f),
                            contentPadding = PaddingValues(start = 2.dp, end = 2.dp, top = Spacing.sm, bottom = Spacing.md),
                            horizontalArrangement = Arrangement.spacedBy(2.dp),
                            verticalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            items(searchResults, key = { it.id }) { media ->
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .aspectRatio(1f)
                                        .clickable { onOpenMedia(media) }
                                ) {
                                    MediaThumbnail(item = media, modifier = Modifier.fillMaxSize())
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
                            }
                        }
                    }
                } else {
                    val matchingAlbums = albums.filter { it.title.contains(query, ignoreCase = true) }
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(Spacing.lg),
                        verticalArrangement = Arrangement.spacedBy(Spacing.sm)
                    ) {
                        items(matchingAlbums, key = { it.id }) { album ->
                            GlassCard(onClick = { onOpenAlbum(album) }, cornerRadius = CornerRadius.md) {
                                Text(album.title, style = MaterialTheme.typography.titleMedium, color = glassColors.textPrimary)
                                Text("${album.itemCount} items", style = MaterialTheme.typography.bodySmall, color = glassColors.textSecondary)
                            }
                        }
                    }
                }
            }

            // Live autocomplete pills right above bottom search bar
            if (query.isNotEmpty() && suggestions.isNotEmpty()) {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = Spacing.lg, vertical = Spacing.xs),
                    horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
                ) {
                    items(suggestions) { s ->
                        GlassChip(
                            label = s,
                            leadingIcon = Icons.Rounded.Search,
                            onClick = { onSubmitQuery(s) }
                        )
                    }
                }
            }

            // Bottom Thumb-Reach Liquid Glass Search Field
            GlassSearchField(
                query = query,
                onQueryChange = onQueryChange,
                placeholder = strings.searchPlaceholder,
                onVoiceClick = { onSubmitQuery("videos last month") },
                onClearOrClose = onDismissSearch,
                onSubmit = { onSubmitQuery(query) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Spacing.lg, vertical = Spacing.sm)
            )
        }
    }
}
