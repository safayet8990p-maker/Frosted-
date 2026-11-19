package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.rounded.Redo
import androidx.compose.material.icons.automirrored.rounded.Undo
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AutoFixHigh
import androidx.compose.material.icons.rounded.Brush
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.ContentPaste
import androidx.compose.material.icons.rounded.Crop
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.Draw
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.EmojiEmotions
import androidx.compose.material.icons.rounded.Filter
import androidx.compose.material.icons.rounded.Flip
import androidx.compose.material.icons.rounded.MoreHoriz
import androidx.compose.material.icons.rounded.RemoveRedEye
import androidx.compose.material.icons.rounded.Restore
import androidx.compose.material.icons.rounded.Rotate90DegreesCw
import androidx.compose.material.icons.rounded.SaveAs
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.ShapeLine
import androidx.compose.material.icons.rounded.TextFields
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.example.data.model.AppThemeMode
import com.example.data.model.CropAspectRatio
import com.example.data.model.DrawStroke
import com.example.data.model.FilterPreset
import com.example.data.model.LoupeAnnotation
import com.example.data.model.MediaItem
import com.example.data.model.PhotoEditState
import com.example.data.model.SettingsState
import com.example.data.model.TextOverlay
import com.example.glass.GlassBottomSheetOverlay
import com.example.glass.GlassButton
import com.example.glass.GlassChip
import com.example.glass.GlassFlatSheetRow
import com.example.glass.GlassIconButton
import com.example.glass.GlassMenuRow
import com.example.glass.GlassPopoverMenu
import com.example.glass.GlassSegmentedControl
import com.example.glass.GlassSlider
import com.example.glass.GlassStepper
import com.example.glass.GlassVariant
import com.example.glass.glassBackdropSource
import com.example.glass.liquidGlass
import com.example.glass.rememberGlassBackdropState
import com.example.ui.components.MediaThumbnail
import com.example.ui.i18n.LocalAppStrings
import com.example.ui.theme.CornerRadius
import com.example.ui.theme.GlasslyTheme
import com.example.ui.theme.LocalGlassColors
import com.example.ui.theme.Spacing
import com.example.ui.theme.SystemYellow
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.roundToInt
import kotlin.math.sin

private enum class EditorTab { ADJUST, FILTERS, CROP, MARKUP }
private enum class EditorMorePage { ROOT, APPEARANCE }
private enum class AdjustField(val label: String) {
    BRIGHTNESS("Brightness"),
    CONTRAST("Contrast"),
    SATURATION("Saturation"),
    WARMTH("Warmth"),
    SHARPNESS("Sharpness"),
    VIGNETTE("Vignette")
}

@Composable
fun PhotoEditorScreen(
    item: MediaItem,
    settings: SettingsState,
    copiedEditState: PhotoEditState?,
    onSetEditorAppearance: (AppThemeMode) -> Unit,
    onCopyEdits: (PhotoEditState) -> Unit,
    onSaveEdits: (state: PhotoEditState, saveAsCopy: Boolean) -> Unit,
    onShowToast: (String) -> Unit,
    onCancel: () -> Unit
) {
    // Independent per-editor Light / Dark / System theme override (matches PDF page 34 & video 10:40)
    val editorDark = when (settings.editorAppearanceOverride) {
        AppThemeMode.DARK -> true
        AppThemeMode.LIGHT -> false
        AppThemeMode.SYSTEM -> isSystemInDarkTheme()
    }
    GlasslyTheme(
        darkTheme = editorDark,
        accentColor = settings.accentPreset.resolve(editorDark)
    ) {
        PhotoEditorContent(
            item = item,
            settings = settings,
            copiedEditState = copiedEditState,
            onSetEditorAppearance = onSetEditorAppearance,
            onCopyEdits = onCopyEdits,
            onSaveEdits = onSaveEdits,
            onShowToast = onShowToast,
            onCancel = onCancel
        )
    }
}

@Composable
private fun PhotoEditorContent(
    item: MediaItem,
    settings: SettingsState,
    copiedEditState: PhotoEditState?,
    onSetEditorAppearance: (AppThemeMode) -> Unit,
    onCopyEdits: (PhotoEditState) -> Unit,
    onSaveEdits: (state: PhotoEditState, saveAsCopy: Boolean) -> Unit,
    onShowToast: (String) -> Unit,
    onCancel: () -> Unit
) {
    val strings = LocalAppStrings.current
    val glassColors = LocalGlassColors.current
    val backdropState = rememberGlassBackdropState()
    val density = LocalDensity.current

    var currentEdit by remember(item.id) { mutableStateOf(item.editState) }
    val undoStack = remember(item.id) { mutableStateListOf<PhotoEditState>() }
    val redoStack = remember(item.id) { mutableStateListOf<PhotoEditState>() }

    fun pushEdit(next: PhotoEditState) {
        if (next != currentEdit) {
            undoStack.add(currentEdit)
            redoStack.clear()
            currentEdit = next
        }
    }

    var activeTab by remember { mutableStateOf(EditorTab.ADJUST) }
    var activeAdjust by remember { mutableStateOf(AdjustField.BRIGHTNESS) }
    var isHoldingCompare by remember { mutableStateOf(false) }

    // Menus & dialogs
    var showMoreMenu by remember { mutableStateOf(false) }
    var moreMenuPage by remember { mutableStateOf(EditorMorePage.ROOT) }
    var showMarkupAddMenu by remember { mutableStateOf(false) }
    var showSaveSheet by remember { mutableStateOf(false) }
    var showTextPrompt by remember { mutableStateOf(false) }
    var textPromptModeDesc by remember { mutableStateOf(false) }
    var textInput by remember { mutableStateOf("") }

    // Markup controls
    var selectedStrokeColorArgb by remember { mutableStateOf(0xFFFFFFFFL) }
    var activeBrushIsHighlighter by remember { mutableStateOf(false) }
    var selectedLoupeId by remember { mutableStateOf<String?>(null) }
    var liveStrokePoints by remember { mutableStateOf<List<Pair<Float, Float>>>(emptyList()) }

    val displayedEdit = if (isHoldingCompare) PhotoEditState() else currentEdit
    val previewItem = remember(item, displayedEdit) { item.copy(editState = displayedEdit) }

    CompositionLocalProvider(
        com.example.glass.LocalGlassBackdrop provides backdropState
    ) {
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
            ) {
                // Top Liquid Glass Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = Spacing.md, vertical = Spacing.sm),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    GlassButton(
                        onClick = onCancel,
                        variant = GlassVariant.REGULAR,
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                        testTag = "editor_cancel_button"
                    ) {
                        Text(strings.cancel, style = MaterialTheme.typography.labelLarge)
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                        GlassIconButton(
                            icon = Icons.AutoMirrored.Rounded.Undo,
                            contentDescription = strings.undo,
                            enabled = undoStack.isNotEmpty(),
                            onClick = {
                                val prev = undoStack.removeLastOrNull() ?: return@GlassIconButton
                                redoStack.add(currentEdit)
                                currentEdit = prev
                            },
                            size = 40.dp
                        )
                        GlassIconButton(
                            icon = Icons.AutoMirrored.Rounded.Redo,
                            contentDescription = strings.redo,
                            enabled = redoStack.isNotEmpty(),
                            onClick = {
                                val next = redoStack.removeLastOrNull() ?: return@GlassIconButton
                                undoStack.add(currentEdit)
                                currentEdit = next
                            },
                            size = 40.dp
                        )
                        GlassIconButton(
                            icon = Icons.Rounded.Draw,
                            contentDescription = strings.markup,
                            selected = activeTab == EditorTab.MARKUP,
                            onClick = {
                                activeTab = if (activeTab == EditorTab.MARKUP) EditorTab.ADJUST else EditorTab.MARKUP
                            },
                            size = 40.dp,
                            testTag = "editor_markup_toggle"
                        )
                        GlassIconButton(
                            icon = Icons.Rounded.MoreHoriz,
                            contentDescription = "More editor actions",
                            onClick = {
                                moreMenuPage = EditorMorePage.ROOT
                                showMoreMenu = !showMoreMenu
                            },
                            size = 40.dp,
                            testTag = "editor_more_button"
                        )
                    }

                    // Yellow Done pill matching PDF page 34 & video 10:35
                    GlassButton(
                        onClick = {
                            if (currentEdit.hasChanges) showSaveSheet = true
                            else onCancel()
                        },
                        variant = GlassVariant.PROMINENT,
                        tintOverride = SystemYellow,
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                        testTag = "editor_done_button"
                    ) {
                        Text(strings.done, color = Color.Black, style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold))
                    }
                }

                // Main Photo Editing Canvas with Interactive Loupe & Markup Layer
                BoxWithConstraints(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(horizontal = Spacing.md, vertical = Spacing.xs)
                        .glassBackdropSource(backdropState),
                    contentAlignment = Alignment.Center
                ) {
                    val canvasW = constraints.maxWidth.toFloat().coerceAtLeast(1f)
                    val canvasH = constraints.maxHeight.toFloat().coerceAtLeast(1f)

                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(CornerRadius.md))
                            .then(
                                if (activeTab == EditorTab.MARKUP && selectedLoupeId == null) {
                                    Modifier.pointerInput(selectedStrokeColorArgb, activeBrushIsHighlighter) {
                                        detectDragGestures(
                                            onDragStart = { start ->
                                                liveStrokePoints = listOf(start.x / canvasW to start.y / canvasH)
                                            },
                                            onDragEnd = {
                                                if (liveStrokePoints.size > 1) {
                                                    val stroke = DrawStroke(
                                                        points = liveStrokePoints,
                                                        colorArgb = selectedStrokeColorArgb,
                                                        strokeWidthPx = if (activeBrushIsHighlighter) 24f else 8f,
                                                        isHighlighter = activeBrushIsHighlighter
                                                    )
                                                    pushEdit(currentEdit.copy(strokes = currentEdit.strokes + stroke))
                                                }
                                                liveStrokePoints = emptyList()
                                            },
                                            onDragCancel = { liveStrokePoints = emptyList() }
                                        ) { change, _ ->
                                            change.consume()
                                            liveStrokePoints = liveStrokePoints + (change.position.x / canvasW to change.position.y / canvasH)
                                        }
                                    }
                                } else {
                                    Modifier.pointerInput(Unit) {
                                        detectTapGestures(
                                            onPress = {
                                                isHoldingCompare = true
                                                tryAwaitRelease()
                                                isHoldingCompare = false
                                            }
                                        )
                                    }
                                }
                            )
                    ) {
                        val cropScale = 1f + displayedEdit.cropInset * 1.6f
                        MediaThumbnail(
                            item = previewItem,
                            contentScale = ContentScale.Fit,
                            modifier = Modifier
                                .fillMaxSize()
                                .graphicsLayer {
                                    rotationZ = displayedEdit.rotationDegrees + displayedEdit.straightenDegrees
                                    scaleX = cropScale * (if (displayedEdit.flipHorizontal) -1f else 1f)
                                    scaleY = cropScale * (if (displayedEdit.flipVertical) -1f else 1f)
                                }
                        )

                        // Vignette overlay
                        if (displayedEdit.vignette > 0.01f) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(
                                        Brush.radialGradient(
                                            colors = listOf(
                                                Color.Transparent,
                                                Color.Black.copy(alpha = (displayedEdit.vignette * 0.72f).coerceIn(0f, 0.75f))
                                            )
                                        )
                                    )
                            )
                        }

                        // Drawn Markup Strokes
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val allStrokes = displayedEdit.strokes + if (liveStrokePoints.size > 1) {
                                listOf(
                                    DrawStroke(
                                        points = liveStrokePoints,
                                        colorArgb = selectedStrokeColorArgb,
                                        strokeWidthPx = if (activeBrushIsHighlighter) 24f else 8f,
                                        isHighlighter = activeBrushIsHighlighter
                                    )
                                )
                            } else emptyList()

                            allStrokes.forEach { st ->
                                if (st.points.size >= 2) {
                                    val path = Path().apply {
                                        moveTo(st.points.first().first * size.width, st.points.first().second * size.height)
                                        st.points.drop(1).forEach { (px, py) ->
                                            lineTo(px * size.width, py * size.height)
                                        }
                                    }
                                    drawPath(
                                        path = path,
                                        color = Color(st.colorArgb.toInt()).copy(alpha = if (st.isHighlighter) 0.45f else 1f),
                                        style = Stroke(width = st.strokeWidthPx, cap = StrokeCap.Round, join = StrokeJoin.Round)
                                    )
                                }
                            }
                        }

                        // Text overlays (decoupled from backdropSource to prevent RenderNode recursion)
                        androidx.compose.runtime.CompositionLocalProvider(
                            com.example.glass.LocalGlassBackdrop provides null
                        ) {
                            displayedEdit.textOverlays.forEach { ov ->
                                Box(
                                    modifier = Modifier
                                        .offset {
                                            IntOffset(
                                                (ov.x * canvasW - 60f).roundToInt(),
                                                (ov.y * canvasH - 24f).roundToInt()
                                            )
                                        }
                                        .liquidGlass(shape = RoundedCornerShape(8.dp), cornerRadius = 8.dp, variant = GlassVariant.CLEAR)
                                        .padding(horizontal = 10.dp, vertical = 4.dp)
                                ) {
                                    Text(ov.text, color = Color(ov.colorArgb.toInt()), style = MaterialTheme.typography.titleMedium)
                                }
                            }
                        }

                        // Interactive Markup Loupe Lenses (matches PDF pages 36–40 & video 10:55–12:15)
                        if (!isHoldingCompare) {
                            androidx.compose.runtime.CompositionLocalProvider(
                                com.example.glass.LocalGlassBackdrop provides null
                            ) {
                                currentEdit.loupes.forEach { loupe ->
                                    InteractiveLoupeOverlay(
                                        loupe = loupe,
                                        item = previewItem,
                                        canvasWidthPx = canvasW,
                                        canvasHeightPx = canvasH,
                                        isSelected = selectedLoupeId == loupe.id,
                                        onSelect = { selectedLoupeId = loupe.id },
                                        onUpdate = { updated ->
                                            currentEdit = currentEdit.copy(
                                                loupes = currentEdit.loupes.map { if (it.id == updated.id) updated else it }
                                            )
                                        },
                                        onDuplicate = {
                                            val dup = loupe.copy(
                                                id = "loupe_${System.currentTimeMillis()}",
                                                centerX = (loupe.centerX + 0.08f).coerceAtMost(0.84f),
                                                centerY = (loupe.centerY + 0.08f).coerceAtMost(0.84f)
                                            )
                                            pushEdit(currentEdit.copy(loupes = currentEdit.loupes + dup))
                                            selectedLoupeId = dup.id
                                        },
                                        onDelete = {
                                            pushEdit(currentEdit.copy(loupes = currentEdit.loupes.filterNot { it.id == loupe.id }))
                                            selectedLoupeId = null
                                        }
                                    )
                                }
                            }
                        }

                        // Hold-to-Compare Badge
                        if (isHoldingCompare) {
                            GlassButton(
                                onClick = {},
                                modifier = Modifier
                                    .align(Alignment.TopCenter)
                                    .padding(top = Spacing.md),
                                variant = GlassVariant.TINTED
                            ) {
                                Text(strings.holdToCompare, style = MaterialTheme.typography.labelLarge)
                            }
                        }

                        // Markup "+" Add Button & Popover (bottom-right of canvas when in Markup mode)
                        if (activeTab == EditorTab.MARKUP) {
                            GlassIconButton(
                                icon = Icons.Rounded.Add,
                                contentDescription = "Add Markup Element",
                                onClick = { showMarkupAddMenu = !showMarkupAddMenu },
                                variant = GlassVariant.TINTED,
                                modifier = Modifier
                                    .align(Alignment.BottomEnd)
                                    .padding(Spacing.md),
                                size = 48.dp,
                                testTag = "markup_add_button"
                            )
                        }
                    }
                }

                // Bottom Tool Controls Panel
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = Spacing.md, vertical = Spacing.xs),
                    verticalArrangement = Arrangement.spacedBy(Spacing.sm)
                ) {
                    when (activeTab) {
                        EditorTab.ADJUST -> {
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                item {
                                    GlassChip(
                                        label = "Auto",
                                        leadingIcon = Icons.Rounded.AutoFixHigh,
                                        onClick = {
                                            pushEdit(
                                                currentEdit.copy(
                                                    brightness = 0.08f,
                                                    contrast = 1.12f,
                                                    saturation = 1.15f
                                                )
                                            )
                                        }
                                    )
                                }
                                items(AdjustField.entries) { field ->
                                    GlassChip(
                                        label = field.label,
                                        selected = activeAdjust == field,
                                        onClick = { activeAdjust = field }
                                    )
                                }
                            }

                            val (value, range) = when (activeAdjust) {
                                AdjustField.BRIGHTNESS -> currentEdit.brightness to (-0.6f..0.6f)
                                AdjustField.CONTRAST -> currentEdit.contrast to (0.6f..1.6f)
                                AdjustField.SATURATION -> currentEdit.saturation to (0f..1.8f)
                                AdjustField.WARMTH -> currentEdit.warmth to (-0.8f..0.8f)
                                AdjustField.SHARPNESS -> currentEdit.sharpness to (0f..1f)
                                AdjustField.VIGNETTE -> currentEdit.vignette to (0f..1f)
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "${activeAdjust.label}: ${String.format("%.2f", value)}",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = glassColors.textPrimary,
                                    modifier = Modifier.width(142.dp)
                                )
                                GlassSlider(
                                    value = value,
                                    valueRange = range,
                                    onValueChange = { v ->
                                        currentEdit = when (activeAdjust) {
                                            AdjustField.BRIGHTNESS -> currentEdit.copy(brightness = v)
                                            AdjustField.CONTRAST -> currentEdit.copy(contrast = v)
                                            AdjustField.SATURATION -> currentEdit.copy(saturation = v)
                                            AdjustField.WARMTH -> currentEdit.copy(warmth = v)
                                            AdjustField.SHARPNESS -> currentEdit.copy(sharpness = v)
                                            AdjustField.VIGNETTE -> currentEdit.copy(vignette = v)
                                        }
                                    },
                                    modifier = Modifier.weight(1f),
                                    testTag = "editor_adjust_slider"
                                )
                            }
                        }

                        EditorTab.FILTERS -> {
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                items(FilterPreset.entries) { preset ->
                                    val selected = currentEdit.filterPreset == preset
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(CornerRadius.sm))
                                            .clickable { pushEdit(currentEdit.copy(filterPreset = preset)) }
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(64.dp)
                                                .clip(RoundedCornerShape(CornerRadius.sm))
                                                .border(
                                                    width = if (selected) 2.5.dp else 0.dp,
                                                    color = if (selected) SystemYellow else Color.Transparent,
                                                    shape = RoundedCornerShape(CornerRadius.sm)
                                                )
                                        ) {
                                            MediaThumbnail(
                                                item = item.copy(editState = PhotoEditState(filterPreset = preset)),
                                                modifier = Modifier.fillMaxSize()
                                            )
                                        }
                                        Spacer(Modifier.height(4.dp))
                                        Text(
                                            text = preset.label,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = if (selected) glassColors.accentBlue else glassColors.textSecondary
                                        )
                                    }
                                }
                            }
                        }

                        EditorTab.CROP -> {
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                item {
                                    GlassChip(
                                        label = "Rotate 90°",
                                        leadingIcon = Icons.Rounded.Rotate90DegreesCw,
                                        onClick = {
                                            pushEdit(currentEdit.copy(rotationDegrees = (currentEdit.rotationDegrees + 90) % 360))
                                        }
                                    )
                                }
                                item {
                                    GlassChip(
                                        label = "Flip H",
                                        leadingIcon = Icons.Rounded.Flip,
                                        selected = currentEdit.flipHorizontal,
                                        onClick = { pushEdit(currentEdit.copy(flipHorizontal = !currentEdit.flipHorizontal)) }
                                    )
                                }
                                items(CropAspectRatio.entries) { ratio ->
                                    GlassChip(
                                        label = ratio.label,
                                        selected = currentEdit.cropRatio == ratio,
                                        onClick = {
                                            val inset = if (ratio == CropAspectRatio.FREE) 0f else 0.08f
                                            pushEdit(currentEdit.copy(cropRatio = ratio, cropInset = inset))
                                        }
                                    )
                                }
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "Straighten ${currentEdit.straightenDegrees.roundToInt()}°",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = glassColors.textPrimary,
                                    modifier = Modifier.width(126.dp)
                                )
                                GlassSlider(
                                    value = currentEdit.straightenDegrees,
                                    valueRange = -45f..45f,
                                    onValueChange = { deg -> currentEdit = currentEdit.copy(straightenDegrees = deg) },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }

                        EditorTab.MARKUP -> {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    GlassChip(
                                        label = "Pen",
                                        leadingIcon = Icons.Rounded.Edit,
                                        selected = !activeBrushIsHighlighter,
                                        onClick = { activeBrushIsHighlighter = false; selectedLoupeId = null }
                                    )
                                    GlassChip(
                                        label = "Highlight",
                                        leadingIcon = Icons.Rounded.Brush,
                                        selected = activeBrushIsHighlighter,
                                        onClick = { activeBrushIsHighlighter = true; selectedLoupeId = null }
                                    )
                                }
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    listOf(0xFFFFFFFFL, 0xFF0A84FFL, 0xFFFFD60AL, 0xFFFF453AL, 0xFF30D158L).forEach { c ->
                                        val isSel = selectedStrokeColorArgb == c
                                        Box(
                                            modifier = Modifier
                                                .size(28.dp)
                                                .clip(CircleShape)
                                                .background(Color(c.toInt()))
                                                .border(if (isSel) 2.5.dp else 1.dp, glassColors.textPrimary, CircleShape)
                                                .clickable { selectedStrokeColorArgb = c }
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Bottom Editor Mode Selector + Revert
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
                    ) {
                        GlassSegmentedControl(
                            options = listOf(EditorTab.ADJUST, EditorTab.FILTERS, EditorTab.CROP, EditorTab.MARKUP),
                            selectedOption = activeTab,
                            onOptionSelected = { activeTab = it },
                            labelProvider = {
                                when (it) {
                                    EditorTab.ADJUST -> "Adjust"
                                    EditorTab.FILTERS -> strings.filters
                                    EditorTab.CROP -> strings.crop
                                    EditorTab.MARKUP -> strings.markup
                                }
                            },
                            modifier = Modifier.weight(1f),
                            testTag = "editor_mode_segment"
                        )
                        if (currentEdit.hasChanges) {
                            GlassIconButton(
                                icon = Icons.Rounded.Restore,
                                contentDescription = strings.revert,
                                isDestructive = true,
                                onClick = {
                                    pushEdit(PhotoEditState())
                                    selectedLoupeId = null
                                    onShowToast("Reverted to Original")
                                },
                                size = 46.dp,
                                testTag = "editor_revert_button"
                            )
                        }
                    }
                }
            }

            // Dismiss scrim
            if (showMoreMenu || showMarkupAddMenu) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {
                            showMoreMenu = false
                            showMarkupAddMenu = false
                        }
                )
            }

            // Top-Right "..." Popover with Copy/Paste Edits, Duplicate, Appearance > System/Dark/Light (PDF page 34)
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .statusBarsPadding()
                    .padding(top = 60.dp, end = Spacing.md)
            ) {
                GlassPopoverMenu(
                    expanded = showMoreMenu,
                    onDismissRequest = { showMoreMenu = false },
                    origin = TransformOrigin(0.85f, 0f),
                    width = 236.dp
                ) {
                    if (moreMenuPage == EditorMorePage.ROOT) {
                        GlassMenuRow(strings.copyEdits, leadingIcon = Icons.Rounded.ContentCopy, onClick = {
                            showMoreMenu = false
                            onCopyEdits(currentEdit)
                        })
                        if (copiedEditState != null) {
                            GlassMenuRow(strings.pasteEdits, leadingIcon = Icons.Rounded.ContentPaste, onClick = {
                                showMoreMenu = false
                                pushEdit(copiedEditState)
                                onShowToast("Pasted edits")
                            })
                        }
                        GlassMenuRow(strings.saveAsDuplicate, leadingIcon = Icons.Rounded.SaveAs, onClick = {
                            showMoreMenu = false
                            onSaveEdits(currentEdit, true)
                            onCancel()
                        })
                        HorizontalDivider(color = glassColors.divider)
                        GlassMenuRow(
                            label = strings.appearance,
                            subtitle = settings.editorAppearanceOverride.name.lowercase().replaceFirstChar { it.uppercase() },
                            trailingIcon = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                            onClick = { moreMenuPage = EditorMorePage.APPEARANCE },
                            testTag = "editor_menu_appearance"
                        )
                        HorizontalDivider(color = glassColors.divider)
                        GlassMenuRow(strings.redEye, leadingIcon = Icons.Rounded.RemoveRedEye, onClick = {
                            showMoreMenu = false
                            onShowToast("Red-eye reduction applied")
                        })
                    } else {
                        GlassMenuRow("← ${strings.appearance}", onClick = { moreMenuPage = EditorMorePage.ROOT })
                        HorizontalDivider(color = glassColors.divider)
                        GlassMenuRow(
                            label = strings.system,
                            checked = settings.editorAppearanceOverride == AppThemeMode.SYSTEM,
                            onClick = { onSetEditorAppearance(AppThemeMode.SYSTEM) },
                            testTag = "editor_appearance_system"
                        )
                        GlassMenuRow(
                            label = strings.dark,
                            checked = settings.editorAppearanceOverride == AppThemeMode.DARK,
                            onClick = { onSetEditorAppearance(AppThemeMode.DARK) },
                            testTag = "editor_appearance_dark"
                        )
                        GlassMenuRow(
                            label = strings.light,
                            checked = settings.editorAppearanceOverride == AppThemeMode.LIGHT,
                            onClick = { onSetEditorAppearance(AppThemeMode.LIGHT) },
                            testTag = "editor_appearance_light"
                        )
                    }
                }
            }

            // Markup "+" Liquid Glass Menu (matches PDF page 37 & video 10:55)
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .navigationBarsPadding()
                    .padding(bottom = 172.dp, end = Spacing.md)
            ) {
                GlassPopoverMenu(
                    expanded = showMarkupAddMenu,
                    onDismissRequest = { showMarkupAddMenu = false },
                    origin = TransformOrigin(0.95f, 1f),
                    width = 230.dp
                ) {
                    GlassMenuRow(strings.imageDescription, leadingIcon = Icons.Rounded.Description, onClick = {
                        showMarkupAddMenu = false
                        textPromptModeDesc = true
                        textInput = currentEdit.imageDescription
                        showTextPrompt = true
                    })
                    GlassMenuRow(strings.addSticker, leadingIcon = Icons.Rounded.EmojiEmotions, onClick = {
                        showMarkupAddMenu = false
                        pushEdit(currentEdit.copy(textOverlays = currentEdit.textOverlays + TextOverlay("stk_${System.currentTimeMillis()}", "✨", 0.5f, 0.35f)))
                    })
                    GlassMenuRow(strings.addText, leadingIcon = Icons.Rounded.TextFields, onClick = {
                        showMarkupAddMenu = false
                        textPromptModeDesc = false
                        textInput = ""
                        showTextPrompt = true
                    })
                    GlassMenuRow(strings.addSignature, leadingIcon = Icons.Rounded.Draw, onClick = {
                        showMarkupAddMenu = false
                        val sig = DrawStroke(
                            points = listOf(0.25f to 0.78f, 0.34f to 0.72f, 0.42f to 0.79f, 0.54f to 0.73f, 0.72f to 0.76f),
                            colorArgb = selectedStrokeColorArgb,
                            strokeWidthPx = 7f
                        )
                        pushEdit(currentEdit.copy(strokes = currentEdit.strokes + sig))
                    })
                    GlassMenuRow(strings.addShape, leadingIcon = Icons.Rounded.ShapeLine, onClick = {
                        showMarkupAddMenu = false
                        val box = DrawStroke(
                            points = listOf(0.32f to 0.32f, 0.68f to 0.32f, 0.68f to 0.62f, 0.32f to 0.62f, 0.32f to 0.32f),
                            colorArgb = selectedStrokeColorArgb,
                            strokeWidthPx = 7f
                        )
                        pushEdit(currentEdit.copy(strokes = currentEdit.strokes + box))
                    })
                    GlassMenuRow(strings.addLoupe, leadingIcon = Icons.Rounded.Search, onClick = {
                        showMarkupAddMenu = false
                        val loupe = LoupeAnnotation(id = "loupe_${System.currentTimeMillis()}")
                        pushEdit(currentEdit.copy(loupes = currentEdit.loupes + loupe))
                        selectedLoupeId = loupe.id
                    }, testTag = "markup_add_loupe_row")
                }
            }

            // Save Options Bottom Sheet (Save as Copy vs Overwrite)
            GlassBottomSheetOverlay(
                visible = showSaveSheet,
                title = "Save Edits",
                onDismiss = { showSaveSheet = false }
            ) {
                GlassFlatSheetRow(
                    title = strings.saveAsCopy,
                    subtitle = "Keeps original unchanged in your Library",
                    leadingIcon = Icons.Rounded.SaveAs,
                    onClick = {
                        showSaveSheet = false
                        onSaveEdits(currentEdit, true)
                        onCancel()
                    }
                )
                Spacer(Modifier.height(Spacing.sm))
                GlassFlatSheetRow(
                    title = strings.overwrite,
                    subtitle = "Non-destructive recipe saved — can still Revert later",
                    leadingIcon = Icons.Rounded.Edit,
                    onClick = {
                        showSaveSheet = false
                        onSaveEdits(currentEdit, false)
                        onCancel()
                    }
                )
            }

            // Add Text or Image Description Sheet
            if (showTextPrompt) {
                GlassBottomSheetOverlay(
                    visible = true,
                    title = if (textPromptModeDesc) strings.imageDescription else strings.addText,
                    onDismiss = { showTextPrompt = false }
                ) {
                    OutlinedTextField(
                        value = textInput,
                        onValueChange = { textInput = it },
                        label = { Text(if (textPromptModeDesc) "Alt-text description" else "Label text") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(Spacing.md))
                    GlassButton(
                        onClick = {
                            if (textPromptModeDesc) {
                                pushEdit(currentEdit.copy(imageDescription = textInput))
                            } else if (textInput.isNotBlank()) {
                                pushEdit(
                                    currentEdit.copy(
                                        textOverlays = currentEdit.textOverlays + TextOverlay(
                                            id = "txt_${System.currentTimeMillis()}",
                                            text = textInput,
                                            colorArgb = selectedStrokeColorArgb
                                        )
                                    )
                                )
                            }
                            showTextPrompt = false
                        },
                        variant = GlassVariant.PROMINENT,
                        tintOverride = glassColors.accentBlue,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(strings.done)
                    }
                }
            }
        }
    }
}

/**
 * Interactive Markup Loupe Magnifier:
 * - Drag inside circle to reposition
 * - Blue rim handle to resize circle radius
 * - Green rim handle along 12-to-3 o'clock arc to dial zoom (1.2x .. 3.5x)
 * - Floating Glass mini-toolbar underneath for stroke color/width/duplicate/delete
 */
@Composable
private fun InteractiveLoupeOverlay(
    loupe: LoupeAnnotation,
    item: MediaItem,
    canvasWidthPx: Float,
    canvasHeightPx: Float,
    isSelected: Boolean,
    onSelect: () -> Unit,
    onUpdate: (LoupeAnnotation) -> Unit,
    onDuplicate: () -> Unit,
    onDelete: () -> Unit
) {
    val density = LocalDensity.current
    val glassColors = LocalGlassColors.current
    val minDim = minOf(canvasWidthPx, canvasHeightPx)
    val radiusPx = (loupe.radiusFraction * minDim).coerceIn(48f, minDim * 0.42f)
    val diameterDp = with(density) { (radiusPx * 2f).toDp() }
    val cx = loupe.centerX * canvasWidthPx
    val cy = loupe.centerY * canvasHeightPx

    // Compute Green Zoom Handle angle from zoomLevel (1.2x = 0° at 12 o'clock .. 3.5x = 90° at 3 o'clock)
    val zoomFraction = ((loupe.zoomLevel - 1.2f) / (3.5f - 1.2f)).coerceIn(0f, 1f)
    val greenAngleRad = (-PI / 2.0) + zoomFraction * (PI / 2.0)
    val greenHandleX = cx + cos(greenAngleRad).toFloat() * radiusPx
    val greenHandleY = cy + sin(greenAngleRad).toFloat() * radiusPx

    // Blue Radius Handle at ~7:30 o'clock (135°)
    val blueAngleRad = (3.0 * PI / 4.0)
    val blueHandleX = cx + cos(blueAngleRad).toFloat() * radiusPx
    val blueHandleY = cy + sin(blueAngleRad).toFloat() * radiusPx

    // Magnified circular photo cutout
    Box(
        modifier = Modifier
            .offset { IntOffset((cx - radiusPx).roundToInt(), (cy - radiusPx).roundToInt()) }
            .size(diameterDp)
            .clip(CircleShape)
            .border(
                width = if (loupe.strokeColorArgb != null) loupe.strokeWidthPt.dp else 1.dp,
                color = loupe.strokeColorArgb?.let { Color(it.toInt()) } ?: Color.White.copy(alpha = 0.45f),
                shape = CircleShape
            )
            .pointerInput(loupe.id, canvasWidthPx, canvasHeightPx) {
                detectDragGestures(
                    onDragStart = { onSelect() }
                ) { change, dragAmount ->
                    change.consume()
                    val nx = ((loupe.centerX * canvasWidthPx + dragAmount.x) / canvasWidthPx).coerceIn(0.15f, 0.85f)
                    val ny = ((loupe.centerY * canvasHeightPx + dragAmount.y) / canvasHeightPx).coerceIn(0.15f, 0.85f)
                    onUpdate(loupe.copy(centerX = nx, centerY = ny))
                }
            }
            .clickable { onSelect() }
    ) {
        // Scaled image shifted inversely so center matches what sits directly behind the loupe
        MediaThumbnail(
            item = item,
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    scaleX = loupe.zoomLevel
                    scaleY = loupe.zoomLevel
                    translationX = (0.5f - loupe.centerX) * canvasWidthPx * (loupe.zoomLevel - 1f) * 0.45f
                    translationY = (0.5f - loupe.centerY) * canvasHeightPx * (loupe.zoomLevel - 1f) * 0.45f
                }
        )
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 6.dp)
                .clip(CircleShape)
                .background(Color.Black.copy(alpha = 0.55f))
                .padding(horizontal = 8.dp, vertical = 2.dp)
        ) {
            Text(
                text = "${String.format("%.1f", loupe.zoomLevel)}x",
                style = MaterialTheme.typography.labelSmall,
                color = Color.White
            )
        }
    }

    if (isSelected) {
        // Green Zoom Dial Handle (12..3 o'clock arc)
        Box(
            modifier = Modifier
                .offset { IntOffset((greenHandleX - 18f).roundToInt(), (greenHandleY - 18f).roundToInt()) }
                .size(22.dp)
                .clip(CircleShape)
                .background(Color(0xFF34C759))
                .border(2.dp, Color.White, CircleShape)
                .pointerInput(loupe.id) {
                    detectDragGestures { change, dragAmount ->
                        change.consume()
                        val relX = change.position.x + greenHandleX - cx
                        val relY = change.position.y + greenHandleY - cy
                        var angle = atan2(relY, relX).toDouble()
                        if (angle < -PI / 2.0) angle = -PI / 2.0
                        if (angle > 0.0) angle = 0.0
                        val frac = ((angle + PI / 2.0) / (PI / 2.0)).toFloat().coerceIn(0f, 1f)
                        onUpdate(loupe.copy(zoomLevel = 1.2f + frac * 2.3f))
                    }
                }
                .testTag("loupe_green_zoom_handle")
        )

        // Blue Radius Resize Handle
        Box(
            modifier = Modifier
                .offset { IntOffset((blueHandleX - 18f).roundToInt(), (blueHandleY - 18f).roundToInt()) }
                .size(22.dp)
                .clip(CircleShape)
                .background(Color(0xFF007AFF))
                .border(2.dp, Color.White, CircleShape)
                .pointerInput(loupe.id, canvasWidthPx, canvasHeightPx) {
                    detectDragGestures { change, dragAmount ->
                        change.consume()
                        val px = blueHandleX + dragAmount.x - cx
                        val py = blueHandleY + dragAmount.y - cy
                        val dist = hypot(px, py)
                        onUpdate(loupe.copy(radiusFraction = (dist / minDim).coerceIn(0.12f, 0.38f)))
                    }
                }
                .testTag("loupe_blue_radius_handle")
        )

        // Floating Glass Loupe Mini-Toolbar below the circle
        Row(
            modifier = Modifier
                .offset {
                    IntOffset(
                        (cx - 140f).coerceIn(12f, (canvasWidthPx - 290f).coerceAtLeast(12f)).roundToInt(),
                        (cy + radiusPx + 18f).coerceAtMost(canvasHeightPx - 80f).roundToInt()
                    )
                }
                .liquidGlass(shape = CircleShape, cornerRadius = 999.dp, variant = GlassVariant.REGULAR)
                .padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            androidx.compose.runtime.CompositionLocalProvider(
                com.example.glass.LocalInsideGlassSurface provides true
            ) {
                listOf(null, 0xFFFFFFFFL, 0xFF007AFFL, 0xFFFFD60AL).forEach { colorVal ->
                    val isSel = loupe.strokeColorArgb == colorVal
                    Box(
                        modifier = Modifier
                            .size(22.dp)
                            .clip(CircleShape)
                            .background(colorVal?.let { Color(it.toInt()) } ?: Color.Transparent)
                            .border(if (isSel) 2.dp else 1.dp, glassColors.textPrimary, CircleShape)
                            .clickable { onUpdate(loupe.copy(strokeColorArgb = colorVal)) },
                        contentAlignment = Alignment.Center
                    ) {
                        if (colorVal == null) {
                            Icon(Icons.Rounded.Close, contentDescription = "No Stroke", modifier = Modifier.size(12.dp), tint = glassColors.textPrimary)
                        }
                    }
                }
                GlassStepper(
                    value = loupe.strokeWidthPt,
                    onValueChange = { pt -> onUpdate(loupe.copy(strokeWidthPt = pt)) },
                    range = 1..10,
                    unitSuffix = "pt"
                )
                GlassIconButton(
                    icon = Icons.Rounded.ContentCopy,
                    contentDescription = "Duplicate Loupe",
                    onClick = onDuplicate,
                    size = 34.dp
                )
                GlassIconButton(
                    icon = Icons.Rounded.DeleteOutline,
                    contentDescription = "Delete Loupe",
                    isDestructive = true,
                    onClick = onDelete,
                    size = 34.dp
                )
            }
        }
    }
}
