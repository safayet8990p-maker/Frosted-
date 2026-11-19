package com.example.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Collections
import androidx.compose.material.icons.rounded.PhotoLibrary
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.VerifiedUser
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.model.MediaPermissionState
import com.example.glass.GlassButton
import com.example.glass.GlassCard
import com.example.glass.GlassVariant
import com.example.glass.LocalGlassConfig
import com.example.glass.glassBackdropSource
import com.example.glass.rememberGlassBackdropState
import com.example.ui.i18n.LocalAppStrings
import com.example.ui.theme.CornerRadius
import com.example.ui.theme.LocalGlassColors
import com.example.ui.theme.Spacing
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun GlasslySplashScreen(
    onFinished: () -> Unit
) {
    val config = LocalGlassConfig.current
    val alphaAnim = remember { Animatable(0f) }
    val scaleAnim = remember { Animatable(0.86f) }

    LaunchedEffect(Unit) {
        if (config.reduceMotion) {
            alphaAnim.snapTo(1f)
            scaleAnim.snapTo(1f)
            delay(300)
        } else {
            launch { alphaAnim.animateTo(1f, tween(340)) }
            launch { scaleAnim.animateTo(1f, spring(dampingRatio = 0.72f, stiffness = Spring.StiffnessMediumLow)) }
            delay(700)
        }
        onFinished()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0A1A36)),
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painterResource(id = R.drawable.img_backdrop_aurora),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer { alpha = 0.55f }
        )

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.graphicsLayer {
                alpha = alphaAnim.value
                scaleX = scaleAnim.value
                scaleY = scaleAnim.value
            }
        ) {
            Image(
                painter = painterResource(id = R.drawable.glassly_icon_fg),
                contentDescription = "Glassly Logo",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(116.dp)
                    .clip(RoundedCornerShape(28.dp))
            )
            Spacer(Modifier.height(Spacing.lg))
            Text(
                text = "Glassly",
                style = MaterialTheme.typography.displayLarge,
                color = Color.White
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = "Version 1.0.7",
                style = MaterialTheme.typography.labelMedium,
                color = Color.White.copy(alpha = 0.75f)
            )
        }
    }
}

private data class TourSlide(
    val icon: ImageVector,
    val title: String,
    val body: String,
    val previewRes: Int
)

@Composable
fun WelcomeTourAndPermissionScreen(
    permissionState: MediaPermissionState,
    onRequestMediaPermissions: () -> Unit,
    onContinueToLibrary: () -> Unit
) {
    val strings = LocalAppStrings.current
    val glassColors = LocalGlassColors.current
    val backdropState = rememberGlassBackdropState()
    val scope = rememberCoroutineScope()

    val slides = listOf(
        TourSlide(
            icon = Icons.Rounded.AutoAwesome,
            title = strings.welcomeTourTitle1,
            body = strings.welcomeTourBody1,
            previewRes = R.drawable.img_backdrop_aurora
        ),
        TourSlide(
            icon = Icons.Rounded.Search,
            title = strings.welcomeTourTitle2,
            body = strings.welcomeTourBody2,
            previewRes = R.drawable.img_sample_aegean
        ),
        TourSlide(
            icon = Icons.Rounded.PhotoLibrary,
            title = strings.welcomeTourTitle3,
            body = strings.welcomeTourBody3,
            previewRes = R.drawable.img_sample_alpine
        )
    )
    val pagerState = rememberPagerState(pageCount = { slides.size + 1 }) // +1 permission page

    androidx.compose.runtime.CompositionLocalProvider(
        com.example.glass.LocalGlassBackdrop provides backdropState
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // Colorful backdrop layer recorded for real glass refraction
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .glassBackdropSource(backdropState)
            ) {
                Image(
                    painter = painterResource(id = R.drawable.img_backdrop_aurora),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }

            // Floating Liquid Glass onboarding UI
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .navigationBarsPadding()
                    .padding(Spacing.lg),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    GlassButton(
                        onClick = onContinueToLibrary,
                        variant = GlassVariant.CLEAR,
                        testTag = "onboarding_skip_button"
                    ) {
                        Text(strings.skipTour, style = MaterialTheme.typography.labelLarge)
                    }
                }

                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier
                        .weight(1f)
                        .widthIn(max = 520.dp)
                ) { page ->
                    if (page < slides.size) {
                        val slide = slides[page]
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            GlassCard(
                                modifier = Modifier.fillMaxWidth(),
                                cornerRadius = CornerRadius.xl,
                                variant = GlassVariant.REGULAR
                            ) {
                                Image(
                                    painter = painterResource(id = slide.previewRes),
                                    contentDescription = slide.title,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(220.dp)
                                        .clip(RoundedCornerShape(CornerRadius.md))
                                )
                                Spacer(Modifier.height(Spacing.xl))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(44.dp)
                                            .clip(CircleShape)
                                            .background(glassColors.accentBlue.copy(alpha = 0.18f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = slide.icon,
                                            contentDescription = null,
                                            tint = glassColors.accentBlue
                                        )
                                    }
                                    Spacer(Modifier.width(Spacing.md))
                                    Text(
                                        text = slide.title,
                                        style = MaterialTheme.typography.headlineMedium,
                                        color = glassColors.textPrimary
                                    )
                                }
                                Spacer(Modifier.height(Spacing.sm))
                                Text(
                                    text = slide.body,
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = glassColors.textSecondary
                                )
                            }
                        }
                    } else {
                        // Friendly iOS-style Photo Permission Card (supports Full / Partial / Denied)
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            GlassCard(
                                modifier = Modifier.fillMaxWidth(),
                                cornerRadius = CornerRadius.xl,
                                variant = GlassVariant.REGULAR
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.VerifiedUser,
                                    contentDescription = "Media Privacy",
                                    tint = glassColors.accentBlue,
                                    modifier = Modifier
                                        .size(56.dp)
                                        .align(Alignment.CenterHorizontally)
                                )
                                Spacer(Modifier.height(Spacing.lg))
                                Text(
                                    text = "Access Your Photos & Videos",
                                    style = MaterialTheme.typography.headlineMedium,
                                    color = glassColors.textPrimary,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.fillMaxWidth()
                                )
                                Spacer(Modifier.height(Spacing.sm))
                                val statusDescription = when (permissionState) {
                                    MediaPermissionState.FULL ->
                                        "Full photo & video access is granted. Your device MediaStore library is ready."
                                    MediaPermissionState.PARTIAL ->
                                        "Selected photos access is active. You can grant full access or pick additional photos anytime."
                                    MediaPermissionState.DENIED, MediaPermissionState.UNKNOWN ->
                                        "Glassly works completely offline on your device. Grant photo & video access to browse your camera roll, or continue with curated showcase photos."
                                }
                                Text(
                                    text = statusDescription,
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = glassColors.textSecondary,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.fillMaxWidth()
                                )
                                Spacer(Modifier.height(Spacing.xl))
                                if (permissionState != MediaPermissionState.FULL) {
                                    GlassButton(
                                        onClick = onRequestMediaPermissions,
                                        modifier = Modifier.fillMaxWidth(),
                                        variant = GlassVariant.PROMINENT,
                                        tintOverride = glassColors.accentBlue,
                                        testTag = "permission_allow_button"
                                    ) {
                                        Text(
                                            text = if (permissionState == MediaPermissionState.PARTIAL) {
                                                strings.manageAccess
                                            } else {
                                                strings.allowAccess
                                            },
                                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold)
                                        )
                                    }
                                    Spacer(Modifier.height(Spacing.md))
                                }
                                GlassButton(
                                    onClick = onContinueToLibrary,
                                    modifier = Modifier.fillMaxWidth(),
                                    variant = if (permissionState == MediaPermissionState.FULL) GlassVariant.PROMINENT else GlassVariant.REGULAR,
                                    testTag = "onboarding_get_started_button"
                                ) {
                                    Text(strings.getStarted, style = MaterialTheme.typography.labelLarge)
                                }
                            }
                        }
                    }
                }

                // Page dots + Continue button
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 520.dp)
                        .padding(vertical = Spacing.md),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        repeat(slides.size + 1) { idx ->
                            val active = pagerState.currentPage == idx
                            Box(
                                modifier = Modifier
                                    .size(width = if (active) 22.dp else 8.dp, height = 8.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (active) Color.White else Color.White.copy(alpha = 0.4f)
                                    )
                            )
                        }
                    }

                    if (pagerState.currentPage < slides.size) {
                        GlassButton(
                            onClick = {
                                scope.launch {
                                    pagerState.animateScrollToPage(pagerState.currentPage + 1)
                                }
                            },
                            variant = GlassVariant.PROMINENT,
                            tintOverride = glassColors.accentBlue,
                            testTag = "onboarding_continue_button"
                        ) {
                            Text(strings.continueLabel, style = MaterialTheme.typography.labelLarge)
                        }
                    } else {
                        Spacer(Modifier.width(48.dp))
                    }
                }
            }
        }
    }
}
