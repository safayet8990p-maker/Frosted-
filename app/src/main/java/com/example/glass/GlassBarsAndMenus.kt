package com.example.glass

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Collections
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.material.icons.rounded.IosShare
import androidx.compose.material.icons.rounded.MoreHoriz
import androidx.compose.material.icons.rounded.PhotoLibrary
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.data.model.TimeSegment
import com.example.ui.i18n.LocalAppStrings
import com.example.ui.theme.CornerRadius
import com.example.ui.theme.ElevationRules
import com.example.ui.theme.LocalGlassColors
import com.example.ui.theme.MotionTokens
import com.example.ui.theme.Spacing

enum class MainNavTab {
    LIBRARY,
    COLLECTIONS
}

@Composable
fun GlassTopToolbar(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    onBackClick: (() -> Unit)? = null,
    onTitleClick: (() -> Unit)? = null,
    trailingContent: @Composable RowScope.() -> Unit = {}
) {
    val glassColors = LocalGlassColors.current
    Row(
        modifier = modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = Spacing.lg, vertical = Spacing.sm),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (onBackClick != null) {
            GlassIconButton(
                icon = Icons.AutoMirrored.Rounded.ArrowBack,
                contentDescription = "Back",
                onClick = onBackClick,
                size = 44.dp,
                testTag = "toolbar_back_button"
            )
            Spacer(Modifier.width(Spacing.sm))
        }
        Column(
            modifier = Modifier
                .weight(1f)
                .let {
                    if (onTitleClick != null) {
                        it.clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = onTitleClick
                        )
                    } else it
                }
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.headlineLarge,
                color = glassColors.textPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.labelMedium,
                    color = glassColors.textSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        Row(
            horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
            verticalAlignment = Alignment.CenterVertically,
            content = trailingContent
        )
    }
}

/**
 * Exact iOS 26 Split Bottom Bar:
 * - Default: Left 2-tab Liquid Glass capsule [Library | Collections] + Right circular (Search) pill
 * - Library Time Segment mode (or scrolled): [Grid Icon] [Years | Months | All] + (Search)
 * - Selection Mode: [Share] [X Photos Selected] [Delete]
 * - Shrinks smoothly on downward scroll and expands on scroll up or tap.
 */
@Composable
fun GlassSplitBottomBar(
    activeTab: MainNavTab,
    isCompactOnScroll: Boolean,
    showTimeSegmentsInBar: Boolean,
    activeTimeSegment: TimeSegment,
    isSelectionMode: Boolean,
    selectedCount: Int,
    onTabSelected: (MainNavTab) -> Unit,
    onExpandBarTap: () -> Unit,
    onToggleTimeSegmentsBar: () -> Unit,
    onTimeSegmentSelected: (TimeSegment) -> Unit,
    onSearchClick: () -> Unit,
    onShareSelected: () -> Unit,
    onDeleteSelected: () -> Unit,
    modifier: Modifier = Modifier
) {
    val strings = LocalAppStrings.current
    val glassColors = LocalGlassColors.current
    val config = LocalGlassConfig.current

    val barScale by animateFloatAsState(
        targetValue = if (isCompactOnScroll && !isSelectionMode) 0.90f else 1f,
        animationSpec = if (config.reduceMotion) tween(0) else spring(
            dampingRatio = 0.78f,
            stiffness = config.springStiffness
        ),
        label = "floatingTabBarScale"
    )
    val barHeight by animateDpAsState(
        targetValue = if (isCompactOnScroll && !isSelectionMode) 50.dp else 62.dp,
        animationSpec = if (config.reduceMotion) tween(0) else spring(
            dampingRatio = 0.8f,
            stiffness = config.springStiffness
        ),
        label = "floatingTabBarHeight"
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = Spacing.lg, vertical = Spacing.sm),
        contentAlignment = Alignment.BottomCenter
    ) {
        AnimatedContent(
            targetState = isSelectionMode,
            transitionSpec = {
                (fadeIn(tween(180)) + slideInVertically { it / 3 }) togetherWith
                    (fadeOut(tween(140)) + slideOutVertically { it / 3 })
            },
            label = "bottomBarMode"
        ) { selecting ->
            if (selecting) {
                // Multi-select floating glass action bar (matches PDF page 48 & video 3:38)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(58.dp)
                        .glassElevationShadow(ElevationRules.floatingBarShadow, CircleShape)
                        .liquidGlass(shape = CircleShape, cornerRadius = 999.dp, variant = GlassVariant.REGULAR)
                        .padding(horizontal = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    CompositionLocalProvider(LocalInsideGlassSurface provides true) {
                        GlassIconButton(
                            icon = Icons.Rounded.IosShare,
                            contentDescription = strings.share,
                            onClick = onShareSelected,
                            enabled = selectedCount > 0,
                            variant = GlassVariant.CLEAR,
                            size = 44.dp,
                            testTag = "selection_share_button"
                        )
                        Text(
                            text = if (selectedCount == 0) "Select Items" else "$selectedCount Photos Selected",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                            color = glassColors.textPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(horizontal = 8.dp)
                        )
                        GlassIconButton(
                            icon = Icons.Rounded.DeleteOutline,
                            contentDescription = strings.delete,
                            onClick = onDeleteSelected,
                            enabled = selectedCount > 0,
                            isDestructive = true,
                            variant = GlassVariant.CLEAR,
                            size = 44.dp,
                            testTag = "selection_delete_button"
                        )
                    }
                }
            } else {
                GlassContainer(
                    modifier = Modifier
                        .fillMaxWidth()
                        .graphicsLayer {
                            scaleX = barScale
                            scaleY = barScale
                        },
                    morphSpacing = 14.dp
                ) { containerState ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        if (showTimeSegmentsInBar && activeTab == MainNavTab.LIBRARY) {
                            // Morphed time-segment pill [Grid] + [Years | Months | All]
                            Row(
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(end = Spacing.sm),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
                            ) {
                                GlassIconButton(
                                    icon = Icons.Rounded.GridView,
                                    contentDescription = strings.tabLibrary,
                                    onClick = onToggleTimeSegmentsBar,
                                    size = barHeight,
                                    testTag = "bottom_bar_grid_toggle"
                                )
                                GlassSegmentedControl(
                                    options = listOf(TimeSegment.YEARS, TimeSegment.MONTHS, TimeSegment.ALL),
                                    selectedOption = activeTimeSegment,
                                    onOptionSelected = onTimeSegmentSelected,
                                    labelProvider = { seg ->
                                        when (seg) {
                                            TimeSegment.YEARS -> strings.years
                                            TimeSegment.MONTHS -> strings.months
                                            TimeSegment.DAYS -> strings.days
                                            TimeSegment.ALL -> strings.all
                                        }
                                    },
                                    modifier = Modifier
                                        .weight(1f)
                                        .glassContainerChild(containerState, "main_pill"),
                                    testTag = "library_time_segment_pill"
                                )
                            }
                        } else {
                            // Exact 2-tab Liquid Glass capsule on bottom-left (Library + Collections)
                            Row(
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(end = Spacing.md)
                                    .height(barHeight)
                                    .glassContainerChild(containerState, "main_pill")
                                    .glassElevationShadow(ElevationRules.floatingBarShadow, CircleShape)
                                    .liquidGlass(shape = CircleShape, cornerRadius = 999.dp, variant = GlassVariant.REGULAR)
                                    .clickable(
                                        interactionSource = remember { MutableInteractionSource() },
                                        indication = null,
                                        enabled = isCompactOnScroll
                                    ) { onExpandBarTap() }
                                    .padding(4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                TabCapsuleItem(
                                    icon = Icons.Rounded.PhotoLibrary,
                                    label = strings.tabLibrary,
                                    selected = activeTab == MainNavTab.LIBRARY,
                                    compact = isCompactOnScroll,
                                    onClick = {
                                        if (isCompactOnScroll) onExpandBarTap()
                                        onTabSelected(MainNavTab.LIBRARY)
                                    },
                                    modifier = Modifier.weight(1f),
                                    testTag = "tab_library"
                                )
                                TabCapsuleItem(
                                    icon = Icons.Rounded.Collections,
                                    label = strings.tabCollections,
                                    selected = activeTab == MainNavTab.COLLECTIONS,
                                    compact = isCompactOnScroll,
                                    onClick = {
                                        if (isCompactOnScroll) onExpandBarTap()
                                        onTabSelected(MainNavTab.COLLECTIONS)
                                    },
                                    modifier = Modifier.weight(1f),
                                    testTag = "tab_collections"
                                )
                            }
                        }

                        // Separate circular Search button on bottom-right (PDF page 42, 43)
                        GlassIconButton(
                            icon = Icons.Rounded.Search,
                            contentDescription = strings.tabSearch,
                            onClick = onSearchClick,
                            size = barHeight,
                            modifier = Modifier.glassContainerChild(containerState, "search_circle"),
                            testTag = "tab_search_circle"
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TabCapsuleItem(
    icon: ImageVector,
    label: String,
    selected: Boolean,
    compact: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    testTag: String
) {
    val glassColors = LocalGlassColors.current
    val config = LocalGlassConfig.current
    val interactionSource = remember { MutableInteractionSource() }
    val pressState = rememberGlassPressState(interactionSource)

    val itemTint = if (selected) {
        if (config.accentPreset.key == "frosted") {
            if (glassColors.isDark) Color(0x30FFFFFF) else Color(0xFFEAECEF)
        } else {
            config.accentPreset.resolve(glassColors.isDark).copy(alpha = if (glassColors.isDark) 0.24f else 0.14f)
        }
    } else Color.Transparent

    val fgColor = if (selected) {
        if (config.accentPreset.key == "frosted") glassColors.accentBlue
        else config.accentPreset.resolve(glassColors.isDark)
    } else glassColors.textPrimary

    Column(
        modifier = modifier
            .testTag(testTag)
            .fillMaxSize()
            .graphicsLayer {
                scaleX = pressState.scaleX
                scaleY = pressState.scaleY
            }
            .clip(CircleShape)
            .background(itemTint)
            .semantics {
                role = Role.Tab
                this.selected = selected
                contentDescription = label
            }
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            ),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = fgColor,
            modifier = Modifier.size(if (compact) 20.dp else 22.dp)
        )
        AnimatedVisibility(visible = !compact) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium
                ),
                color = fgColor,
                maxLines = 1
            )
        }
    }
}

/**
 * In-place Liquid Glass Popover Menu that expands from its trigger edge with increased thickness & shadow.
 */
@Composable
fun GlassPopoverMenu(
    expanded: Boolean,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    origin: TransformOrigin = TransformOrigin(1f, 0f),
    width: Dp = 260.dp,
    content: @Composable ColumnScope.() -> Unit
) {
    val config = LocalGlassConfig.current
    AnimatedVisibility(
        visible = expanded,
        enter = if (config.reduceMotion) fadeIn(tween(0)) else (
            scaleIn(
                initialScale = 0.35f,
                transformOrigin = origin,
                animationSpec = spring(dampingRatio = 0.76f, stiffness = Spring.StiffnessMedium)
            ) + fadeIn(tween(MotionTokens.FastFadeMs))
        ),
        exit = if (config.reduceMotion) fadeOut(tween(0)) else (
            scaleOut(
                targetScale = 0.35f,
                transformOrigin = origin,
                animationSpec = tween(150)
            ) + fadeOut(tween(130))
        ),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .width(width)
                .glassElevationShadow(ElevationRules.expandedMenuShadow, RoundedCornerShape(CornerRadius.lg))
                .liquidGlass(
                    shape = RoundedCornerShape(CornerRadius.lg),
                    cornerRadius = CornerRadius.lg,
                    variant = GlassVariant.REGULAR,
                    thicknessBoost = 0.45f
                )
                .padding(vertical = 8.dp)
        ) {
            CompositionLocalProvider(LocalInsideGlassSurface provides true) {
                content()
            }
        }
    }
}

@Composable
fun GlassMenuRow(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    leadingIcon: ImageVector? = null,
    trailingIcon: ImageVector? = null,
    checked: Boolean? = null,
    isDestructive: Boolean = false,
    subtitle: String? = null,
    testTag: String? = null
) {
    val glassColors = LocalGlassColors.current
    val fgColor = if (isDestructive) glassColors.destructive else glassColors.textPrimary
    Row(
        modifier = modifier
            .let { if (testTag != null) it.testTag(testTag) else it }
            .fillMaxWidth()
            .defaultMinSize(minHeight = 46.dp)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (checked != null) {
            Box(modifier = Modifier.width(22.dp), contentAlignment = Alignment.CenterStart) {
                if (checked) {
                    Text(
                        text = "✓",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = glassColors.accentBlue
                    )
                }
            }
        }
        if (leadingIcon != null) {
            Icon(
                imageVector = leadingIcon,
                contentDescription = null,
                tint = fgColor,
                modifier = Modifier.size(19.dp)
            )
            Spacer(Modifier.width(12.dp))
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
                color = fgColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = glassColors.textSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        if (trailingIcon != null) {
            Icon(
                imageVector = trailingIcon,
                contentDescription = null,
                tint = glassColors.textSecondary,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

@Composable
fun GlassAlertDialog(
    visible: Boolean,
    title: String,
    message: String,
    confirmLabel: String,
    cancelLabel: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    isDestructive: Boolean = false,
    origin: TransformOrigin = TransformOrigin(0.5f, 0.85f)
) {
    val glassColors = LocalGlassColors.current
    val config = LocalGlassConfig.current

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(tween(150)),
        exit = fadeOut(tween(150))
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.36f))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onDismiss
                ),
            contentAlignment = Alignment.Center
        ) {
            AnimatedVisibility(
                visible = visible,
                enter = if (config.reduceMotion) fadeIn(tween(0)) else (
                    scaleIn(
                        initialScale = 0.45f,
                        transformOrigin = origin,
                        animationSpec = spring(dampingRatio = 0.72f, stiffness = Spring.StiffnessMedium)
                    ) + fadeIn(tween(160))
                ),
                exit = scaleOut(targetScale = 0.45f, transformOrigin = origin) + fadeOut(tween(120))
            ) {
                Column(
                    modifier = Modifier
                        .padding(horizontal = Spacing.xl)
                        .widthIn(max = 360.dp)
                        .glassElevationShadow(ElevationRules.dialogShadow, RoundedCornerShape(CornerRadius.xl))
                        .liquidGlass(
                            shape = RoundedCornerShape(CornerRadius.xl),
                            cornerRadius = CornerRadius.xl,
                            variant = GlassVariant.REGULAR,
                            thicknessBoost = 0.6f
                        )
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) {}
                        .padding(Spacing.xl),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleLarge,
                        color = glassColors.textPrimary
                    )
                    Spacer(Modifier.height(Spacing.sm))
                    Text(
                        text = message,
                        style = MaterialTheme.typography.bodyMedium,
                        color = glassColors.textSecondary
                    )
                    Spacer(Modifier.height(Spacing.xl))
                    CompositionLocalProvider(LocalInsideGlassSurface provides true) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(Spacing.md)
                        ) {
                            GlassButton(
                                onClick = onDismiss,
                                modifier = Modifier.weight(1f),
                                variant = GlassVariant.CLEAR,
                                testTag = "alert_cancel_button"
                            ) {
                                Text(cancelLabel, style = MaterialTheme.typography.labelLarge)
                            }
                            GlassButton(
                                onClick = onConfirm,
                                modifier = Modifier.weight(1f),
                                variant = if (isDestructive) GlassVariant.REGULAR else GlassVariant.PROMINENT,
                                isDestructive = isDestructive,
                                testTag = "alert_confirm_button"
                            ) {
                                Text(confirmLabel, style = MaterialTheme.typography.labelLarge)
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Glass bottom sheet container. Inside the glass shell, rows use flat tonal fills (never glass inside glass).
 */
@Composable
fun GlassBottomSheetOverlay(
    visible: Boolean,
    title: String,
    onDismiss: () -> Unit,
    trailingHeaderAction: @Composable (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val glassColors = LocalGlassColors.current
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(tween(180)),
        exit = fadeOut(tween(150))
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.40f))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onDismiss
                ),
            contentAlignment = Alignment.BottomCenter
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 560.dp)
                    .navigationBarsPadding()
                    .padding(horizontal = Spacing.sm, vertical = Spacing.sm)
                    .glassElevationShadow(ElevationRules.dialogShadow, RoundedCornerShape(CornerRadius.xl))
                    .liquidGlass(
                        shape = RoundedCornerShape(CornerRadius.xl),
                        cornerRadius = CornerRadius.xl,
                        variant = GlassVariant.REGULAR,
                        thicknessBoost = 0.55f
                    )
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) {}
                    .padding(horizontal = Spacing.lg, vertical = Spacing.md)
            ) {
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .size(width = 40.dp, height = 4.dp)
                        .clip(CircleShape)
                        .background(glassColors.textTertiary.copy(alpha = 0.5f))
                )
                Spacer(Modifier.height(Spacing.md))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleLarge,
                        color = glassColors.textPrimary
                    )
                    trailingHeaderAction?.invoke()
                }
                Spacer(Modifier.height(Spacing.md))
                CompositionLocalProvider(LocalInsideGlassSurface provides true) {
                    content()
                }
            }
        }
    }
}

@Composable
fun GlassFlatSheetRow(
    title: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    leadingIcon: ImageVector? = null,
    trailingContent: @Composable (() -> Unit)? = null
) {
    val glassColors = LocalGlassColors.current
    Row(
        modifier = modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 52.dp)
            .clip(RoundedCornerShape(CornerRadius.sm))
            .background(glassColors.flatRowFill)
            .clickable(onClick = onClick)
            .padding(horizontal = Spacing.md, vertical = Spacing.sm),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (leadingIcon != null) {
            Icon(
                imageVector = leadingIcon,
                contentDescription = null,
                tint = glassColors.textPrimary,
                modifier = Modifier.size(20.dp)
            )
            Spacer(Modifier.width(Spacing.md))
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                color = glassColors.textPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = glassColors.textSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        trailingContent?.invoke()
    }
}

@Composable
fun GlassToastBanner(
    message: String?,
    modifier: Modifier = Modifier
) {
    val glassColors = LocalGlassColors.current
    AnimatedVisibility(
        visible = message != null,
        enter = slideInVertically { -it } + fadeIn(tween(180)),
        exit = slideOutVertically { -it } + fadeOut(tween(160)),
        modifier = modifier
            .statusBarsPadding()
            .padding(top = Spacing.sm, start = Spacing.lg, end = Spacing.lg)
    ) {
        Box(
            modifier = Modifier
                .glassElevationShadow(ElevationRules.floatingBarShadow, CircleShape)
                .liquidGlass(shape = CircleShape, cornerRadius = 999.dp, variant = GlassVariant.TINTED)
                .padding(horizontal = Spacing.lg, vertical = Spacing.sm)
        ) {
            Text(
                text = message.orEmpty(),
                style = MaterialTheme.typography.labelLarge,
                color = glassColors.textPrimary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun GlassPickerRow(
    options: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
    ) {
        options.forEachIndexed { index, label ->
            GlassChip(
                label = label,
                selected = index == selectedIndex,
                onClick = { onSelect(index) }
            )
        }
    }
}

@Composable
fun AuroraMeshBackdrop(modifier: Modifier = Modifier) {
    val glassColors = LocalGlassColors.current
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(glassColors.background)
    )
}
