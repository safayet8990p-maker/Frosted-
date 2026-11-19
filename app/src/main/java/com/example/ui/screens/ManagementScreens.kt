package com.example.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Backspace
import androidx.compose.material.icons.automirrored.rounded.NavigateBefore
import androidx.compose.material.icons.automirrored.rounded.NavigateNext
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.CollectionsBookmark
import androidx.compose.material.icons.rounded.DeleteForever
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Fingerprint
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.LockOpen
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.RestoreFromTrash
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.AlbumModel
import com.example.data.model.MediaItem
import com.example.data.model.SettingsState
import com.example.data.repository.MediaRepository
import com.example.glass.GlassAlertDialog
import com.example.glass.GlassBadge
import com.example.glass.GlassBottomSheetOverlay
import com.example.glass.GlassButton
import com.example.glass.GlassCard
import com.example.glass.GlassChip
import com.example.glass.GlassFlatSheetRow
import com.example.glass.GlassIconButton
import com.example.glass.GlassTopToolbar
import com.example.glass.GlassVariant
import com.example.glass.liquidGlass
import com.example.ui.components.MediaThumbnail
import com.example.ui.i18n.LocalAppStrings
import com.example.ui.theme.CornerRadius
import com.example.ui.theme.LocalGlassColors
import com.example.ui.theme.Spacing
import kotlinx.coroutines.delay

@Composable
fun TrashScreen(
    trashedItems: List<MediaItem>,
    retentionDays: Int,
    onRestore: (String) -> Unit,
    onDeleteForever: (String) -> Unit,
    onEmptyTrash: () -> Unit,
    onBack: () -> Unit
) {
    val strings = LocalAppStrings.current
    val glassColors = LocalGlassColors.current
    var showEmptyConfirm by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize().background(glassColors.background)) {
        if (trashedItems.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                GlassCard(modifier = Modifier.padding(Spacing.xl)) {
                    Text(strings.recentlyDeleted, style = MaterialTheme.typography.titleLarge, color = glassColors.textPrimary)
                    Spacer(Modifier.height(Spacing.xs))
                    Text(
                        "Photos and videos you delete appear here for $retentionDays days before permanent removal.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = glassColors.textSecondary
                    )
                }
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                contentPadding = PaddingValues(top = 110.dp, bottom = 100.dp, start = 4.dp, end = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(trashedItems, key = { it.id }) { item ->
                    val elapsedDays = ((System.currentTimeMillis() - (item.trashedAtMillis ?: System.currentTimeMillis())) / 86_400_000L).toInt()
                    val daysLeft = (retentionDays - elapsedDays).coerceAtLeast(1)
                    Column(
                        modifier = Modifier
                            .clip(RoundedCornerShape(CornerRadius.sm))
                            .background(glassColors.secondaryBackground)
                    ) {
                        Box(modifier = Modifier.fillMaxWidth().aspectRatio(1f)) {
                            MediaThumbnail(item = item, modifier = Modifier.fillMaxSize())
                            GlassBadge(
                                text = "${daysLeft}d",
                                modifier = Modifier.align(Alignment.BottomCenter).padding(4.dp)
                            )
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            GlassIconButton(
                                icon = Icons.Rounded.RestoreFromTrash,
                                contentDescription = strings.restore,
                                onClick = { onRestore(item.id) },
                                size = 36.dp
                            )
                            GlassIconButton(
                                icon = Icons.Rounded.DeleteForever,
                                contentDescription = strings.deleteForever,
                                isDestructive = true,
                                onClick = { onDeleteForever(item.id) },
                                size = 36.dp
                            )
                        }
                    }
                }
            }
        }

        GlassTopToolbar(
            title = strings.recentlyDeleted,
            subtitle = "${trashedItems.size} items",
            onBackClick = onBack,
            trailingContent = {
                if (trashedItems.isNotEmpty()) {
                    GlassButton(
                        onClick = { showEmptyConfirm = true },
                        isDestructive = true,
                        testTag = "trash_empty_button"
                    ) {
                        Text(strings.emptyTrash)
                    }
                }
            }
        )

        GlassAlertDialog(
            visible = showEmptyConfirm,
            title = "Empty Recently Deleted?",
            message = "All ${trashedItems.size} items will be permanently removed.",
            confirmLabel = strings.emptyTrash,
            cancelLabel = strings.cancel,
            isDestructive = true,
            origin = TransformOrigin(0.9f, 0.1f),
            onConfirm = {
                showEmptyConfirm = false
                onEmptyTrash()
            },
            onDismiss = { showEmptyConfirm = false }
        )
    }
}

@Composable
fun HiddenVaultScreen(
    settings: SettingsState,
    isUnlocked: Boolean,
    hiddenItems: List<MediaItem>,
    onUnlockSuccess: () -> Unit,
    onLockVault: () -> Unit,
    onTriggerBiometric: () -> Unit,
    onUnhide: (String) -> Unit,
    onOpenMedia: (MediaItem) -> Unit,
    onBack: () -> Unit
) {
    val strings = LocalAppStrings.current
    val glassColors = LocalGlassColors.current
    var enteredPin by remember { mutableStateOf("") }
    var pinError by remember { mutableStateOf(false) }

    val unlocked = isUnlocked || !settings.hiddenLockEnabled

    Box(modifier = Modifier.fillMaxSize().background(glassColors.background)) {
        if (!unlocked) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = 110.dp, start = Spacing.xl, end = Spacing.xl, bottom = Spacing.xl),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(Icons.Rounded.Lock, contentDescription = null, tint = glassColors.accentBlue, modifier = Modifier.size(48.dp))
                Spacer(Modifier.height(Spacing.md))
                Text(strings.enterPinPrompt, style = MaterialTheme.typography.headlineMedium, color = glassColors.textPrimary)
                Spacer(Modifier.height(Spacing.xs))
                Text(
                    text = if (pinError) "Incorrect PIN (Default: 2626)" else "Default Vault PIN: ${settings.hiddenVaultPin}",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (pinError) glassColors.destructive else glassColors.textSecondary
                )
                Spacer(Modifier.height(Spacing.lg))

                // PIN Dots
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    repeat(4) { idx ->
                        val filled = idx < enteredPin.length
                        Box(
                            modifier = Modifier
                                .size(16.dp)
                                .clip(RoundedCornerShape(50))
                                .background(if (filled) glassColors.accentBlue else glassColors.flatRowFill)
                        )
                    }
                }
                Spacer(Modifier.height(Spacing.xl))

                // Glass Numeric Keypad
                val rows = listOf(listOf("1", "2", "3"), listOf("4", "5", "6"), listOf("7", "8", "9"))
                Column(verticalArrangement = Arrangement.spacedBy(10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    rows.forEach { row ->
                        Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                            row.forEach { digit ->
                                GlassButton(
                                    onClick = {
                                        pinError = false
                                        val next = enteredPin + digit
                                        if (next.length == 4) {
                                            if (next == settings.hiddenVaultPin) {
                                                enteredPin = ""
                                                onUnlockSuccess()
                                            } else {
                                                pinError = true
                                                enteredPin = ""
                                            }
                                        } else {
                                            enteredPin = next
                                        }
                                    },
                                    isCircle = true,
                                    modifier = Modifier.size(64.dp),
                                    testTag = "vault_pin_$digit"
                                ) {
                                    Text(digit, style = MaterialTheme.typography.titleLarge)
                                }
                            }
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.CenterVertically) {
                        if (settings.biometricUnlockEnabled) {
                            GlassIconButton(
                                icon = Icons.Rounded.Fingerprint,
                                contentDescription = strings.unlockWithBiometric,
                                onClick = onTriggerBiometric,
                                variant = GlassVariant.TINTED,
                                size = 64.dp,
                                testTag = "vault_biometric_button"
                            )
                        } else {
                            Spacer(Modifier.size(64.dp))
                        }
                        GlassButton(
                            onClick = {
                                pinError = false
                                val next = enteredPin + "0"
                                if (next.length == 4) {
                                    if (next == settings.hiddenVaultPin) {
                                        enteredPin = ""
                                        onUnlockSuccess()
                                    } else {
                                        pinError = true
                                        enteredPin = ""
                                    }
                                } else enteredPin = next
                            },
                            isCircle = true,
                            modifier = Modifier.size(64.dp),
                            testTag = "vault_pin_0"
                        ) {
                            Text("0", style = MaterialTheme.typography.titleLarge)
                        }
                        GlassIconButton(
                            icon = Icons.AutoMirrored.Rounded.Backspace,
                            contentDescription = "Backspace",
                            onClick = { if (enteredPin.isNotEmpty()) enteredPin = enteredPin.dropLast(1) },
                            size = 64.dp
                        )
                    }
                }
            }
        } else if (hiddenItems.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                GlassCard(modifier = Modifier.padding(Spacing.xl)) {
                    Text(strings.hiddenAlbum, style = MaterialTheme.typography.titleLarge, color = glassColors.textPrimary)
                    Spacer(Modifier.height(Spacing.xs))
                    Text("No hidden photos or videos yet.", style = MaterialTheme.typography.bodyMedium, color = glassColors.textSecondary)
                }
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                contentPadding = PaddingValues(top = 110.dp, bottom = 100.dp, start = 4.dp, end = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(hiddenItems, key = { it.id }) { item ->
                    Column(
                        modifier = Modifier
                            .clip(RoundedCornerShape(CornerRadius.sm))
                            .background(glassColors.secondaryBackground)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .aspectRatio(1f)
                                .clickable { onOpenMedia(item) }
                        ) {
                            MediaThumbnail(item = item, modifier = Modifier.fillMaxSize())
                        }
                        GlassButton(
                            onClick = { onUnhide(item.id) },
                            variant = GlassVariant.CLEAR,
                            modifier = Modifier.fillMaxWidth().padding(4.dp)
                        ) {
                            Icon(Icons.Rounded.LockOpen, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Unhide", style = MaterialTheme.typography.labelMedium)
                        }
                    }
                }
            }
        }

        GlassTopToolbar(
            title = strings.hiddenAlbum,
            subtitle = if (unlocked) "${hiddenItems.size} items" else "Locked",
            onBackClick = onBack,
            trailingContent = {
                if (unlocked && settings.hiddenLockEnabled) {
                    GlassIconButton(
                        icon = Icons.Rounded.Lock,
                        contentDescription = "Lock Vault",
                        onClick = onLockVault,
                        size = 44.dp
                    )
                }
            }
        )
    }
}

@Composable
fun DuplicatesScreen(
    duplicateGroups: List<List<MediaItem>>,
    onTrashDuplicate: (String) -> Unit,
    onOpenMedia: (MediaItem) -> Unit,
    onBack: () -> Unit
) {
    val strings = LocalAppStrings.current
    val glassColors = LocalGlassColors.current
    Box(modifier = Modifier.fillMaxSize().background(glassColors.background)) {
        if (duplicateGroups.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                GlassCard(modifier = Modifier.padding(Spacing.xl)) {
                    Text(strings.duplicatesTitle, style = MaterialTheme.typography.titleLarge, color = glassColors.textPrimary)
                    Spacer(Modifier.height(Spacing.xs))
                    Text("No duplicate photos or videos detected.", style = MaterialTheme.typography.bodyMedium, color = glassColors.textSecondary)
                }
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(top = 110.dp, bottom = 60.dp, start = Spacing.lg, end = Spacing.lg),
                verticalArrangement = Arrangement.spacedBy(Spacing.md)
            ) {
                items(duplicateGroups) { group ->
                    GlassCard(cornerRadius = CornerRadius.lg) {
                        Text("${group.size} Similar Items • ${MediaRepository.formatBytes(group.first().sizeBytes)}", style = MaterialTheme.typography.titleSmall, color = glassColors.textPrimary)
                        Spacer(Modifier.height(Spacing.sm))
                        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm), modifier = Modifier.fillMaxWidth()) {
                            group.forEachIndexed { idx, item ->
                                Column(modifier = Modifier.weight(1f)) {
                                    MediaThumbnail(
                                        item = item,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .aspectRatio(1f)
                                            .clip(RoundedCornerShape(CornerRadius.sm))
                                            .clickable { onOpenMedia(item) }
                                    )
                                    Spacer(Modifier.height(4.dp))
                                    if (idx > 0) {
                                        GlassButton(
                                            onClick = { onTrashDuplicate(item.id) },
                                            isDestructive = true,
                                            modifier = Modifier.fillMaxWidth(),
                                            contentPadding = PaddingValues(vertical = 6.dp)
                                        ) {
                                            Text("Clean", style = MaterialTheme.typography.labelMedium)
                                        }
                                    } else {
                                        GlassBadge(text = "Keep Best", modifier = Modifier.align(Alignment.CenterHorizontally))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        GlassTopToolbar(title = strings.duplicatesTitle, subtitle = "${duplicateGroups.size} sets", onBackClick = onBack)
    }
}

@Composable
fun LargeFilesScreen(
    largeFiles: List<MediaItem>,
    onTrashItem: (String) -> Unit,
    onOpenMedia: (MediaItem) -> Unit,
    onBack: () -> Unit
) {
    val strings = LocalAppStrings.current
    val glassColors = LocalGlassColors.current
    val totalBytes = remember(largeFiles) { largeFiles.sumOf { it.sizeBytes } }

    Box(modifier = Modifier.fillMaxSize().background(glassColors.background)) {
        if (largeFiles.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                GlassCard(modifier = Modifier.padding(Spacing.xl)) {
                    Text(strings.largeFilesTitle, style = MaterialTheme.typography.titleLarge, color = glassColors.textPrimary)
                    Spacer(Modifier.height(Spacing.xs))
                    Text("No files larger than 5 MB found.", style = MaterialTheme.typography.bodyMedium, color = glassColors.textSecondary)
                }
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(top = 110.dp, bottom = 60.dp, start = Spacing.lg, end = Spacing.lg),
                verticalArrangement = Arrangement.spacedBy(Spacing.sm)
            ) {
                items(largeFiles, key = { it.id }) { item ->
                    GlassCard(onClick = { onOpenMedia(item) }, cornerRadius = CornerRadius.md) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                            MediaThumbnail(
                                item = item,
                                modifier = Modifier.size(60.dp).clip(RoundedCornerShape(CornerRadius.sm))
                            )
                            Spacer(Modifier.width(Spacing.md))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(item.displayName, style = MaterialTheme.typography.titleSmall, color = glassColors.textPrimary, maxLines = 1)
                                Text(
                                    "${MediaRepository.formatBytes(item.sizeBytes)} • ${item.width}×${item.height}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = glassColors.textSecondary
                                )
                            }
                            GlassIconButton(
                                icon = Icons.Rounded.DeleteOutline,
                                contentDescription = "Move to Trash",
                                isDestructive = true,
                                onClick = { onTrashItem(item.id) },
                                size = 40.dp
                            )
                        }
                    }
                }
            }
        }
        GlassTopToolbar(
            title = strings.largeFilesTitle,
            subtitle = "${largeFiles.size} files • ${MediaRepository.formatBytes(totalBytes)}",
            onBackClick = onBack
        )
    }
}

@Composable
fun SlideshowScreen(
    items: List<MediaItem>,
    onBack: () -> Unit
) {
    var index by remember { mutableIntStateOf(0) }
    var isPlaying by remember { mutableStateOf(true) }

    val transition = rememberInfiniteTransition(label = "kenBurns")
    val kenBurnsScale by transition.animateFloat(
        initialValue = 1.0f,
        targetValue = 1.12f,
        animationSpec = infiniteRepeatable(tween(4200, easing = LinearEasing)),
        label = "kenBurnsScale"
    )

    LaunchedEffect(isPlaying, index, items.size) {
        if (isPlaying && items.isNotEmpty()) {
            delay(3600)
            index = (index + 1) % items.size
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        if (items.isNotEmpty()) {
            val current = items[index.coerceIn(0, items.lastIndex)]
            Crossfade(targetState = current, animationSpec = tween(750), label = "slideshowCrossfade") { media ->
                MediaThumbnail(
                    item = media,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            scaleX = kenBurnsScale
                            scaleY = kenBurnsScale
                        }
                )
            }
        }

        // Minimal clean Liquid Glass transport bar
        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = Spacing.xl)
                .liquidGlass(shape = RoundedCornerShape(999.dp), cornerRadius = 999.dp, variant = GlassVariant.REGULAR)
                .padding(horizontal = 12.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            androidx.compose.runtime.CompositionLocalProvider(
                com.example.glass.LocalInsideGlassSurface provides true
            ) {
                GlassIconButton(
                    icon = Icons.AutoMirrored.Rounded.NavigateBefore,
                    contentDescription = "Previous",
                    onClick = { if (items.isNotEmpty()) index = (index - 1 + items.size) % items.size },
                    variant = GlassVariant.CLEAR,
                    size = 44.dp
                )
                GlassIconButton(
                    icon = if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                    contentDescription = if (isPlaying) "Pause" else "Play",
                    onClick = { isPlaying = !isPlaying },
                    variant = GlassVariant.TINTED,
                    size = 48.dp
                )
                GlassIconButton(
                    icon = Icons.AutoMirrored.Rounded.NavigateNext,
                    contentDescription = "Next",
                    onClick = { if (items.isNotEmpty()) index = (index + 1) % items.size },
                    variant = GlassVariant.CLEAR,
                    size = 44.dp
                )
                GlassIconButton(
                    icon = Icons.Rounded.Close,
                    contentDescription = "Close Slideshow",
                    onClick = onBack,
                    variant = GlassVariant.CLEAR,
                    size = 44.dp
                )
            }
        }
    }
}

@Composable
fun AddToAlbumSheet(
    visible: Boolean,
    albums: List<AlbumModel>,
    onSelectAlbum: (String) -> Unit,
    onCreateAndAdd: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val strings = LocalAppStrings.current
    val glassColors = LocalGlassColors.current
    var newTitle by remember { mutableStateOf("") }

    GlassBottomSheetOverlay(
        visible = visible,
        title = strings.addToAlbum,
        onDismiss = onDismiss
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
            OutlinedTextField(
                value = newTitle,
                onValueChange = { newTitle = it },
                label = { Text("New Album") },
                singleLine = true,
                modifier = Modifier.weight(1f)
            )
            GlassIconButton(
                icon = Icons.Rounded.Add,
                contentDescription = "Create and add",
                enabled = newTitle.isNotBlank(),
                variant = GlassVariant.PROMINENT,
                tintOverride = glassColors.accentBlue,
                onClick = {
                    onCreateAndAdd(newTitle)
                    newTitle = ""
                },
                size = 48.dp
            )
        }
        Spacer(Modifier.height(Spacing.md))
        LazyColumn(modifier = Modifier.height(260.dp), verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
            items(albums.filter { it.isCustom }, key = { it.id }) { album ->
                GlassFlatSheetRow(
                    title = album.title,
                    subtitle = "${album.itemCount} items",
                    leadingIcon = Icons.Rounded.CollectionsBookmark,
                    onClick = { onSelectAlbum(album.id) }
                )
            }
        }
    }
}

@Composable
fun AdjustDateLocationSheet(
    visible: Boolean,
    initialLocation: String,
    onApply: (dateOffsetDays: Int, location: String?) -> Unit,
    onDismiss: () -> Unit
) {
    val strings = LocalAppStrings.current
    val glassColors = LocalGlassColors.current
    var locationInput by remember(initialLocation) { mutableStateOf(initialLocation) }
    var dateOffsetDays by remember { mutableIntStateOf(0) }

    GlassBottomSheetOverlay(
        visible = visible,
        title = "${strings.adjustDateTime} & Location",
        onDismiss = onDismiss
    ) {
        Text("Shift Date", style = MaterialTheme.typography.labelLarge, color = glassColors.textSecondary)
        Spacer(Modifier.height(Spacing.xs))
        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
            listOf(-7 to "-7d", -1 to "-1d", 0 to "Unchanged", 1 to "+1d").forEach { (d, lbl) ->
                GlassChip(label = lbl, selected = dateOffsetDays == d, onClick = { dateOffsetDays = d })
            }
        }
        Spacer(Modifier.height(Spacing.md))
        OutlinedTextField(
            value = locationInput,
            onValueChange = { locationInput = it },
            label = { Text("Place / City") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(Spacing.md))
        GlassButton(
            onClick = { onApply(dateOffsetDays, locationInput.ifBlank { null }) },
            variant = GlassVariant.PROMINENT,
            tintOverride = glassColors.accentBlue,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(strings.done, style = MaterialTheme.typography.labelLarge)
        }
    }
}

fun shareMediaItems(context: Context, items: List<MediaItem>, onDone: (String) -> Unit) {
    if (items.isEmpty()) return
    val contentUris = items.mapNotNull {
        if (it.uriString.startsWith("content://")) Uri.parse(it.uriString) else null
    }
    if (contentUris.isEmpty()) {
        val shareText = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, "Shared from Glassly")
            putExtra(Intent.EXTRA_TEXT, items.joinToString("\n") { "${it.displayName} (${it.locationName ?: it.bucketName})" })
        }
        runCatching { context.startActivity(Intent.createChooser(shareText, "Share via Glassly")) }
        onDone("Shared ${items.size} showcase item(s)")
        return
    }
    val intent = if (contentUris.size == 1) {
        Intent(Intent.ACTION_SEND).apply {
            type = items.first().mimeType
            putExtra(Intent.EXTRA_STREAM, contentUris.first())
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    } else {
        Intent(Intent.ACTION_SEND_MULTIPLE).apply {
            type = "*/*"
            putParcelableArrayListExtra(Intent.EXTRA_STREAM, ArrayList(contentUris))
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }
    runCatching { context.startActivity(Intent.createChooser(intent, "Share via Glassly")) }
    onDone("Shared ${items.size} item(s)")
}
