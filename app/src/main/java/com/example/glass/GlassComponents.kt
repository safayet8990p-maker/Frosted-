package com.example.glass

import android.view.HapticFeedbackConstants
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.example.ui.theme.CornerRadius
import com.example.ui.theme.ElevationRules
import com.example.ui.theme.LocalGlassColors
import com.example.ui.theme.Spacing
import com.example.ui.theme.SystemBlue
import com.example.ui.theme.SystemBlueDark
import kotlin.math.roundToInt

@Composable
fun GlassButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    variant: GlassVariant = GlassVariant.REGULAR,
    enabled: Boolean = true,
    selected: Boolean = false,
    loading: Boolean = false,
    isCircle: Boolean = false,
    isDestructive: Boolean = false,
    tintOverride: Color? = null,
    contentDescription: String? = null,
    testTag: String? = null,
    contentPadding: PaddingValues = if (isCircle) PaddingValues(0.dp) else PaddingValues(horizontal = 18.dp, vertical = 12.dp),
    content: @Composable RowScope.() -> Unit
) {
    val config = LocalGlassConfig.current
    val glassColors = LocalGlassColors.current
    val interactionSource = remember { MutableInteractionSource() }
    val pressState = rememberGlassPressState(interactionSource, enabled && !loading)

    val cornerDp = if (isCircle) 999.dp else CornerRadius.pill
    val shape = if (isCircle) CircleShape else RoundedCornerShape(cornerDp)
    val minTouch = if (config.largerTouchTargets) 54.dp else Spacing.touchTarget

    val effectiveVariant = when {
        selected && variant == GlassVariant.REGULAR -> GlassVariant.TINTED
        else -> variant
    }
    val effectiveTint = when {
        isDestructive -> glassColors.destructive.copy(alpha = 0.24f)
        tintOverride != null -> tintOverride
        selected && variant == GlassVariant.PROMINENT -> glassColors.accentBlue.copy(alpha = 0.88f)
        else -> null
    }

    val contentColor = when {
        !enabled -> glassColors.textTertiary
        isDestructive -> glassColors.destructive
        effectiveVariant == GlassVariant.PROMINENT -> {
            if (effectiveTint != null && effectiveTint.luminance() > 0.55f) Color(0xFF0E1116)
            else if (config.accentPreset.key == "frosted" && !glassColors.isDark) Color.White
            else if (config.accentPreset.key == "frosted" && glassColors.isDark) Color(0xFF0E1116)
            else Color.White
        }
        else -> glassColors.textPrimary
    }

    val appliedModifier = modifier
        .let { if (testTag != null) it.testTag(testTag) else it }
        .defaultMinSize(minWidth = minTouch, minHeight = minTouch)
        .graphicsLayer {
            scaleX = pressState.scaleX
            scaleY = pressState.scaleY
        }
        .glassElevationShadow(
            elevation = if (pressState.isPressed) ElevationRules.floatingBarShadow else ElevationRules.restingShadow,
            shape = shape
        )
        .liquidGlass(
            shape = shape,
            cornerRadius = cornerDp,
            variant = effectiveVariant,
            tintOverride = effectiveTint,
            isPressed = pressState.isPressed,
            shimmerPhase = pressState.shimmerPhase
        )
        .alpha(if (enabled) 1f else 0.52f)
        .semantics {
            role = Role.Button
            this.selected = selected
            if (contentDescription != null) {
                this.contentDescription = contentDescription
            }
        }
        .clickable(
            interactionSource = interactionSource,
            indication = null,
            enabled = enabled && !loading,
            onClick = onClick
        )

    Box(
        modifier = appliedModifier,
        contentAlignment = Alignment.Center
    ) {
        CompositionLocalProvider(LocalContentColor provides contentColor) {
            Row(
                modifier = Modifier.padding(contentPadding),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                if (loading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                        color = contentColor
                    )
                } else {
                    content()
                }
            }
        }
    }
}

@Composable
fun GlassIconButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    variant: GlassVariant = GlassVariant.REGULAR,
    selected: Boolean = false,
    enabled: Boolean = true,
    isDestructive: Boolean = false,
    tintOverride: Color? = null,
    iconTint: Color? = null,
    size: Dp = 48.dp,
    testTag: String? = null
) {
    GlassButton(
        onClick = onClick,
        modifier = modifier.size(size),
        variant = variant,
        enabled = enabled,
        selected = selected,
        isCircle = true,
        isDestructive = isDestructive,
        tintOverride = tintOverride,
        contentDescription = contentDescription,
        testTag = testTag
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = iconTint ?: LocalContentColor.current,
            modifier = Modifier.size(21.dp)
        )
    }
}

@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = CornerRadius.lg,
    variant: GlassVariant = GlassVariant.REGULAR,
    contentPadding: PaddingValues = PaddingValues(Spacing.lg),
    onClick: (() -> Unit)? = null,
    testTag: String? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val shape = RoundedCornerShape(cornerRadius)
    val interactionSource = remember { MutableInteractionSource() }
    val pressState = rememberGlassPressState(interactionSource, enabled = onClick != null)

    val cardModifier = modifier
        .let { if (testTag != null) it.testTag(testTag) else it }
        .graphicsLayer {
            if (onClick != null) {
                scaleX = pressState.scaleX
                scaleY = pressState.scaleY
            }
        }
        .glassElevationShadow(ElevationRules.restingShadow, shape)
        .liquidGlass(
            shape = shape,
            cornerRadius = cornerRadius,
            variant = variant,
            isPressed = pressState.isPressed,
            shimmerPhase = pressState.shimmerPhase
        )
        .let {
            if (onClick != null) {
                it.clickable(
                    interactionSource = interactionSource,
                    indication = null,
                    onClick = onClick
                )
            } else it
        }

    Column(
        modifier = cardModifier.padding(contentPadding)
    ) {
        CompositionLocalProvider(LocalInsideGlassSurface provides true) {
            content()
        }
    }
}

@Composable
fun GlassSwitch(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    contentDescription: String = "Toggle",
    testTag: String? = null
) {
    val config = LocalGlassConfig.current
    val glassColors = LocalGlassColors.current
    val view = LocalView.current
    val interactionSource = remember { MutableInteractionSource() }
    val pressState = rememberGlassPressState(interactionSource, enabled)

    val trackWidth = 54.dp
    val trackHeight = 32.dp
    val thumbSize = 26.dp

    val thumbOffset by animateDpAsState(
        targetValue = if (checked) trackWidth - thumbSize - 3.dp else 3.dp,
        animationSpec = if (config.reduceMotion) tween(0) else spring(
            dampingRatio = 0.65f,
            stiffness = config.springStiffness
        ),
        label = "glassSwitchThumb"
    )
    val thumbWidthStretch by animateDpAsState(
        targetValue = if (pressState.isPressed && !config.reduceMotion) 32.dp else thumbSize,
        animationSpec = tween(140),
        label = "glassSwitchStretch"
    )

    val trackTint = if (checked) {
        if (config.accentPreset.key == "frosted") glassColors.accentBlue.copy(alpha = 0.86f)
        else config.accentPreset.resolve(glassColors.isDark).copy(alpha = 0.88f)
    } else {
        if (glassColors.isDark) Color(0x55484A52) else Color(0x33787880)
    }

    Box(
        modifier = modifier
            .let { if (testTag != null) it.testTag(testTag) else it }
            .defaultMinSize(minWidth = 54.dp, minHeight = Spacing.touchTarget)
            .semantics {
                role = Role.Switch
                this.selected = checked
                this.contentDescription = contentDescription
            }
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                enabled = enabled
            ) {
                if (config.hapticsEnabled) {
                    view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                }
                onCheckedChange(!checked)
            }
            .alpha(if (enabled) 1f else 0.48f),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(trackWidth, trackHeight)
                .liquidGlass(
                    shape = CircleShape,
                    cornerRadius = 999.dp,
                    variant = if (checked) GlassVariant.TINTED else GlassVariant.CLEAR,
                    tintOverride = trackTint,
                    isPressed = pressState.isPressed
                )
        ) {
            Box(
                modifier = Modifier
                    .offset(x = if (checked && pressState.isPressed) thumbOffset - 6.dp else thumbOffset)
                    .align(Alignment.CenterStart)
                    .size(width = thumbWidthStretch, height = thumbSize)
                    .glassElevationShadow(4.dp, CircleShape)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.96f))
                    .border(0.75.dp, Color.White, CircleShape)
            )
        }
    }
}

@Composable
fun GlassSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    valueRange: ClosedFloatingPointRange<Float> = 0f..1f,
    enabled: Boolean = true,
    showGlassThumb: Boolean = true,
    accentColor: Color? = null,
    contentDescription: String = "Slider",
    testTag: String? = null
) {
    val config = LocalGlassConfig.current
    val glassColors = LocalGlassColors.current
    val view = LocalView.current
    val density = LocalDensity.current
    var isDragging by remember { mutableStateOf(false) }

    val fraction = ((value - valueRange.start) / (valueRange.endInclusive - valueRange.start).coerceAtLeast(0.0001f))
        .coerceIn(0f, 1f)

    val thumbScale by animateFloatAsState(
        targetValue = if (isDragging && !config.reduceMotion) 1.24f else 1f,
        animationSpec = spring(dampingRatio = 0.65f, stiffness = config.springStiffness),
        label = "glassSliderThumbScale"
    )
    val trackColor = accentColor ?: if (glassColors.isDark) SystemBlueDark else SystemBlue

    BoxWithConstraints(
        modifier = modifier
            .let { if (testTag != null) it.testTag(testTag) else it }
            .fillMaxWidth()
            .defaultMinSize(minHeight = Spacing.touchTarget)
            .semantics { this.contentDescription = contentDescription }
            .pointerInput(enabled, valueRange) {
                if (!enabled) return@pointerInput
                detectTapGestures { offset ->
                    val frac = (offset.x / size.width.toFloat()).coerceIn(0f, 1f)
                    val mapped = valueRange.start + frac * (valueRange.endInclusive - valueRange.start)
                    onValueChange(mapped)
                    if (config.hapticsEnabled) {
                        view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                    }
                }
            }
            .pointerInput(enabled, valueRange) {
                if (!enabled) return@pointerInput
                detectDragGestures(
                    onDragStart = { isDragging = true },
                    onDragEnd = { isDragging = false },
                    onDragCancel = { isDragging = false }
                ) { change, _ ->
                    change.consume()
                    val frac = (change.position.x / size.width.toFloat()).coerceIn(0f, 1f)
                    val mapped = valueRange.start + frac * (valueRange.endInclusive - valueRange.start)
                    onValueChange(mapped)
                }
            }
            .alpha(if (enabled) 1f else 0.45f),
        contentAlignment = Alignment.CenterStart
    ) {
        val totalWidthPx = with(density) { maxWidth.toPx() }
        val thumbW = 34.dp
        val thumbWPx = with(density) { thumbW.toPx() }
        val travelPx = (totalWidthPx - thumbWPx).coerceAtLeast(1f)
        val thumbOffsetPx = (fraction * travelPx).roundToInt()

        // Track
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(CircleShape)
                .background(if (glassColors.isDark) Color(0x38FFFFFF) else Color(0x22000000))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(fraction.coerceAtLeast(0.02f))
                    .height(6.dp)
                    .clip(CircleShape)
                    .background(trackColor)
            )
        }

        // Translucent refractive glass pill thumb (matches PDF page 16, 26, 33, 40)
        if (showGlassThumb) {
            Box(
                modifier = Modifier
                    .offset { IntOffset(thumbOffsetPx, 0) }
                    .size(width = thumbW, height = 24.dp)
                    .graphicsLayer {
                        scaleX = thumbScale
                        scaleY = thumbScale
                    }
                    .glassElevationShadow(if (isDragging) 12.dp else 6.dp, CircleShape)
                    .liquidGlass(
                        shape = CircleShape,
                        cornerRadius = 999.dp,
                        variant = GlassVariant.CLEAR,
                        tintOverride = Color.White.copy(alpha = if (isDragging) 0.42f else 0.72f),
                        isPressed = isDragging
                    )
            )
        }
    }
}

@Composable
fun GlassStepper(
    value: Int,
    onValueChange: (Int) -> Unit,
    range: IntRange,
    modifier: Modifier = Modifier,
    unitSuffix: String = "",
    testTag: String? = null
) {
    val glassColors = LocalGlassColors.current
    Row(
        modifier = modifier
            .let { if (testTag != null) it.testTag(testTag) else it }
            .glassElevationShadow(ElevationRules.restingShadow, CircleShape)
            .liquidGlass(shape = CircleShape, cornerRadius = 999.dp, variant = GlassVariant.REGULAR)
            .padding(horizontal = 6.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(if (glassColors.isDark) Color(0x28FFFFFF) else Color(0x44FFFFFF))
                .clickable(enabled = value > range.first) { onValueChange(value - 1) }
                .semantics {
                    role = Role.Button
                    this.contentDescription = "Decrease"
                },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Rounded.Remove,
                contentDescription = "Decrease",
                tint = if (value > range.first) glassColors.textPrimary else glassColors.textTertiary,
                modifier = Modifier.size(19.dp)
            )
        }
        Text(
            text = "$value$unitSuffix",
            style = MaterialTheme.typography.labelLarge,
            color = glassColors.textPrimary,
            modifier = Modifier.padding(horizontal = 6.dp)
        )
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(if (glassColors.isDark) Color(0x28FFFFFF) else Color(0x44FFFFFF))
                .clickable(enabled = value < range.last) { onValueChange(value + 1) }
                .semantics {
                    role = Role.Button
                    this.contentDescription = "Increase"
                },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Rounded.Add,
                contentDescription = "Increase",
                tint = if (value < range.last) glassColors.textPrimary else glassColors.textTertiary,
                modifier = Modifier.size(19.dp)
            )
        }
    }
}

@Composable
fun <T> GlassSegmentedControl(
    options: List<T>,
    selectedOption: T,
    onOptionSelected: (T) -> Unit,
    labelProvider: (T) -> String,
    modifier: Modifier = Modifier,
    testTag: String? = null
) {
    val glassColors = LocalGlassColors.current
    val config = LocalGlassConfig.current

    BoxWithConstraints(
        modifier = modifier
            .let { if (testTag != null) it.testTag(testTag) else it }
            .glassElevationShadow(ElevationRules.floatingBarShadow, CircleShape)
            .liquidGlass(shape = CircleShape, cornerRadius = 999.dp, variant = GlassVariant.REGULAR)
            .padding(4.dp)
    ) {
        val count = options.size.coerceAtLeast(1)
        val itemWidth = maxWidth / count
        val selectedIndex = options.indexOf(selectedOption).coerceAtLeast(0)
        val indicatorOffset by animateDpAsState(
            targetValue = itemWidth * selectedIndex,
            animationSpec = if (config.reduceMotion) tween(0) else spring(
                dampingRatio = 0.72f,
                stiffness = config.springStiffness
            ),
            label = "glassSegmentedPill"
        )

        // Sliding active specular pill (flat tonal highlight inside outer glass capsule)
        Box(
            modifier = Modifier
                .offset(x = indicatorOffset)
                .width(itemWidth)
                .height(38.dp)
                .glassElevationShadow(3.dp, CircleShape)
                .clip(CircleShape)
                .background(if (glassColors.isDark) Color(0x3DFFFFFF) else Color(0xE8FFFFFF))
                .border(0.8.dp, Color.White.copy(alpha = if (glassColors.isDark) 0.25f else 0.9f), CircleShape)
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            options.forEach { option ->
                val isSelected = option == selectedOption
                val label = labelProvider(option)
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .defaultMinSize(minHeight = 38.dp)
                        .clip(CircleShape)
                        .semantics {
                            role = Role.Tab
                            selected = isSelected
                            contentDescription = label
                        }
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) { onOptionSelected(option) },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium
                        ),
                        color = if (isSelected) glassColors.textPrimary else glassColors.textSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(horizontal = 8.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun GlassSearchField(
    query: String,
    onQueryChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    onVoiceClick: (() -> Unit)? = null,
    onClearOrClose: (() -> Unit)? = null,
    onSubmit: (() -> Unit)? = null,
    testTag: String = "glass_search_field"
) {
    val glassColors = LocalGlassColors.current
    Row(
        modifier = modifier
            .testTag(testTag)
            .defaultMinSize(minHeight = 52.dp)
            .glassElevationShadow(ElevationRules.floatingBarShadow, CircleShape)
            .liquidGlass(shape = CircleShape, cornerRadius = 999.dp, variant = GlassVariant.REGULAR)
            .padding(horizontal = 14.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Rounded.Search,
            contentDescription = "Search",
            tint = glassColors.textSecondary,
            modifier = Modifier.size(20.dp)
        )
        Spacer(Modifier.width(10.dp))
        Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
            if (query.isEmpty()) {
                Text(
                    text = placeholder,
                    style = MaterialTheme.typography.bodyLarge,
                    color = glassColors.textTertiary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            BasicTextField(
                value = query,
                onValueChange = onQueryChange,
                singleLine = true,
                textStyle = MaterialTheme.typography.bodyLarge.copy(color = glassColors.textPrimary),
                cursorBrush = SolidColor(glassColors.accentBlue),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { onSubmit?.invoke() }),
                modifier = Modifier.fillMaxWidth()
            )
        }
        if (query.isNotEmpty() || onClearOrClose != null) {
            Box(
                modifier = Modifier
                    .testTag("${testTag}_clear")
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(if (glassColors.isDark) Color(0x28FFFFFF) else Color(0x44FFFFFF))
                    .clickable {
                        if (query.isNotEmpty()) onQueryChange("") else onClearOrClose?.invoke()
                    }
                    .semantics {
                        role = Role.Button
                        this.contentDescription = if (query.isNotEmpty()) "Clear search" else "Close search"
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.Close,
                    contentDescription = if (query.isNotEmpty()) "Clear search" else "Close search",
                    tint = glassColors.textPrimary,
                    modifier = Modifier.size(18.dp)
                )
            }
        } else if (onVoiceClick != null) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(if (glassColors.isDark) Color(0x28FFFFFF) else Color(0x44FFFFFF))
                    .clickable(onClick = onVoiceClick)
                    .semantics {
                        role = Role.Button
                        this.contentDescription = "Voice search"
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.Mic,
                    contentDescription = "Voice search",
                    tint = glassColors.textPrimary,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
fun GlassChip(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    selected: Boolean = false,
    leadingIcon: ImageVector? = null,
    tintOverride: Color? = null,
    testTag: String? = null
) {
    val glassColors = LocalGlassColors.current
    GlassButton(
        onClick = onClick,
        modifier = modifier.height(38.dp),
        variant = if (selected) GlassVariant.PROMINENT else GlassVariant.REGULAR,
        selected = selected,
        tintOverride = tintOverride ?: if (selected) glassColors.accentBlue.copy(alpha = 0.88f) else null,
        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
        contentDescription = label,
        testTag = testTag
    ) {
        if (leadingIcon != null) {
            Icon(
                imageVector = leadingIcon,
                contentDescription = null,
                modifier = Modifier.size(16.dp)
            )
            Spacer(Modifier.width(6.dp))
        }
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            maxLines = 1
        )
    }
}

@Composable
fun GlassBadge(
    text: String? = null,
    icon: ImageVector? = null,
    modifier: Modifier = Modifier,
    iconTint: Color = Color.White,
    tintOverride: Color = Color(0x660B0E14),
    contentDescription: String? = text
) {
    Row(
        modifier = modifier
            .semantics {
                if (contentDescription != null) this.contentDescription = contentDescription
            }
            .liquidGlass(
                shape = CircleShape,
                cornerRadius = 999.dp,
                variant = GlassVariant.CLEAR,
                tintOverride = tintOverride,
                adaptiveDarkBackdrop = true
            )
            .padding(horizontal = if (text != null) 7.dp else 5.dp, vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(12.dp)
            )
        }
        if (text != null) {
            Text(
                text = text,
                style = MaterialTheme.typography.labelSmall,
                color = Color.White,
                maxLines = 1
            )
        }
    }
}

@Composable
fun GlassProgressLoader(
    modifier: Modifier = Modifier,
    label: String? = null,
    progress: Float? = null
) {
    val glassColors = LocalGlassColors.current
    GlassCard(
        modifier = modifier,
        cornerRadius = CornerRadius.lg,
        variant = GlassVariant.REGULAR,
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 14.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (progress != null) {
                CircularProgressIndicator(
                    progress = { progress.coerceIn(0f, 1f) },
                    modifier = Modifier.size(24.dp),
                    strokeWidth = 2.5.dp,
                    color = glassColors.accentBlue
                )
            } else {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    strokeWidth = 2.5.dp,
                    color = glassColors.accentBlue
                )
            }
            if (label != null) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodyMedium,
                    color = glassColors.textPrimary
                )
            }
        }
    }
}

@Composable
fun GlassScrubberBubble(
    label: String,
    modifier: Modifier = Modifier
) {
    val glassColors = LocalGlassColors.current
    Box(
        modifier = modifier
            .glassElevationShadow(ElevationRules.floatingBarShadow, CircleShape)
            .liquidGlass(shape = CircleShape, cornerRadius = 999.dp, variant = GlassVariant.TINTED)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = glassColors.textPrimary,
            maxLines = 1
        )
    }
}
