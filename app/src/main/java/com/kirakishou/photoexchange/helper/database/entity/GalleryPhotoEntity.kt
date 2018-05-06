package com.kirakishou.photoexchange.helper.database.entity

import android.arch.persistence.room.ColumnInfo
import android.arch.persistence.room.Entity
import android.arch.persistence.room.PrimaryKey
import com.kirakishou.photoexchange.helper.database.entity.GalleryPhotoEntity.Companion.TABLE_NAME

@Entity(tableName = TABLE_NAME)
class GalleryPhotoEntity(

    @PrimaryKey(autoGenerate = false)
    @ColumnInfo(name = GALLERY_PHOTO_ID_COLUMN, index = true)
    var galleryPhotoId: Long = 0L,

    @ColumnInfo(name = PHOTO_NAME_COLUMN, index = true)
    var photoName: String = "",

    @ColumnInfo(name = LON_COLUMN)
    var lon: Double = -1.0,

    @ColumnInfo(name = LAT_COLUMN)
    var lat: Double = -1.0,

    @ColumnInfo(name = UPLOADED_ON_COLUMN)
    var uploadedOn: Long = 0L,

    @ColumnInfo(name = LAST_UPDATE_TIME_COLUMN, index = true)
    var lastUpdateTime: Long = 0L,

    @ColumnInfo(name = FAVOURITED_COUNT_COLUMN)
    var favouritedCount: Long = 0L,

    @ColumnInfo(name = IS_FAVOURITED_COLUMN)
    var isFavourited: Boolean = false,

    @ColumnInfo(name = IS_REPORTED_COLUMN)
    var isReported: Boolean = false
) {

    fun isEmpty(): Boolean {
        return this.galleryPhotoId == -1L
    }

    companion object {

        fun empty(): GalleryPhotoEntity {
            return GalleryPhotoEntity(-1L)
        }

        fun create(id: Long, photoName: String, lon: Double, lat: Double, uploadedOn: Long, lastUpdateTime: Long,
                   favouritedCount: Long, isFavourited: Boolean, isReported: Boolean): GalleryPhotoEntity {
            return GalleryPhotoEntity(id, photoName, lon, lat, uploadedOn, lastUpdateTime, favouritedCount, isFavourited, isReported)
        }

        const val TABLE_NAME = "gallery_photos"

        const val GALLERY_PHOTO_ID_COLUMN = "GALLERY_PHOTO_ID"
        const val PHOTO_NAME_COLUMN = "PHOTO_NAME"
        const val LON_COLUMN = "LON"
        const val LAT_COLUMN = "LAT"
        const val UPLOADED_ON_COLUMN = "UPLOADED_ON"
        const val LAST_UPDATE_TIME_COLUMN = "LAST_UPDATE_TIME"
        const val FAVOURITED_COUNT_COLUMN = "FAVOURITED_COUNT"
        const val IS_FAVOURITED_COLUMN = "IS_FAVOURITED"
        const val IS_REPORTED_COLUMN = "IS_REPORTED"
    }
}