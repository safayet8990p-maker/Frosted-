package com.example.ui.screens

import android.app.WallpaperManager
import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.annotation.OptIn
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.VolumeOff
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.EditCalendar
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.Forward10
import androidx.compose.material.icons.rounded.Fullscreen
import androidx.compose.material.icons.rounded.Hexagon
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.IosShare
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.MoreHoriz
import androidx.compose.material.icons.rounded.MotionPhotosOn
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PictureInPictureAlt
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Print
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material.icons.rounded.Replay10
import androidx.compose.material.icons.rounded.Slideshow
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material.icons.rounded.Wallpaper
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import android.os.Bundle
import android.os.CancellationSignal
import android.os.ParcelFileDescriptor
import android.print.PageRange
import android.print.PrintAttributes
import android.print.PrintDocumentAdapter
import android.print.PrintDocumentInfo
import android.print.PrintManager
import android.print.pdf.PrintedPdfDocument
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.example.data.model.ExifMetadata
import com.example.data.model.MediaItem
import com.example.data.model.MediaType
import com.example.data.model.SettingsState
import com.example.data.repository.MediaRepository
import com.example.glass.GlassAlertDialog
import com.example.glass.GlassBadge
import com.example.glass.GlassBottomSheetOverlay
import com.example.glass.GlassButton
import com.example.glass.GlassFlatSheetRow
import com.example.glass.GlassIconButton
import com.example.glass.GlassMenuRow
import com.example.glass.GlassPopoverMenu
import com.example.glass.GlassSlider
import com.example.glass.GlassVariant
import com.example.glass.LocalGlassBackdrop
import com.example.glass.LocalGlassConfig
import com.example.glass.LocalInsideGlassSurface
import com.example.glass.LocalTiltLightState
import com.example.glass.glassBackdropSource
import com.example.glass.liquidGlass
import com.example.glass.rememberGlassBackdropState
import com.example.ui.components.MediaThumbnail
import com.example.ui.i18n.LocalAppStrings
import com.example.ui.theme.CornerRadius
import com.example.ui.theme.LocalGlassColors
import com.example.ui.theme.Spacing
import com.example.ui.theme.SystemBlue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt
import androidx.media3.common.MediaItem as ExoMediaItem

@Composable
fun PhotoViewerScreen(
    items: List<MediaItem>,
    initialMediaId: String,
    settings: SettingsState,
    repository: MediaRepository,
    onBack: () -> Unit,
    onShare: (MediaItem) -> Unit,
    onToggleFavorite: (String) -> Unit,
    onOpenEditor: (MediaItem) -> Unit,
    onDeleteConfirm: (String) -> Unit,
    onHide: (String) -> Unit,
    onStartSlideshow: (List<MediaItem>) -> Unit,
    onAdjustDateLocation: (MediaItem) -> Unit,
    onRequestPip: () -> Unit,
    onToggleFullscreenOrientation: () -> Unit,
    onShowToast: (String) -> Unit
) {
    val strings = LocalAppStrings.current
    val glassColors = LocalGlassColors.current
    val config = LocalGlassConfig.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val backdropState = rememberGlassBackdropState()

    val startIndex = remember(items, initialMediaId) {
        items.indexOfFirst { it.id == initialMediaId }.coerceAtLeast(0)
    }
    val pagerState = rememberPagerState(initialPage = startIndex, pageCount = { items.size.coerceAtLeast(1) })
    val currentItem = items.getOrNull(pagerState.currentPage)

    var showChrome by remember { mutableStateOf(true) }
    var showMoreMenu by remember { mutableStateOf(false) }
    var showInfoSheet by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var spatialModeActive by remember { mutableStateOf(false) }
    var dragDismissY by remember { mutableFloatStateOf(0f) }
    var motionPhotoPlaying by remember { mutableStateOf(false) }

    var exifMetadata by remember { mutableStateOf(ExifMetadata()) }

    LaunchedEffect(currentItem?.id) {
        if (currentItem != null) {
            exifMetadata = repository.readExifMetadata(currentItem)
            spatialModeActive = false
        }
    }

    val bgAlpha = (1f - (abs(dragDismissY) / 680f)).coerceIn(0.25f, 1f)
    val bgColor by animateColorAsState(
        targetValue = if (!showChrome) Color.Black else glassColors.background.copy(alpha = bgAlpha),
        animationSpec = tween(220),
        label = "viewerBg"
    )

    val dateLabel = remember(currentItem?.dateTakenMillis) {
        if (currentItem == null) ""
        else SimpleDateFormat("d MMMM yyyy • HH:mm", Locale.getDefault()).format(Date(currentItem.dateTakenMillis))
    }

    androidx.compose.runtime.CompositionLocalProvider(
        com.example.glass.LocalGlassBackdrop provides backdropState
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(bgColor)
        ) {
            if (currentItem == null) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    GlassButton(onClick = onBack) { Text("Back to Library") }
                }
                return@Box
            }

            // Content layer captured into backdrop graphics layer for floating glass viewer bars
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .glassBackdropSource(backdropState)
            ) {
                CompositionLocalProvider(LocalGlassBackdrop provides null) {
                    HorizontalPager(
                        state = pagerState,
                        modifier = Modifier.fillMaxSize(),
                        userScrollEnabled = !spatialModeActive
                    ) { pageIndex ->
                        val pageItem = items[pageIndex]
                        if (pageItem.mediaType == MediaType.VIDEO) {
                            VideoViewerPage(
                                item = pageItem,
                                settings = settings,
                                showChrome = showChrome,
                                onToggleChrome = { showChrome = !showChrome },
                                onRequestPip = onRequestPip,
                                onToggleFullscreen = onToggleFullscreenOrientation
                            )
                        } else {
                            PhotoZoomAndSpatialPage(
                                item = pageItem,
                                spatialModeActive = spatialModeActive && pageIndex == pagerState.currentPage,
                                motionPhotoPlaying = motionPhotoPlaying && pageIndex == pagerState.currentPage,
                                dragDismissY = if (pageIndex == pagerState.currentPage) dragDismissY else 0f,
                                onDragDismissDelta = { delta ->
                                    val next = (dragDismissY + delta).coerceAtLeast(0f)
                                    dragDismissY = next
                                },
                                onDragDismissEnd = {
                                    if (dragDismissY > 210f) {
                                        onBack()
                                    } else {
                                        dragDismissY = 0f
                                    }
                                },
                                onSwipeUpInfo = {
                                    if (settings.swipeUpForInfo) showInfoSheet = true
                                },
                                onSingleTap = { showChrome = !showChrome },
                                onLongPressMotion = { isDown ->
                                    if (pageItem.mediaType == MediaType.MOTION_PHOTO) {
                                        motionPhotoPlaying = isDown
                                    }
                                }
                            )
                        }
                    }
                }
            }

            // Spatial Scene Top Pill Indicator (matches PDF page 31 & video 9:58)
            AnimatedVisibility(
                visible = spatialModeActive && showChrome,
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .statusBarsPadding()
                    .padding(top = 68.dp)
            ) {
                GlassButton(
                    onClick = { spatialModeActive = false },
                    variant = GlassVariant.PROMINENT,
                    tintOverride = SystemBlue,
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                    testTag = "viewer_spatial_active_pill"
                ) {
                    Icon(Icons.Rounded.Hexagon, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(strings.spatialScene, style = MaterialTheme.typography.labelLarge)
                }
            }

            // Motion Photo live pill
            AnimatedVisibility(
                visible = motionPhotoPlaying,
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .statusBarsPadding()
                    .padding(top = 68.dp, start = Spacing.lg)
            ) {
                GlassBadge(
                    text = "LIVE",
                    icon = Icons.Rounded.MotionPhotosOn
                )
            }

            // Top Glass Toolbar (Back, Date/Place, More ...)
            AnimatedVisibility(
                visible = showChrome,
                enter = fadeIn(tween(180)),
                exit = fadeOut(tween(160)),
                modifier = Modifier.align(Alignment.TopCenter)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = Spacing.lg, vertical = Spacing.sm),
                    horizontalAlignment = Alignment.End
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
                            size = 44.dp,
                            testTag = "viewer_back_button"
                        )

                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .padding(horizontal = Spacing.sm)
                                .clip(CircleShape)
                                .liquidGlass(shape = CircleShape, cornerRadius = 999.dp, variant = GlassVariant.REGULAR)
                                .padding(horizontal = 14.dp, vertical = 6.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = currentItem.locationName ?: currentItem.bucketName,
                                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                                color = glassColors.textPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = dateLabel,
                                style = MaterialTheme.typography.labelSmall,
                                color = glassColors.textSecondary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        GlassIconButton(
                            icon = Icons.Rounded.MoreHoriz,
                            contentDescription = "More options",
                            onClick = { showMoreMenu = !showMoreMenu },
                            size = 44.dp,
                            testTag = "viewer_more_button"
                        )
                    }

                    // Hexagonal Spatial Scene Toggle Button right below "..." matching video 9:50
                    if (settings.showSpatialPhotoControl && currentItem.mediaType != MediaType.VIDEO) {
                        Spacer(Modifier.height(Spacing.sm))
                        GlassIconButton(
                            icon = Icons.Rounded.Hexagon,
                            contentDescription = "Toggle Spatial Scene 3D",
                            onClick = { spatialModeActive = !spatialModeActive },
                            selected = spatialModeActive,
                            tintOverride = if (spatialModeActive) SystemBlue else null,
                            iconTint = if (spatialModeActive) Color.White else null,
                            size = 44.dp,
                            testTag = "viewer_spatial_toggle_button"
                        )
                    }
                }
            }

            // Bottom Filmstrip Scrubber + Liquid Glass Action Bar
            AnimatedVisibility(
                visible = showChrome,
                enter = fadeIn(tween(180)),
                exit = fadeOut(tween(160)),
                modifier = Modifier.align(Alignment.BottomCenter)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(bottom = Spacing.sm),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    if (items.size > 1) {
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = Spacing.lg),
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = Spacing.sm)
                        ) {
                            itemsIndexed(items, key = { _, m -> "strip_${m.id}" }) { idx, media ->
                                val selected = idx == pagerState.currentPage
                                Box(
                                    modifier = Modifier
                                        .size(width = if (selected) 46.dp else 34.dp, height = 46.dp)
                                        .clip(RoundedCornerShape(6.dp))
                                        .border(
                                            width = if (selected) 2.dp else 0.dp,
                                            color = if (selected) Color.White else Color.Transparent,
                                            shape = RoundedCornerShape(6.dp)
                                        )
                                        .clickable {
                                            scope.launch { pagerState.animateScrollToPage(idx) }
                                        }
                                ) {
                                    MediaThumbnail(item = media, modifier = Modifier.fillMaxSize())
                                }
                            }
                        }
                    }

                    Row(
                        modifier = Modifier
                            .padding(horizontal = Spacing.lg)
                            .fillMaxWidth()
                            .height(58.dp)
                            .liquidGlass(shape = CircleShape, cornerRadius = 999.dp, variant = GlassVariant.REGULAR)
                            .padding(horizontal = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        CompositionLocalProvider(LocalInsideGlassSurface provides true) {
                            GlassIconButton(
                                icon = Icons.Rounded.IosShare,
                                contentDescription = strings.share,
                                onClick = { onShare(currentItem) },
                                variant = GlassVariant.CLEAR,
                                size = 44.dp,
                                testTag = "viewer_share_button"
                            )
                            GlassIconButton(
                                icon = if (currentItem.isFavorite) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                                contentDescription = strings.favorite,
                                onClick = { onToggleFavorite(currentItem.id) },
                                iconTint = if (currentItem.isFavorite) Color(0xFFFF453A) else null,
                                variant = GlassVariant.CLEAR,
                                size = 44.dp,
                                testTag = "viewer_favorite_button"
                            )
                            GlassIconButton(
                                icon = Icons.Rounded.Info,
                                contentDescription = strings.infoTitle,
                                onClick = { showInfoSheet = true },
                                variant = GlassVariant.CLEAR,
                                size = 44.dp,
                                testTag = "viewer_info_button"
                            )
                            if (currentItem.mediaType != MediaType.VIDEO) {
                                GlassIconButton(
                                    icon = Icons.Rounded.Edit,
                                    contentDescription = strings.edit,
                                    onClick = { onOpenEditor(currentItem) },
                                    variant = GlassVariant.CLEAR,
                                    size = 44.dp,
                                    testTag = "viewer_edit_button"
                                )
                            }
                            GlassIconButton(
                                icon = Icons.Rounded.DeleteOutline,
                                contentDescription = strings.delete,
                                onClick = { showDeleteConfirm = true },
                                isDestructive = true,
                                variant = GlassVariant.CLEAR,
                                size = 44.dp,
                                testTag = "viewer_delete_button"
                            )
                        }
                    }
                }
            }

            if (showMoreMenu) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {
                            showMoreMenu = false
                        }
                )
            }

            // Top-right More Popover Menu
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .statusBarsPadding()
                    .padding(top = 64.dp, end = Spacing.lg)
            ) {
                GlassPopoverMenu(
                    expanded = showMoreMenu,
                    onDismissRequest = { showMoreMenu = false },
                    origin = TransformOrigin(0.92f, 0f),
                    width = 246.dp
                ) {
                    GlassMenuRow(strings.infoTitle, leadingIcon = Icons.Rounded.Info, onClick = { showMoreMenu = false; showInfoSheet = true })
                    GlassMenuRow(strings.playAsSlideshow, leadingIcon = Icons.Rounded.Slideshow, onClick = { showMoreMenu = false; onStartSlideshow(items) })
                    if (currentItem.mediaType != MediaType.VIDEO) {
                        GlassMenuRow("Set as Wallpaper", leadingIcon = Icons.Rounded.Wallpaper, onClick = {
                            showMoreMenu = false
                            scope.launch {
                                val msg = setMediaAsWallpaper(context, currentItem)
                                onShowToast(msg)
                            }
                        })
                        GlassMenuRow("Print Photo", leadingIcon = Icons.Rounded.Print, onClick = {
                            showMoreMenu = false
                            printMediaItem(context, currentItem, onShowToast)
                        })
                    }
                    HorizontalDivider(color = glassColors.divider)
                    GlassMenuRow(strings.adjustDateTime, leadingIcon = Icons.Rounded.EditCalendar, onClick = { showMoreMenu = false; onAdjustDateLocation(currentItem) })
                    GlassMenuRow(strings.adjustLocation, leadingIcon = Icons.Rounded.LocationOn, onClick = { showMoreMenu = false; onAdjustDateLocation(currentItem) })
                    GlassMenuRow(strings.hide, leadingIcon = Icons.Rounded.VisibilityOff, onClick = {
                        showMoreMenu = false
                        onHide(currentItem.id)
                        onBack()
                    })
                }
            }

            // Glass Info & EXIF Panel Sheet
            GlassBottomSheetOverlay(
                visible = showInfoSheet,
                title = strings.infoTitle,
                onDismiss = { showInfoSheet = false }
            ) {
                GlassFlatSheetRow(
                    title = currentItem.displayName,
                    subtitle = "${currentItem.width} × ${currentItem.height} • ${MediaRepository.formatBytes(currentItem.sizeBytes)}",
                    onClick = {}
                )
                Spacer(Modifier.height(Spacing.sm))
                GlassFlatSheetRow(
                    title = "${exifMetadata.cameraModel} (${exifMetadata.lensInfo})",
                    subtitle = "${exifMetadata.iso} • ${exifMetadata.aperture} • ${exifMetadata.shutterSpeed}",
                    onClick = {}
                )
                Spacer(Modifier.height(Spacing.sm))
                GlassFlatSheetRow(
                    title = dateLabel,
                    subtitle = currentItem.filePath,
                    onClick = {}
                )
                if (currentItem.editState.imageDescription.isNotBlank()) {
                    Spacer(Modifier.height(Spacing.sm))
                    GlassFlatSheetRow(
                        title = strings.imageDescription,
                        subtitle = currentItem.editState.imageDescription,
                        onClick = {}
                    )
                }
                if (currentItem.locationName != null) {
                    Spacer(Modifier.height(Spacing.sm))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(104.dp)
                            .clip(RoundedCornerShape(CornerRadius.md))
                            .background(
                                Brush.linearGradient(
                                    listOf(Color(0xFF1D4E89), Color(0xFF00B4D8), Color(0xFF48CAE4))
                                )
                            )
                            .padding(Spacing.md)
                    ) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            drawCircle(Color.White.copy(alpha = 0.22f), radius = 34f, center = Offset(size.width * 0.72f, size.height * 0.5f))
                            drawCircle(Color.White, radius = 10f, center = Offset(size.width * 0.72f, size.height * 0.5f))
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Rounded.LocationOn, contentDescription = null, tint = Color.White)
                            Spacer(Modifier.width(Spacing.sm))
                            Column {
                                Text(currentItem.locationName, style = MaterialTheme.typography.titleSmall, color = Color.White)
                                val coords = if (currentItem.latitude != null && currentItem.longitude != null) {
                                    String.format(Locale.US, "%.4f, %.4f", currentItem.latitude, currentItem.longitude)
                                } else "Captured Location"
                                Text(coords, style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.85f))
                            }
                        }
                    }
                }
            }

            // Delete Confirmation Alert expanding from Delete icon
            GlassAlertDialog(
                visible = showDeleteConfirm,
                title = "Delete this item?",
                message = "It will remain in Recently Deleted for ${settings.trashRetentionDays} days.",
                confirmLabel = strings.delete,
                cancelLabel = strings.cancel,
                isDestructive = true,
                origin = TransformOrigin(0.85f, 0.92f),
                onConfirm = {
                    showDeleteConfirm = false
                    onDeleteConfirm(currentItem.id)
                    if (items.size <= 1) onBack()
                },
                onDismiss = { showDeleteConfirm = false }
            )
        }
    }
}

@Composable
private fun PhotoZoomAndSpatialPage(
    item: MediaItem,
    spatialModeActive: Boolean,
    motionPhotoPlaying: Boolean,
    dragDismissY: Float,
    onDragDismissDelta: (Float) -> Unit,
    onDragDismissEnd: () -> Unit,
    onSwipeUpInfo: () -> Unit,
    onSingleTap: () -> Unit,
    onLongPressMotion: (Boolean) -> Unit
) {
    val tiltState = LocalTiltLightState.current
    val config = LocalGlassConfig.current

    var zoomScale by remember(item.id) { mutableFloatStateOf(1f) }
    var panOffset by remember(item.id) { mutableStateOf(Offset.Zero) }
    var spatialTouchOffset by remember(item.id) { mutableStateOf(Offset.Zero) }

    val motionScale by animateFloatAsState(
        targetValue = if (motionPhotoPlaying) 1.07f else 1f,
        animationSpec = spring(dampingRatio = 0.7f, stiffness = Spring.StiffnessLow),
        label = "motionPhotoPulse"
    )
    val dismissScale = (1f - (abs(dragDismissY) / 1400f)).coerceIn(0.72f, 1f)

    // 3D depth offsets from device tilt + touch drag when Spatial Scene is active
    val tiltX = if (spatialModeActive && !config.reduceMotion) (tiltState.tiltX * 26f + spatialTouchOffset.x) else 0f
    val tiltY = if (spatialModeActive && !config.reduceMotion) (tiltState.tiltY * 26f + spatialTouchOffset.y) else 0f

    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(item.id, spatialModeActive) {
                detectTapGestures(
                    onTap = { onSingleTap() },
                    onDoubleTap = {
                        if (zoomScale > 1.2f) {
                            zoomScale = 1f
                            panOffset = Offset.Zero
                        } else {
                            zoomScale = 2.5f
                        }
                    },
                    onLongPress = { onLongPressMotion(true) },
                    onPress = {
                        tryAwaitRelease()
                        onLongPressMotion(false)
                    }
                )
            }
            .pointerInput(item.id, spatialModeActive) {
                if (spatialModeActive) {
                    detectDragGestures(
                        onDragEnd = { spatialTouchOffset = Offset.Zero },
                        onDragCancel = { spatialTouchOffset = Offset.Zero }
                    ) { change, dragAmount ->
                        change.consume()
                        spatialTouchOffset = Offset(
                            x = (spatialTouchOffset.x + dragAmount.x * 0.25f).coerceIn(-36f, 36f),
                            y = (spatialTouchOffset.y + dragAmount.y * 0.25f).coerceIn(-36f, 36f)
                        )
                    }
                } else {
                    detectTransformGestures { _, pan, zoom, _ ->
                        val nextZoom = (zoomScale * zoom).coerceIn(1f, 4.5f)
                        zoomScale = nextZoom
                        if (nextZoom > 1.05f) {
                            panOffset += pan
                        } else {
                            if (pan.y < -36f) {
                                onSwipeUpInfo()
                            } else if (pan.y > 0f || dragDismissY > 0f) {
                                onDragDismissDelta(pan.y)
                            }
                        }
                    }
                }
            }
            .pointerInput(dragDismissY) {
                detectDragGestures(
                    onDragEnd = { onDragDismissEnd() },
                    onDragCancel = { onDragDismissEnd() }
                ) { _, dragAmount ->
                    if (zoomScale <= 1.05f && !spatialModeActive) {
                        if (dragAmount.y < -40f) onSwipeUpInfo()
                        else if (dragAmount.y > 0f || dragDismissY > 0f) onDragDismissDelta(dragAmount.y)
                    }
                }
            },
        contentAlignment = Alignment.Center
    ) {
        if (spatialModeActive) {
            // Depth Background Layer (soft blur + opposite parallax)
            MediaThumbnail(
                item = item,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        scaleX = 1.18f
                        scaleY = 1.18f
                        translationX = -tiltX * 0.55f
                        translationY = -tiltY * 0.55f
                        alpha = 0.68f
                    }
                    .blur(18.dp)
            )
            // Foreground Subject Parallax Plane
            MediaThumbnail(
                item = item,
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(12.dp)
                    .graphicsLayer {
                        scaleX = 1.08f
                        scaleY = 1.08f
                        translationX = tiltX
                        translationY = tiltY
                        rotationY = (tiltX * 0.18f).coerceIn(-8f, 8f)
                        rotationX = (-tiltY * 0.18f).coerceIn(-8f, 8f)
                        cameraDistance = 14f * density
                    }
            )
        } else {
            MediaThumbnail(
                item = item,
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .fillMaxSize()
                    .offset { IntOffset(0, dragDismissY.roundToInt()) }
                    .graphicsLayer {
                        scaleX = zoomScale * motionScale * dismissScale
                        scaleY = zoomScale * motionScale * dismissScale
                        translationX = panOffset.x
                        translationY = panOffset.y
                    }
            )
        }
    }
}

@OptIn(UnstableApi::class)
@Composable
private fun VideoViewerPage(
    item: MediaItem,
    settings: SettingsState,
    showChrome: Boolean,
    onToggleChrome: () -> Unit,
    onRequestPip: () -> Unit,
    onToggleFullscreen: () -> Unit
) {
    val context = LocalContext.current
    val glassColors = LocalGlassColors.current

    var isPlaying by remember(item.id) { mutableStateOf(settings.autoplayVideos) }
    var isMuted by remember(item.id) { mutableStateOf(false) }
    var isLooping by remember(item.id) { mutableStateOf(settings.loopVideos) }
    var playbackSpeed by remember(item.id) { mutableFloatStateOf(settings.defaultPlaybackSpeed) }
    var durationMs by remember(item.id) { mutableLongStateOf(item.durationMs.coerceAtLeast(10_000L)) }
    var positionMs by remember(item.id) { mutableLongStateOf(0L) }
    var showSpeedMenu by remember { mutableStateOf(false) }

    val useRealExo = !item.isSampleFallback && item.uriString.startsWith("content://")

    val exoPlayer = remember(item.id, useRealExo) {
        if (useRealExo) {
            ExoPlayer.Builder(context).build().apply {
                setMediaItem(ExoMediaItem.fromUri(Uri.parse(item.uriString)))
                repeatMode = if (isLooping) Player.REPEAT_MODE_ONE else Player.REPEAT_MODE_OFF
                setPlaybackSpeed(playbackSpeed)
                volume = if (isMuted) 0f else 1f
                playWhenReady = isPlaying
                prepare()
            }
        } else null
    }

    DisposableEffect(exoPlayer) {
        onDispose { exoPlayer?.release() }
    }

    LaunchedEffect(exoPlayer, isPlaying, isLooping, isMuted, playbackSpeed) {
        if (exoPlayer != null) {
            exoPlayer.playWhenReady = isPlaying
            exoPlayer.repeatMode = if (isLooping) Player.REPEAT_MODE_ONE else Player.REPEAT_MODE_OFF
            exoPlayer.volume = if (isMuted) 0f else 1f
            exoPlayer.setPlaybackSpeed(playbackSpeed)
        }
    }

    // Progress ticker for both real ExoPlayer and sample showcase video
    LaunchedEffect(item.id, isPlaying, playbackSpeed, exoPlayer) {
        while (true) {
            if (exoPlayer != null) {
                if (exoPlayer.duration > 0) durationMs = exoPlayer.duration
                positionMs = exoPlayer.currentPosition.coerceAtLeast(0L)
            } else if (isPlaying) {
                positionMs += (250 * playbackSpeed).roundToInt()
                if (positionMs >= durationMs) {
                    if (isLooping) positionMs = 0L else isPlaying = false
                }
            }
            delay(250)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {
                onToggleChrome()
            }
    ) {
        if (exoPlayer != null) {
            AndroidView(
                factory = { ctx ->
                    PlayerView(ctx).apply {
                        player = exoPlayer
                        useController = false
                        keepScreenOn = settings.keepScreenOn
                        layoutParams = FrameLayout.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                        )
                    }
                },
                modifier = Modifier.fillMaxSize()
            )
        } else {
            // Sample showcase video playback visual
            MediaThumbnail(
                item = item,
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxSize()
            )
        }

        CompositionLocalProvider(LocalGlassBackdrop provides null) {
        // Center Floating Liquid Glass Playback Controls (<<, Play/Pause, >> matching PDF page 28)
        AnimatedVisibility(
            visible = showChrome,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.Center)
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(Spacing.xl),
                verticalAlignment = Alignment.CenterVertically
            ) {
                GlassIconButton(
                    icon = Icons.Rounded.Replay10,
                    contentDescription = "Rewind 10 seconds",
                    onClick = {
                        val next = (positionMs - 10_000L).coerceAtLeast(0L)
                        positionMs = next
                        exoPlayer?.seekTo(next)
                    },
                    size = 54.dp,
                    testTag = "video_rewind_button"
                )
                GlassIconButton(
                    icon = if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                    contentDescription = if (isPlaying) "Pause" else "Play",
                    onClick = { isPlaying = !isPlaying },
                    variant = GlassVariant.TINTED,
                    size = 68.dp,
                    testTag = "video_play_pause_button"
                )
                GlassIconButton(
                    icon = Icons.Rounded.Forward10,
                    contentDescription = "Forward 10 seconds",
                    onClick = {
                        val next = (positionMs + 10_000L).coerceAtMost(durationMs)
                        positionMs = next
                        exoPlayer?.seekTo(next)
                    },
                    size = 54.dp,
                    testTag = "video_forward_button"
                )
            }
        }

        // Floating Liquid Glass Video Scrubber & Controls above the bottom bar
        AnimatedVisibility(
            visible = showChrome,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = 126.dp, start = Spacing.lg, end = Spacing.lg)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .liquidGlass(shape = RoundedCornerShape(CornerRadius.lg), cornerRadius = CornerRadius.lg, variant = GlassVariant.REGULAR)
                    .padding(horizontal = Spacing.md, vertical = Spacing.sm)
            ) {
                CompositionLocalProvider(LocalInsideGlassSurface provides true) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = MediaRepository.formatDuration(positionMs),
                        style = MaterialTheme.typography.labelSmall,
                        color = glassColors.textPrimary
                    )
                    GlassSlider(
                        value = if (durationMs > 0) positionMs.toFloat() / durationMs.toFloat() else 0f,
                        onValueChange = { frac ->
                            val target = (frac.toDouble() * durationMs).toLong()
                            positionMs = target
                            exoPlayer?.seekTo(target)
                        },
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = Spacing.sm),
                        testTag = "video_scrubber_slider"
                    )
                    Text(
                        text = MediaRepository.formatDuration(durationMs),
                        style = MaterialTheme.typography.labelSmall,
                        color = glassColors.textSecondary
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    GlassIconButton(
                        icon = if (isMuted) Icons.AutoMirrored.Rounded.VolumeOff else Icons.AutoMirrored.Rounded.VolumeUp,
                        contentDescription = if (isMuted) "Unmute" else "Mute",
                        onClick = { isMuted = !isMuted },
                        variant = GlassVariant.CLEAR,
                        size = 40.dp
                    )
                    GlassIconButton(
                        icon = Icons.Rounded.Repeat,
                        contentDescription = "Loop",
                        onClick = { isLooping = !isLooping },
                        selected = isLooping,
                        variant = GlassVariant.CLEAR,
                        size = 40.dp
                    )
                    GlassButton(
                        onClick = { showSpeedMenu = !showSpeedMenu },
                        variant = GlassVariant.CLEAR,
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Icon(Icons.Rounded.Speed, contentDescription = null, modifier = Modifier.size(15.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("${playbackSpeed}x", style = MaterialTheme.typography.labelMedium)
                    }
                    GlassIconButton(
                        icon = Icons.Rounded.Fullscreen,
                        contentDescription = "Fullscreen Rotation",
                        onClick = onToggleFullscreen,
                        variant = GlassVariant.CLEAR,
                        size = 40.dp
                    )
                    GlassIconButton(
                        icon = Icons.Rounded.PictureInPictureAlt,
                        contentDescription = "Picture in Picture",
                        onClick = onRequestPip,
                        variant = GlassVariant.CLEAR,
                        size = 40.dp
                    )
                }
                }
            }
        }

        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = 220.dp)
        ) {
            GlassPopoverMenu(
                expanded = showSpeedMenu,
                onDismissRequest = { showSpeedMenu = false },
                origin = TransformOrigin(0.5f, 1f),
                width = 160.dp
            ) {
                listOf(0.5f, 1.0f, 1.5f, 2.0f).forEach { spd ->
                    GlassMenuRow(
                        label = "${spd}x",
                        checked = playbackSpeed == spd,
                        onClick = {
                            playbackSpeed = spd
                            showSpeedMenu = false
                        }
                    )
                }
            }
        }
        }
    }
}

private suspend fun setMediaAsWallpaper(context: Context, item: MediaItem): String = withContext(Dispatchers.IO) {
    runCatching {
        val wm = WallpaperManager.getInstance(context)
        val bmp = if (item.sampleDrawableRes != null) {
            BitmapFactory.decodeResource(context.resources, item.sampleDrawableRes)
        } else {
            context.contentResolver.openInputStream(Uri.parse(item.uriString))?.use {
                BitmapFactory.decodeStream(it)
            }
        }
        if (bmp != null) {
            wm.setBitmap(bmp)
            "Wallpaper updated"
        } else {
            "Could not decode image for wallpaper"
        }
    }.getOrElse { "Wallpaper error: ${it.localizedMessage ?: "Permission denied"}" }
}

private fun printMediaItem(context: Context, item: MediaItem, onToast: (String) -> Unit) {
    runCatching {
        val printManager = context.getSystemService(Context.PRINT_SERVICE) as? PrintManager
            ?: throw IllegalStateException("Print service unavailable")
        val bmp = if (item.sampleDrawableRes != null) {
            BitmapFactory.decodeResource(context.resources, item.sampleDrawableRes)
        } else {
            context.contentResolver.openInputStream(Uri.parse(item.uriString))?.use {
                BitmapFactory.decodeStream(it)
            }
        } ?: throw IllegalStateException("Unable to decode bitmap")

        printManager.print(
            item.displayName,
            object : PrintDocumentAdapter() {
                private var pdfDoc: PrintedPdfDocument? = null

                override fun onLayout(
                    oldAttributes: PrintAttributes?,
                    newAttributes: PrintAttributes,
                    cancellationSignal: CancellationSignal?,
                    callback: LayoutResultCallback,
                    extras: Bundle?
                ) {
                    pdfDoc = PrintedPdfDocument(context, newAttributes)
                    if (cancellationSignal?.isCanceled == true) {
                        callback.onLayoutCancelled()
                        return
                    }
                    val info = PrintDocumentInfo.Builder(item.displayName)
                        .setContentType(PrintDocumentInfo.CONTENT_TYPE_PHOTO)
                        .setPageCount(1)
                        .build()
                    callback.onLayoutFinished(info, true)
                }

                override fun onWrite(
                    pages: Array<out PageRange>,
                    destination: ParcelFileDescriptor,
                    cancellationSignal: CancellationSignal?,
                    callback: WriteResultCallback
                ) {
                    val doc = pdfDoc ?: return
                    val page = doc.startPage(0)
                    val canvas = page.canvas
                    val scale = minOf(
                        canvas.width.toFloat() / bmp.width.coerceAtLeast(1),
                        canvas.height.toFloat() / bmp.height.coerceAtLeast(1)
                    )
                    val dx = (canvas.width - bmp.width * scale) * 0.5f
                    val dy = (canvas.height - bmp.height * scale) * 0.5f
                    val matrix = android.graphics.Matrix().apply {
                        setScale(scale, scale)
                        postTranslate(dx, dy)
                    }
                    canvas.drawBitmap(bmp, matrix, null)
                    doc.finishPage(page)
                    java.io.FileOutputStream(destination.fileDescriptor).use { out ->
                        doc.writeTo(out)
                    }
                    doc.close()
                    callback.onWriteFinished(arrayOf(PageRange.ALL_PAGES))
                }
            },
            null
        )
    }.onFailure {
        onToast("Print preview unavailable on this device")
    }
}
