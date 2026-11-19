package com.example.ui.screens

import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.glass.GlassAlertDialog
import com.example.glass.GlassBadge
import com.example.glass.GlassButton
import com.example.glass.GlassCard
import com.example.glass.GlassChip
import com.example.glass.GlassContainer
import com.example.glass.GlassIconButton
import com.example.glass.GlassMenuRow
import com.example.glass.GlassPickerRow
import com.example.glass.GlassPopoverMenu
import com.example.glass.GlassProgressLoader
import com.example.glass.GlassScrubberBubble
import com.example.glass.GlassSearchField
import com.example.glass.GlassSegmentedControl
import com.example.glass.GlassSlider
import com.example.glass.GlassStepper
import com.example.glass.GlassSwitch
import com.example.glass.GlassTopToolbar
import com.example.glass.GlassVariant
import com.example.glass.glassBackdropSource
import com.example.glass.glassContainerChild
import com.example.glass.rememberGlassBackdropState
import com.example.ui.theme.LocalGlassColors
import com.example.ui.theme.Spacing

/**
 * Debug-only Glass Gallery screen showing every shared Liquid Glass component across states.
 */
@Composable
fun GlassGalleryDebugScreen(
    onBack: () -> Unit
) {
    val glassColors = LocalGlassColors.current
    val backdropState = rememberGlassBackdropState()

    var switchState by remember { mutableStateOf(true) }
    var sliderVal by remember { mutableFloatStateOf(0.62f) }
    var stepperVal by remember { mutableIntStateOf(3) }
    var segment by remember { mutableStateOf("Clear") }
    var query by remember { mutableStateOf("") }
    var pickerIdx by remember { mutableIntStateOf(1) }
    var menuOpen by remember { mutableStateOf(false) }
    var alertOpen by remember { mutableStateOf(false) }

    androidx.compose.runtime.CompositionLocalProvider(
        com.example.glass.LocalGlassBackdrop provides backdropState
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Image(
                painter = painterResource(id = R.drawable.img_backdrop_aurora),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .glassBackdropSource(backdropState)
            )

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .navigationBarsPadding(),
                contentPadding = PaddingValues(bottom = 48.dp),
                verticalArrangement = Arrangement.spacedBy(Spacing.lg)
            ) {
                item {
                    GlassTopToolbar(
                        title = "Glass Gallery",
                        subtitle = "Debug Component Spec (v1.0.7)",
                        onBackClick = onBack
                    )
                }

                item {
                    GlassCard(modifier = Modifier.padding(horizontal = Spacing.lg)) {
                        Text("Buttons (Regular / Clear / Tinted / Prominent / Disabled / Loading)", style = MaterialTheme.typography.titleMedium, color = glassColors.textPrimary)
                        Spacer(Modifier.height(Spacing.md))
                        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                            GlassButton(onClick = {}, variant = GlassVariant.REGULAR) { Text("Regular") }
                            GlassButton(onClick = {}, variant = GlassVariant.CLEAR) { Text("Clear") }
                            GlassButton(onClick = {}, variant = GlassVariant.TINTED) { Text("Tinted") }
                        }
                        Spacer(Modifier.height(Spacing.sm))
                        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                            GlassButton(onClick = {}, variant = GlassVariant.PROMINENT, tintOverride = glassColors.accentBlue) { Text("Prominent") }
                            GlassButton(onClick = {}, enabled = false) { Text("Disabled") }
                            GlassButton(onClick = {}, loading = true) { Text("Load") }
                            GlassIconButton(icon = Icons.Rounded.Favorite, contentDescription = "Fav", onClick = {}, selected = true)
                        }
                    }
                }

                item {
                    GlassCard(modifier = Modifier.padding(horizontal = Spacing.lg)) {
                        Text("Controls (Switch, Slider, Stepper, Segmented, Picker)", style = MaterialTheme.typography.titleMedium, color = glassColors.textPrimary)
                        Spacer(Modifier.height(Spacing.md))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            GlassSwitch(checked = switchState, onCheckedChange = { switchState = it })
                            GlassStepper(value = stepperVal, onValueChange = { stepperVal = it }, range = 1..6, unitSuffix = " cols")
                        }
                        Spacer(Modifier.height(Spacing.md))
                        GlassSlider(value = sliderVal, onValueChange = { sliderVal = it })
                        Spacer(Modifier.height(Spacing.md))
                        GlassSegmentedControl(
                            options = listOf("Clear", "Regular", "Tinted"),
                            selectedOption = segment,
                            onOptionSelected = { segment = it },
                            labelProvider = { it }
                        )
                        Spacer(Modifier.height(Spacing.md))
                        GlassPickerRow(
                            options = listOf("Years", "Months", "Days", "All"),
                            selectedIndex = pickerIdx,
                            onSelect = { pickerIdx = it }
                        )
                    }
                }

                item {
                    GlassCard(modifier = Modifier.padding(horizontal = Spacing.lg)) {
                        Text("Search, Chips, Badges, Scrubber & Droplet Morph Container", style = MaterialTheme.typography.titleMedium, color = glassColors.textPrimary)
                        Spacer(Modifier.height(Spacing.md))
                        GlassSearchField(query = query, onQueryChange = { query = it }, placeholder = "Search Glass Gallery...")
                        Spacer(Modifier.height(Spacing.md))
                        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm), verticalAlignment = Alignment.CenterVertically) {
                            GlassChip(label = "Selected", selected = true, onClick = {}, leadingIcon = Icons.Rounded.AutoAwesome)
                            GlassBadge(text = "0:14", icon = Icons.Rounded.PlayArrow)
                            GlassBadge(text = "RAW")
                            GlassScrubberBubble(label = "Nov 2026")
                        }
                        Spacer(Modifier.height(Spacing.md))
                        GlassContainer(modifier = Modifier.fillMaxWidth(), morphSpacing = 18.dp) { cState ->
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                GlassButton(
                                    onClick = { menuOpen = !menuOpen },
                                    modifier = Modifier.glassContainerChild(cState, "a")
                                ) {
                                    Text("Expand Menu")
                                }
                                GlassButton(
                                    onClick = { alertOpen = true },
                                    variant = GlassVariant.TINTED,
                                    modifier = Modifier.glassContainerChild(cState, "b")
                                ) {
                                    Text("Expand Alert")
                                }
                            }
                        }
                        Spacer(Modifier.height(Spacing.md))
                        GlassProgressLoader(label = "Refracting backdrop...", progress = sliderVal)
                    }
                }
            }

            if (menuOpen) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(start = Spacing.xl, bottom = 140.dp)
                ) {
                    GlassPopoverMenu(
                        expanded = menuOpen,
                        onDismissRequest = { menuOpen = false },
                        origin = TransformOrigin(0.2f, 1f)
                    ) {
                        GlassMenuRow("Sort by Date Captured", onClick = { menuOpen = false }, checked = true)
                        GlassMenuRow("View Options", onClick = { menuOpen = false }, leadingIcon = Icons.Rounded.Tune)
                    }
                }
            }

            GlassAlertDialog(
                visible = alertOpen,
                title = "Liquid Glass Alert",
                message = "Alerts expand directly from their trigger control with increased thickness and soft shadow.",
                confirmLabel = "Confirm",
                cancelLabel = "Cancel",
                onConfirm = { alertOpen = false },
                onDismiss = { alertOpen = false }
            )
        }
    }
}
