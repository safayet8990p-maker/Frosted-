package com.example.ui.components

import android.graphics.Bitmap
import android.net.Uri
import android.util.Size
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BrokenImage
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.data.model.FilterPreset
import com.example.data.model.MediaItem
import com.example.data.model.MediaType
import com.example.data.model.PhotoEditState
import com.example.ui.theme.LocalGlassColors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun MediaThumbnail(
    item: MediaItem,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop,
    applyEdits: Boolean = true,
    contentDescription: String? = item.displayName
) {
    val context = LocalContext.current
    val glassColors = LocalGlassColors.current
    val colorFilter = remember(item.editState, applyEdits) {
        if (applyEdits && item.editState.hasChanges) buildEditColorFilter(item.editState) else null
    }
    val editRot = if (applyEdits) item.editState.rotationDegrees.toFloat() else 0f
    val editFlipX = if (applyEdits && item.editState.flipHorizontal) -1f else 1f
    val editFlipY = if (applyEdits && item.editState.flipVertical) -1f else 1f

    if (item.sampleDrawableRes != null) {
        Image(
            painter = painterResource(id = item.sampleDrawableRes),
            contentDescription = contentDescription,
            contentScale = contentScale,
            colorFilter = colorFilter,
            modifier = modifier.graphicsLayer {
                rotationZ = editRot
                scaleX = editFlipX
                scaleY = editFlipY
            }
        )
        return
    }

    if (item.mediaType == MediaType.VIDEO) {
        var videoThumb by remember(item.uriString) { mutableStateOf<Bitmap?>(null) }
        var loadFailed by remember(item.uriString) { mutableStateOf(false) }

        LaunchedEffect(item.uriString) {
            withContext(Dispatchers.IO) {
                runCatching {
                    context.contentResolver.loadThumbnail(
                        Uri.parse(item.uriString),
                        Size(480, 480),
                        null
                    )
                }.onSuccess { bmp ->
                    videoThumb = bmp
                }.onFailure {
                    loadFailed = true
                }
            }
        }

        when {
            videoThumb != null -> {
                Image(
                    bitmap = videoThumb!!.asImageBitmap(),
                    contentDescription = contentDescription,
                    contentScale = contentScale,
                    colorFilter = colorFilter,
                    modifier = modifier
                )
            }
            loadFailed -> CorruptMediaPlaceholder(modifier = modifier)
            else -> ShimmerSkeletonBox(modifier = modifier)
        }
        return
    }

    var isLoading by remember(item.uriString) { mutableStateOf(true) }
    var isError by remember(item.uriString) { mutableStateOf(false) }

    Box(modifier = modifier.background(glassColors.secondaryBackground)) {
        AsyncImage(
            model = ImageRequest.Builder(context)
                .data(Uri.parse(item.uriString))
                .crossfade(true)
                .build(),
            contentDescription = contentDescription,
            contentScale = contentScale,
            colorFilter = colorFilter,
            onLoading = { isLoading = true; isError = false },
            onSuccess = { isLoading = false; isError = false },
            onError = { isLoading = false; isError = true },
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    rotationZ = editRot
                    scaleX = editFlipX
                    scaleY = editFlipY
                }
        )
        if (isLoading) {
            ShimmerSkeletonBox(modifier = Modifier.fillMaxSize())
        } else if (isError) {
            CorruptMediaPlaceholder(modifier = Modifier.fillMaxSize())
        }
    }
}

@Composable
fun ShimmerSkeletonBox(modifier: Modifier = Modifier) {
    val glassColors = LocalGlassColors.current
    val transition = rememberInfiniteTransition(label = "skeletonShimmer")
    val shift by transition.animateFloat(
        initialValue = -300f,
        targetValue = 900f,
        animationSpec = infiniteRepeatable(
            animation = tween(1150, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmerOffset"
    )
    val base = if (glassColors.isDark) Color(0xFF1F222A) else Color(0xFFE3E7EE)
    val highlight = if (glassColors.isDark) Color(0xFF2D323E) else Color(0xFFF3F6FB)
    Box(
        modifier = modifier.background(
            Brush.linearGradient(
                colors = listOf(base, highlight, base),
                start = Offset(shift, shift * 0.4f),
                end = Offset(shift + 320f, shift * 0.4f + 320f)
            )
        )
    )
}

@Composable
fun CorruptMediaPlaceholder(modifier: Modifier = Modifier) {
    val glassColors = LocalGlassColors.current
    Box(
        modifier = modifier.background(glassColors.secondaryBackground),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Rounded.BrokenImage,
            contentDescription = "Unavailable media",
            tint = glassColors.textTertiary,
            modifier = Modifier.size(28.dp)
        )
    }
}

fun buildEditColorFilter(edit: PhotoEditState): ColorFilter? {
    if (!edit.hasChanges) return null
    val matrix = ColorMatrix()

    // Filter preset base
    when (edit.filterPreset) {
        FilterPreset.ORIGINAL -> Unit
        FilterPreset.VIVID -> matrix.setToSaturation(1.38f)
        FilterPreset.DRAMATIC -> {
            val c = 1.24f
            val t = (1f - c) * 128f
            matrix.set(
                ColorMatrix(
                    floatArrayOf(
                        c, 0f, 0f, 0f, t,
                        0f, c, 0f, 0f, t,
                        0f, 0f, c, 0f, t,
                        0f, 0f, 0f, 1f, 0f
                    )
                )
            )
        }
        FilterPreset.MONO, FilterPreset.SILVERTONE -> matrix.setToSaturation(0f)
        FilterPreset.WARM -> matrix.set(
            ColorMatrix(
                floatArrayOf(
                    1.12f, 0f, 0f, 0f, 8f,
                    0f, 1.02f, 0f, 0f, 2f,
                    0f, 0f, 0.88f, 0f, -6f,
                    0f, 0f, 0f, 1f, 0f
                )
            )
        )
        FilterPreset.COOL -> matrix.set(
            ColorMatrix(
                floatArrayOf(
                    0.90f, 0f, 0f, 0f, -4f,
                    0f, 1.01f, 0f, 0f, 2f,
                    0f, 0f, 1.15f, 0f, 10f,
                    0f, 0f, 0f, 1f, 0f
                )
            )
        )
        FilterPreset.NOIR -> {
            val sat = ColorMatrix().apply { setToSaturation(0f) }
            val c = 1.38f
            val t = (1f - c) * 128f
            val con = ColorMatrix(
                floatArrayOf(
                    c, 0f, 0f, 0f, t,
                    0f, c, 0f, 0f, t,
                    0f, 0f, c, 0f, t,
                    0f, 0f, 0f, 1f, 0f
                )
            )
            sat.timesAssign(con)
            matrix.set(sat)
        }
    }

    // Saturation
    if (edit.saturation != 1f) {
        val satM = ColorMatrix().apply { setToSaturation(edit.saturation) }
        matrix.timesAssign(satM)
    }

    // Contrast & brightness + warmth
    val c = edit.contrast
    val b = edit.brightness * 95f
    val t = (1f - c) * 128f + b
    val rWarm = 1f + (edit.warmth * 0.14f)
    val bWarm = 1f - (edit.warmth * 0.14f)
    val adjust = ColorMatrix(
        floatArrayOf(
            c * rWarm, 0f, 0f, 0f, t,
            0f, c, 0f, 0f, t,
            0f, 0f, c * bWarm, 0f, t,
            0f, 0f, 0f, 1f, 0f
        )
    )
    matrix.timesAssign(adjust)
    return ColorFilter.colorMatrix(matrix)
}
