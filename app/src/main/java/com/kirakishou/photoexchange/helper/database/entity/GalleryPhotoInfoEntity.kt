package com.kirakishou.photoexchange.helper.database.entity

import android.arch.persistence.room.ColumnInfo
import android.arch.persistence.room.Entity
import android.arch.persistence.room.PrimaryKey
import com.kirakishou.photoexchange.helper.database.entity.GalleryPhotoInfoEntity.Companion.TABLE_NAME

@Entity(tableName = TABLE_NAME)
class GalleryPhotoInfoEntity(

    @PrimaryKey(autoGenerate = false)
    @ColumnInfo(name = GALLERY_PHOTO_ID_COLUMN, index = true)
    var galleryPhotoId: Long = 0L,

    @ColumnInfo(name = IS_FAVOURITED_COLUMN)
    var isFavourited: Boolean = false,

    @ColumnInfo(name = IS_REPORTED_COLUMN)
    var isReported: Boolean = false,

    @ColumnInfo(name = INSERTED_ON)
    var insertedOn: Long = 0L
) {

    fun isEmpty(): Boolean {
        return this.galleryPhotoId == -1L
    }

    companion object {

        fun empty(): GalleryPhotoInfoEntity {
            return GalleryPhotoInfoEntity(-1L)
        }

        fun create(id: Long, isFavourited: Boolean, isReported: Boolean, time: Long): GalleryPhotoInfoEntity {
            return GalleryPhotoInfoEntity(id, isFavourited, isReported, time)
        }

        const val TABLE_NAME = "GALLERY_PHOTO_INFO"

        const val GALLERY_PHOTO_ID_COLUMN = "GALLERY_PHOTO_ID"
        const val IS_FAVOURITED_COLUMN = "IS_FAVOURITED"
        const val IS_REPORTED_COLUMN = "IS_REPORTED"
        const val INSERTED_ON = "INSERTED_ON"
    }
}