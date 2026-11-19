package com.example.data.repository

import android.content.ContentUris
import android.content.Context
import android.database.ContentObserver
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import androidx.exifinterface.media.ExifInterface
import com.example.R
import com.example.data.local.AlbumMediaCrossRef
import com.example.data.local.CustomAlbumEntity
import com.example.data.local.EditRecipeCodec
import com.example.data.local.GlasslyDao
import com.example.data.local.MediaOverrideEntity
import com.example.data.local.SearchHistoryEntity
import com.example.data.model.ExifMetadata
import com.example.data.model.MediaItem
import com.example.data.model.MediaPermissionState
import com.example.data.model.MediaType
import com.example.data.model.PhotoEditState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.util.Calendar
import java.util.Locale

class MediaRepository(
    private val context: Context,
    private val dao: GlasslyDao
) {
    val customAlbumsFlow: Flow<List<CustomAlbumEntity>> = dao.observeAlbums()
    val albumCrossRefsFlow: Flow<List<AlbumMediaCrossRef>> = dao.observeAlbumCrossRefs()
    val mediaOverridesFlow: Flow<List<MediaOverrideEntity>> = dao.observeMediaOverrides()
    val searchHistoryFlow: Flow<List<SearchHistoryEntity>> = dao.observeSearchHistory()

    fun observeMediaStoreChanges(): Flow<Unit> = callbackFlow {
        val observer = object : ContentObserver(Handler(Looper.getMainLooper())) {
            override fun onChange(selfChange: Boolean) {
                trySend(Unit)
            }
        }
        val resolver = context.contentResolver
        resolver.registerContentObserver(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, true, observer)
        resolver.registerContentObserver(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, true, observer)
        trySend(Unit)
        awaitClose {
            resolver.unregisterContentObserver(observer)
        }
    }

    suspend fun loadLibraryMedia(permissionState: MediaPermissionState): List<MediaItem> = withContext(Dispatchers.IO) {
        val overrides = dao.observeMediaOverrides().first().associateBy { it.mediaId }
        val deviceItems = if (permissionState == MediaPermissionState.FULL || permissionState == MediaPermissionState.PARTIAL) {
            queryDeviceMediaStore()
        } else {
            emptyList()
        }

        val combined = if (deviceItems.isNotEmpty()) {
            deviceItems
        } else {
            buildCuratedShowcaseMedia()
        }

        combined.map { item ->
            val ov = overrides[item.id]
            if (ov == null) item
            else item.copy(
                isFavorite = ov.isFavorite ?: item.isFavorite,
                isHidden = ov.isHidden ?: item.isHidden,
                isTrashed = ov.isTrashed ?: item.isTrashed,
                trashedAtMillis = ov.trashedAtMillis ?: item.trashedAtMillis,
                dateTakenMillis = ov.customDateTakenMillis ?: item.dateTakenMillis,
                locationName = ov.customLocationName ?: item.locationName,
                editState = EditRecipeCodec.decode(ov.editRecipeJson),
                isEdited = ov.isEdited
            )
        }
    }

    private fun queryDeviceMediaStore(): List<MediaItem> {
        val items = mutableListOf<MediaItem>()
        val resolver = context.contentResolver

        val imageProjection = arrayOf(
            MediaStore.Images.Media._ID,
            MediaStore.Images.Media.DISPLAY_NAME,
            MediaStore.Images.Media.MIME_TYPE,
            MediaStore.Images.Media.DATE_TAKEN,
            MediaStore.Images.Media.DATE_ADDED,
            MediaStore.Images.Media.WIDTH,
            MediaStore.Images.Media.HEIGHT,
            MediaStore.Images.Media.SIZE,
            MediaStore.Images.Media.BUCKET_ID,
            MediaStore.Images.Media.BUCKET_DISPLAY_NAME,
            MediaStore.Images.Media.RELATIVE_PATH,
            MediaStore.Images.Media.IS_FAVORITE
        )

        runCatching {
            resolver.query(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                imageProjection,
                null,
                null,
                "${MediaStore.Images.Media.DATE_TAKEN} ASC"
            )?.use { cursor ->
                val idCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
                val nameCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DISPLAY_NAME)
                val mimeCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.MIME_TYPE)
                val takenCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_TAKEN)
                val addedCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_ADDED)
                val wCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.WIDTH)
                val hCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.HEIGHT)
                val sizeCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.SIZE)
                val bIdCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.BUCKET_ID)
                val bNameCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.BUCKET_DISPLAY_NAME)
                val pathCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.RELATIVE_PATH)
                val favCol = cursor.getColumnIndex(MediaStore.Images.Media.IS_FAVORITE)

                while (cursor.moveToNext()) {
                    val msId = cursor.getLong(idCol)
                    val uri = ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, msId)
                    val name = cursor.getString(nameCol) ?: "IMG_$msId.jpg"
                    val mime = cursor.getString(mimeCol) ?: "image/jpeg"
                    val addedSec = cursor.getLong(addedCol)
                    val taken = cursor.getLong(takenCol).takeIf { it > 0L } ?: (addedSec * 1000L)
                    val bucket = cursor.getString(bNameCol) ?: "Camera"
                    val path = cursor.getString(pathCol) ?: "/Pictures"
                    val isScreenshot = bucket.contains("screenshot", true) || path.contains("screenshot", true)
                    val isSelfie = bucket.contains("selfie", true) || name.contains("selfie", true)
                    val type = when {
                        isScreenshot -> MediaType.SCREENSHOT
                        mime.contains("gif", true) -> MediaType.GIF
                        mime.contains("dng", true) || mime.contains("raw", true) -> MediaType.RAW
                        name.contains("MVIMG", true) || name.contains("MP", false) -> MediaType.MOTION_PHOTO
                        else -> MediaType.PHOTO
                    }
                    items.add(
                        MediaItem(
                            id = "img_$msId",
                            mediaStoreId = msId,
                            uriString = uri.toString(),
                            displayName = name,
                            mimeType = mime,
                            mediaType = type,
                            dateTakenMillis = taken,
                            dateAddedMillis = addedSec * 1000L,
                            width = cursor.getInt(wCol).coerceAtLeast(1),
                            height = cursor.getInt(hCol).coerceAtLeast(1),
                            sizeBytes = cursor.getLong(sizeCol),
                            bucketId = cursor.getString(bIdCol) ?: "camera",
                            bucketName = bucket,
                            filePath = path,
                            isFavorite = if (favCol >= 0) cursor.getInt(favCol) == 1 else false,
                            isScreenshot = isScreenshot,
                            isSelfie = isSelfie
                        )
                    )
                }
            }
        }

        val videoProjection = arrayOf(
            MediaStore.Video.Media._ID,
            MediaStore.Video.Media.DISPLAY_NAME,
            MediaStore.Video.Media.MIME_TYPE,
            MediaStore.Video.Media.DATE_TAKEN,
            MediaStore.Video.Media.DATE_ADDED,
            MediaStore.Video.Media.DURATION,
            MediaStore.Video.Media.WIDTH,
            MediaStore.Video.Media.HEIGHT,
            MediaStore.Video.Media.SIZE,
            MediaStore.Video.Media.BUCKET_ID,
            MediaStore.Video.Media.BUCKET_DISPLAY_NAME,
            MediaStore.Video.Media.RELATIVE_PATH,
            MediaStore.Video.Media.IS_FAVORITE
        )

        runCatching {
            resolver.query(
                MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                videoProjection,
                null,
                null,
                "${MediaStore.Video.Media.DATE_TAKEN} ASC"
            )?.use { cursor ->
                val idCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media._ID)
                val nameCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DISPLAY_NAME)
                val mimeCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.MIME_TYPE)
                val takenCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATE_TAKEN)
                val addedCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATE_ADDED)
                val durCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DURATION)
                val wCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.WIDTH)
                val hCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.HEIGHT)
                val sizeCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.SIZE)
                val bIdCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.BUCKET_ID)
                val bNameCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.BUCKET_DISPLAY_NAME)
                val pathCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.RELATIVE_PATH)
                val favCol = cursor.getColumnIndex(MediaStore.Video.Media.IS_FAVORITE)

                while (cursor.moveToNext()) {
                    val msId = cursor.getLong(idCol)
                    val uri = ContentUris.withAppendedId(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, msId)
                    val addedSec = cursor.getLong(addedCol)
                    val taken = cursor.getLong(takenCol).takeIf { it > 0L } ?: (addedSec * 1000L)
                    items.add(
                        MediaItem(
                            id = "vid_$msId",
                            mediaStoreId = msId,
                            uriString = uri.toString(),
                            displayName = cursor.getString(nameCol) ?: "VID_$msId.mp4",
                            mimeType = cursor.getString(mimeCol) ?: "video/mp4",
                            mediaType = MediaType.VIDEO,
                            dateTakenMillis = taken,
                            dateAddedMillis = addedSec * 1000L,
                            durationMs = cursor.getLong(durCol),
                            width = cursor.getInt(wCol).coerceAtLeast(1),
                            height = cursor.getInt(hCol).coerceAtLeast(1),
                            sizeBytes = cursor.getLong(sizeCol),
                            bucketId = cursor.getString(bIdCol) ?: "video",
                            bucketName = cursor.getString(bNameCol) ?: "Videos",
                            filePath = cursor.getString(pathCol) ?: "/Movies",
                            isFavorite = if (favCol >= 0) cursor.getInt(favCol) == 1 else false
                        )
                    )
                }
            }
        }

        return items
    }

    suspend fun readExifMetadata(item: MediaItem): ExifMetadata = withContext(Dispatchers.IO) {
        if (item.isSampleFallback) return@withContext item.exif
        return@withContext runCatching {
            val uri = Uri.parse(item.uriString)
            context.contentResolver.openInputStream(uri)?.use { input ->
                val exif = ExifInterface(input)
                val model = exif.getAttribute(ExifInterface.TAG_MODEL) ?: "Android Camera"
                val fNumber = exif.getAttribute(ExifInterface.TAG_F_NUMBER)?.let { "f/$it" } ?: "f/1.8"
                val iso = exif.getAttribute(ExifInterface.TAG_PHOTOGRAPHIC_SENSITIVITY)?.let { "ISO $it" } ?: "ISO 100"
                val focal = exif.getAttribute(ExifInterface.TAG_FOCAL_LENGTH_IN_35MM_FILM)?.let { "${it}mm" } ?: "24mm"
                val exp = exif.getAttribute(ExifInterface.TAG_EXPOSURE_TIME)?.toDoubleOrNull()?.let { sec ->
                    if (sec > 0 && sec < 1) "1/${(1.0 / sec).toInt()}s" else "${sec}s"
                } ?: "1/125s"
                ExifMetadata(
                    cameraModel = model,
                    lensInfo = "$focal $fNumber",
                    iso = iso,
                    shutterSpeed = exp,
                    aperture = fNumber
                )
            } ?: item.exif
        }.getOrDefault(item.exif)
    }

    suspend fun toggleFavorite(mediaId: String, favorite: Boolean) {
        val current = dao.getOverride(mediaId) ?: MediaOverrideEntity(mediaId = mediaId)
        dao.upsertOverride(current.copy(isFavorite = favorite))
    }

    suspend fun setHidden(mediaId: String, hidden: Boolean) {
        val current = dao.getOverride(mediaId) ?: MediaOverrideEntity(mediaId = mediaId)
        dao.upsertOverride(current.copy(isHidden = hidden))
    }

    suspend fun moveToTrash(mediaId: String) {
        val current = dao.getOverride(mediaId) ?: MediaOverrideEntity(mediaId = mediaId)
        dao.upsertOverride(
            current.copy(
                isTrashed = true,
                trashedAtMillis = System.currentTimeMillis()
            )
        )
    }

    suspend fun restoreFromTrash(mediaId: String) {
        val current = dao.getOverride(mediaId) ?: MediaOverrideEntity(mediaId = mediaId)
        dao.upsertOverride(current.copy(isTrashed = false, trashedAtMillis = null))
    }

    suspend fun deletePermanently(mediaId: String) {
        dao.deleteOverride(mediaId)
    }

    suspend fun updateDateAndLocation(mediaId: String, dateMillis: Long?, location: String?) {
        val current = dao.getOverride(mediaId) ?: MediaOverrideEntity(mediaId = mediaId)
        dao.upsertOverride(
            current.copy(
                customDateTakenMillis = dateMillis ?: current.customDateTakenMillis,
                customLocationName = location ?: current.customLocationName
            )
        )
    }

    suspend fun saveEditState(mediaId: String, editState: PhotoEditState) {
        val current = dao.getOverride(mediaId) ?: MediaOverrideEntity(mediaId = mediaId)
        dao.upsertOverride(
            current.copy(
                editRecipeJson = EditRecipeCodec.encode(editState),
                isEdited = editState.hasChanges
            )
        )
    }

    suspend fun createCustomAlbum(title: String): String {
        val id = "album_${System.currentTimeMillis()}"
        dao.upsertAlbum(CustomAlbumEntity(id = id, title = title.trim(), createdAtMillis = System.currentTimeMillis(), sortOrder = 0))
        return id
    }

    suspend fun renameCustomAlbum(albumId: String, title: String) {
        dao.renameAlbum(albumId, title.trim())
    }

    suspend fun deleteCustomAlbum(albumId: String) {
        dao.clearAlbumRefs(albumId)
        dao.deleteAlbum(albumId)
    }

    suspend fun addMediaToAlbum(albumId: String, mediaIds: List<String>) {
        val now = System.currentTimeMillis()
        mediaIds.forEach { mId ->
            dao.addMediaToAlbum(AlbumMediaCrossRef(albumId = albumId, mediaId = mId, addedAtMillis = now))
        }
    }

    suspend fun recordSearch(query: String) {
        val clean = query.trim()
        if (clean.isNotEmpty()) {
            dao.insertSearchHistory(SearchHistoryEntity(query = clean, searchedAtMillis = System.currentTimeMillis()))
        }
    }

    suspend fun clearSearchHistory() = dao.clearSearchHistory()

    fun searchMediaNaturalLanguage(items: List<MediaItem>, rawQuery: String): List<MediaItem> {
        val q = rawQuery.trim().lowercase(Locale.getDefault())
        if (q.isEmpty()) return items
        val tokens = q.split(Regex("\\s+")).filter { it.isNotBlank() }

        val cal = Calendar.getInstance()
        val currentYear = cal.get(Calendar.YEAR)
        val currentMonth = cal.get(Calendar.MONTH)

        return items.filter { item ->
            var matchesAll = true
            val itemCal = Calendar.getInstance().apply { timeInMillis = item.dateTakenMillis }
            val itemYear = itemCal.get(Calendar.YEAR)
            val itemMonth = itemCal.get(Calendar.MONTH)

            if (q.contains("last month")) {
                val targetMonth = if (currentMonth == 0) 11 else currentMonth - 1
                val targetYear = if (currentMonth == 0) currentYear - 1 else currentYear
                if (itemYear != targetYear || itemMonth != targetMonth) matchesAll = false
            } else if (q.contains("this month")) {
                if (itemYear != currentYear || itemMonth != currentMonth) matchesAll = false
            }

            tokens.forEach { token ->
                when (token) {
                    "last", "month", "this", "in", "from", "of" -> Unit
                    "video", "videos", "clip", "clips" -> if (item.mediaType != MediaType.VIDEO) matchesAll = false
                    "photo", "photos", "image", "images" -> if (item.mediaType == MediaType.VIDEO) matchesAll = false
                    "favorite", "favorites", "favourite", "favourites", "liked" -> if (!item.isFavorite) matchesAll = false
                    "screenshot", "screenshots" -> if (!item.isScreenshot) matchesAll = false
                    "edited" -> if (!item.isEdited) matchesAll = false
                    "selfie", "selfies" -> if (!item.isSelfie) matchesAll = false
                    "raw" -> if (item.mediaType != MediaType.RAW) matchesAll = false
                    "gif", "gifs" -> if (item.mediaType != MediaType.GIF) matchesAll = false
                    "spring" -> if (itemMonth !in 2..4) matchesAll = false
                    "summer" -> if (itemMonth !in 5..7) matchesAll = false
                    "autumn", "fall" -> if (itemMonth !in 8..10) matchesAll = false
                    "winter" -> if (itemMonth !in listOf(11, 0, 1)) matchesAll = false
                    else -> {
                        val yearNum = token.toIntOrNull()
                        if (yearNum != null && yearNum in 1995..2040) {
                            if (itemYear != yearNum) matchesAll = false
                        } else {
                            val monthName = itemCal.getDisplayName(Calendar.MONTH, Calendar.LONG, Locale.ENGLISH)?.lowercase() ?: ""
                            val haystack = listOfNotNull(
                                item.displayName,
                                item.bucketName,
                                item.locationName,
                                monthName,
                                item.editState.imageDescription
                            ).joinToString(" ").lowercase()
                            if (!haystack.contains(token)) matchesAll = false
                        }
                    }
                }
            }
            matchesAll
        }
    }

    private fun buildCuratedShowcaseMedia(): List<MediaItem> {
        val now = System.currentTimeMillis()
        val day = 86_400_000L
        return listOf(
            MediaItem(
                id = "sample_1",
                mediaStoreId = null,
                uriString = "sample://aegean_1",
                sampleDrawableRes = R.drawable.img_sample_aegean,
                displayName = "Aegean_Sunset_Santorini.jpg",
                mimeType = "image/jpeg",
                mediaType = MediaType.PHOTO,
                dateTakenMillis = now - 390 * day,
                dateAddedMillis = now - 390 * day,
                width = 1600,
                height = 1200,
                sizeBytes = 4_850_000L,
                bucketId = "aegean",
                bucketName = "Aegean Adventure",
                locationName = "Santorini, Greece",
                latitude = 36.3932,
                longitude = 25.4615,
                isFavorite = true,
                isSampleFallback = true,
                exif = ExifMetadata("Glassly Pro Optics", "24mm f/1.78", "ISO 64", "1/1200s", "f/1.78")
            ),
            MediaItem(
                id = "sample_2",
                mediaStoreId = null,
                uriString = "sample://alpine_1",
                sampleDrawableRes = R.drawable.img_sample_alpine,
                displayName = "Dolomites_Morning_Lake.dng",
                mimeType = "image/x-adobe-dng",
                mediaType = MediaType.RAW,
                dateTakenMillis = now - 210 * day,
                dateAddedMillis = now - 210 * day,
                width = 1200,
                height = 1600,
                sizeBytes = 28_400_000L,
                bucketId = "trips",
                bucketName = "Alpine Escape",
                locationName = "Lago di Braies, Italy",
                latitude = 46.6943,
                longitude = 12.0854,
                isFavorite = true,
                isEdited = true,
                isSampleFallback = true,
                exif = ExifMetadata("Glassly Cinema Sensor", "35mm f/1.6", "ISO 80", "1/640s", "f/1.6")
            ),
            MediaItem(
                id = "sample_3",
                mediaStoreId = null,
                uriString = "sample://poppy_video",
                sampleDrawableRes = R.drawable.img_sample_poppy,
                displayName = "California_Poppies_Motion.mp4",
                mimeType = "video/mp4",
                mediaType = MediaType.VIDEO,
                dateTakenMillis = now - 45 * day,
                dateAddedMillis = now - 45 * day,
                durationMs = 14_000L,
                width = 1920,
                height = 1080,
                sizeBytes = 64_200_000L,
                bucketId = "nature",
                bucketName = "Spring Blooms",
                locationName = "Antelope Valley, CA",
                latitude = 34.7248,
                longitude = -118.3965,
                isFavorite = true,
                isSampleFallback = true
            ),
            MediaItem(
                id = "sample_4",
                mediaStoreId = null,
                uriString = "sample://aurora_1",
                sampleDrawableRes = R.drawable.img_backdrop_aurora,
                displayName = "Liquid_Prism_Wallpaper.png",
                mimeType = "image/png",
                mediaType = MediaType.MOTION_PHOTO,
                dateTakenMillis = now - 28 * day,
                dateAddedMillis = now - 28 * day,
                width = 1080,
                height = 1920,
                sizeBytes = 6_200_000L,
                bucketId = "wallpapers",
                bucketName = "Wallpaper Picks",
                locationName = "Reykjavik, Iceland",
                isSharedWithYou = true,
                isSampleFallback = true
            ),
            MediaItem(
                id = "sample_5",
                mediaStoreId = null,
                uriString = "sample://aegean_2",
                sampleDrawableRes = R.drawable.img_sample_aegean,
                displayName = "Screenshot_LiquidGlass_UI.png",
                mimeType = "image/png",
                mediaType = MediaType.SCREENSHOT,
                dateTakenMillis = now - 12 * day,
                dateAddedMillis = now - 12 * day,
                width = 1290,
                height = 2796,
                sizeBytes = 3_100_000L,
                bucketId = "screenshots",
                bucketName = "Screenshots",
                isScreenshot = true,
                isSampleFallback = true
            ),
            MediaItem(
                id = "sample_6",
                mediaStoreId = null,
                uriString = "sample://alpine_2",
                sampleDrawableRes = R.drawable.img_sample_alpine,
                displayName = "Glacier_Ridge_Panorama.jpg",
                mimeType = "image/jpeg",
                mediaType = MediaType.PHOTO,
                dateTakenMillis = now - 4 * day,
                dateAddedMillis = now - 4 * day,
                width = 1600,
                height = 1200,
                sizeBytes = 5_100_000L,
                bucketId = "trips",
                bucketName = "Alpine Escape",
                locationName = "Zermatt, Switzerland",
                latitude = 46.0207,
                longitude = 7.7491,
                isSharedWithYou = true,
                isSampleFallback = true
            ),
            MediaItem(
                id = "sample_7",
                mediaStoreId = null,
                uriString = "sample://poppy_2",
                sampleDrawableRes = R.drawable.img_sample_poppy,
                displayName = "Golden_Hour_Petals_4K.mp4",
                mimeType = "video/mp4",
                mediaType = MediaType.VIDEO,
                dateTakenMillis = now - 2 * day,
                dateAddedMillis = now - 2 * day,
                durationMs = 253_000L,
                width = 3840,
                height = 2160,
                sizeBytes = 148_000_000L,
                bucketId = "nature",
                bucketName = "Spring Blooms",
                locationName = "Big Sur, CA",
                isFavorite = false,
                isSampleFallback = true
            ),
            MediaItem(
                id = "sample_8",
                mediaStoreId = null,
                uriString = "sample://aegean_3",
                sampleDrawableRes = R.drawable.img_sample_aegean,
                displayName = "Oia_Cliffside_GIF.gif",
                mimeType = "image/gif",
                mediaType = MediaType.GIF,
                dateTakenMillis = now - day,
                dateAddedMillis = now - day,
                width = 1200,
                height = 900,
                sizeBytes = 9_400_000L,
                bucketId = "aegean",
                bucketName = "Aegean Adventure",
                locationName = "Oia, Greece",
                isSelfie = true,
                isSampleFallback = true
            )
        )
    }

    companion object {
        fun formatDuration(durationMs: Long): String {
            val totalSec = (durationMs / 1000L).coerceAtLeast(0L)
            val hours = totalSec / 3600
            val mins = (totalSec % 3600) / 60
            val secs = totalSec % 60
            return if (hours > 0) String.format(Locale.US, "%d:%02d:%02d", hours, mins, secs)
            else String.format(Locale.US, "%d:%02d", mins, secs)
        }

        fun formatBytes(bytes: Long): String {
            return when {
                bytes >= 1_000_000_000L -> String.format(Locale.US, "%.2f GB", bytes / 1_000_000_000.0)
                bytes >= 1_000_000L -> String.format(Locale.US, "%.1f MB", bytes / 1_000_000.0)
                bytes >= 1_000L -> String.format(Locale.US, "%.0f KB", bytes / 1_000.0)
                else -> "$bytes B"
            }
        }
    }
}
