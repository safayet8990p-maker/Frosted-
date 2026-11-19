package com.example.data.local

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.data.model.CropAspectRatio
import com.example.data.model.DrawStroke
import com.example.data.model.FilterPreset
import com.example.data.model.LoupeAnnotation
import com.example.data.model.PhotoEditState
import com.example.data.model.TextOverlay
import kotlinx.coroutines.flow.Flow
import org.json.JSONArray
import org.json.JSONObject

@Entity(tableName = "custom_albums")
data class CustomAlbumEntity(
    @PrimaryKey val id: String,
    val title: String,
    val createdAtMillis: Long,
    val sortOrder: Int
)

@Entity(tableName = "album_media_cross_ref", primaryKeys = ["albumId", "mediaId"])
data class AlbumMediaCrossRef(
    val albumId: String,
    val mediaId: String,
    val addedAtMillis: Long
)

@Entity(tableName = "media_overrides")
data class MediaOverrideEntity(
    @PrimaryKey val mediaId: String,
    val isFavorite: Boolean? = null,
    val isHidden: Boolean? = null,
    val isTrashed: Boolean? = null,
    val trashedAtMillis: Long? = null,
    val customDateTakenMillis: Long? = null,
    val customLocationName: String? = null,
    val editRecipeJson: String? = null,
    val isEdited: Boolean = false
)

@Entity(tableName = "search_history")
data class SearchHistoryEntity(
    @PrimaryKey val query: String,
    val searchedAtMillis: Long
)

@Dao
interface GlasslyDao {
    @Query("SELECT * FROM custom_albums ORDER BY sortOrder ASC, createdAtMillis DESC")
    fun observeAlbums(): Flow<List<CustomAlbumEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAlbum(album: CustomAlbumEntity)

    @Query("UPDATE custom_albums SET title = :title WHERE id = :albumId")
    suspend fun renameAlbum(albumId: String, title: String)

    @Query("DELETE FROM custom_albums WHERE id = :albumId")
    suspend fun deleteAlbum(albumId: String)

    @Query("SELECT * FROM album_media_cross_ref")
    fun observeAlbumCrossRefs(): Flow<List<AlbumMediaCrossRef>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addMediaToAlbum(ref: AlbumMediaCrossRef)

    @Query("DELETE FROM album_media_cross_ref WHERE albumId = :albumId")
    suspend fun clearAlbumRefs(albumId: String)

    @Query("SELECT * FROM media_overrides")
    fun observeMediaOverrides(): Flow<List<MediaOverrideEntity>>

    @Query("SELECT * FROM media_overrides WHERE mediaId = :mediaId LIMIT 1")
    suspend fun getOverride(mediaId: String): MediaOverrideEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertOverride(override: MediaOverrideEntity)

    @Query("DELETE FROM media_overrides WHERE mediaId = :mediaId")
    suspend fun deleteOverride(mediaId: String)

    @Query("SELECT * FROM search_history ORDER BY searchedAtMillis DESC LIMIT 12")
    fun observeSearchHistory(): Flow<List<SearchHistoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSearchHistory(entry: SearchHistoryEntity)

    @Query("DELETE FROM search_history")
    suspend fun clearSearchHistory()
}

@Database(
    entities = [
        CustomAlbumEntity::class,
        AlbumMediaCrossRef::class,
        MediaOverrideEntity::class,
        SearchHistoryEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class GlasslyDatabase : RoomDatabase() {
    abstract fun glasslyDao(): GlasslyDao

    companion object {
        @Volatile
        private var INSTANCE: GlasslyDatabase? = null

        fun getInstance(context: Context): GlasslyDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    GlasslyDatabase::class.java,
                    "glassly_local.db"
                ).build().also { INSTANCE = it }
            }
        }
    }
}

object EditRecipeCodec {
    fun encode(state: PhotoEditState): String {
        val obj = JSONObject()
        obj.put("brightness", state.brightness.toDouble())
        obj.put("contrast", state.contrast.toDouble())
        obj.put("saturation", state.saturation.toDouble())
        obj.put("warmth", state.warmth.toDouble())
        obj.put("sharpness", state.sharpness.toDouble())
        obj.put("vignette", state.vignette.toDouble())
        obj.put("rotation", state.rotationDegrees)
        obj.put("straighten", state.straightenDegrees.toDouble())
        obj.put("flipH", state.flipHorizontal)
        obj.put("flipV", state.flipVertical)
        obj.put("cropRatio", state.cropRatio.name)
        obj.put("cropInset", state.cropInset.toDouble())
        obj.put("filter", state.filterPreset.name)
        obj.put("desc", state.imageDescription)

        val loupesArr = JSONArray()
        state.loupes.forEach { l ->
            val lo = JSONObject()
            lo.put("id", l.id)
            lo.put("cx", l.centerX.toDouble())
            lo.put("cy", l.centerY.toDouble())
            lo.put("rf", l.radiusFraction.toDouble())
            lo.put("zoom", l.zoomLevel.toDouble())
            lo.put("sc", l.strokeColorArgb ?: -1L)
            lo.put("sw", l.strokeWidthPt)
            loupesArr.put(lo)
        }
        obj.put("loupes", loupesArr)

        val textsArr = JSONArray()
        state.textOverlays.forEach { t ->
            val to = JSONObject()
            to.put("id", t.id)
            to.put("text", t.text)
            to.put("x", t.x.toDouble())
            to.put("y", t.y.toDouble())
            to.put("c", t.colorArgb)
            textsArr.put(to)
        }
        obj.put("texts", textsArr)

        val strokesArr = JSONArray()
        state.strokes.forEach { s ->
            val so = JSONObject()
            so.put("c", s.colorArgb)
            so.put("w", s.strokeWidthPx.toDouble())
            so.put("h", s.isHighlighter)
            val pts = JSONArray()
            s.points.forEach { (px, py) ->
                pts.put(JSONArray().apply {
                    put(px.toDouble())
                    put(py.toDouble())
                })
            }
            so.put("pts", pts)
            strokesArr.put(so)
        }
        obj.put("strokes", strokesArr)
        return obj.toString()
    }

    fun decode(json: String?): PhotoEditState {
        if (json.isNullOrBlank()) return PhotoEditState()
        return try {
            val obj = JSONObject(json)
            val loupes = buildList {
                val arr = obj.optJSONArray("loupes") ?: JSONArray()
                for (i in 0 until arr.length()) {
                    val lo = arr.getJSONObject(i)
                    val sc = lo.optLong("sc", -1L)
                    add(
                        LoupeAnnotation(
                            id = lo.optString("id", "loupe_$i"),
                            centerX = lo.optDouble("cx", 0.5).toFloat(),
                            centerY = lo.optDouble("cy", 0.5).toFloat(),
                            radiusFraction = lo.optDouble("rf", 0.22).toFloat(),
                            zoomLevel = lo.optDouble("zoom", 2.0).toFloat(),
                            strokeColorArgb = if (sc == -1L) null else sc,
                            strokeWidthPt = lo.optInt("sw", 3)
                        )
                    )
                }
            }
            val texts = buildList {
                val arr = obj.optJSONArray("texts") ?: JSONArray()
                for (i in 0 until arr.length()) {
                    val to = arr.getJSONObject(i)
                    add(
                        TextOverlay(
                            id = to.optString("id", "text_$i"),
                            text = to.optString("text", ""),
                            x = to.optDouble("x", 0.5).toFloat(),
                            y = to.optDouble("y", 0.5).toFloat(),
                            colorArgb = to.optLong("c", 0xFFFFFFFFL)
                        )
                    )
                }
            }
            val strokes = buildList {
                val arr = obj.optJSONArray("strokes") ?: JSONArray()
                for (i in 0 until arr.length()) {
                    val so = arr.getJSONObject(i)
                    val ptsArr = so.optJSONArray("pts") ?: JSONArray()
                    val pts = buildList {
                        for (j in 0 until ptsArr.length()) {
                            val pair = ptsArr.getJSONArray(j)
                            add(pair.getDouble(0).toFloat() to pair.getDouble(1).toFloat())
                        }
                    }
                    add(
                        DrawStroke(
                            points = pts,
                            colorArgb = so.optLong("c", 0xFFFFFFFFL),
                            strokeWidthPx = so.optDouble("w", 8.0).toFloat(),
                            isHighlighter = so.optBoolean("h", false)
                        )
                    )
                }
            }
            PhotoEditState(
                brightness = obj.optDouble("brightness", 0.0).toFloat(),
                contrast = obj.optDouble("contrast", 1.0).toFloat(),
                saturation = obj.optDouble("saturation", 1.0).toFloat(),
                warmth = obj.optDouble("warmth", 0.0).toFloat(),
                sharpness = obj.optDouble("sharpness", 0.0).toFloat(),
                vignette = obj.optDouble("vignette", 0.0).toFloat(),
                rotationDegrees = obj.optInt("rotation", 0),
                straightenDegrees = obj.optDouble("straighten", 0.0).toFloat(),
                flipHorizontal = obj.optBoolean("flipH", false),
                flipVertical = obj.optBoolean("flipV", false),
                cropRatio = runCatching {
                    CropAspectRatio.valueOf(obj.optString("cropRatio", "FREE"))
                }.getOrDefault(CropAspectRatio.FREE),
                cropInset = obj.optDouble("cropInset", 0.0).toFloat(),
                filterPreset = runCatching {
                    FilterPreset.valueOf(obj.optString("filter", "ORIGINAL"))
                }.getOrDefault(FilterPreset.ORIGINAL),
                strokes = strokes,
                textOverlays = texts,
                loupes = loupes,
                imageDescription = obj.optString("desc", "")
            )
        } catch (_: Throwable) {
            PhotoEditState()
        }
    }
}
