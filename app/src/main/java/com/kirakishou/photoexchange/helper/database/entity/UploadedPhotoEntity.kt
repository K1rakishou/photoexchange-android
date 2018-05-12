package com.kirakishou.photoexchange.helper.database.entity

import android.arch.persistence.room.ColumnInfo
import android.arch.persistence.room.Entity
import android.arch.persistence.room.PrimaryKey
import com.kirakishou.photoexchange.helper.database.entity.UploadedPhotoEntity.Companion.TABLE_NAME
import com.kirakishou.photoexchange.helper.util.TimeUtils

@Entity(tableName = TABLE_NAME)
class UploadedPhotoEntity(

    @PrimaryKey(autoGenerate = false)
    @ColumnInfo(name = PHOTO_ID_COLUMN)
    var photoId: Long? = null,

    @ColumnInfo(name = PHOTO_NAME_COLUMN, index = true)
    var photoName: String? = null,

    @ColumnInfo(name = LON_COLUMN)
    var lon: Double = 0.0,

    @ColumnInfo(name = LAT_COLUMN)
    var lat: Double = 0.0,

    @ColumnInfo(name = UPLOADED_ON_COLUMN)
    var uploadedOn: Long? = null
) {

    fun isEmpty() : Boolean {
        return photoId == null
    }

    companion object {

        fun empty(): UploadedPhotoEntity {
            return UploadedPhotoEntity()
        }

        fun create(photoId: Long, photoName: String): UploadedPhotoEntity {
            return UploadedPhotoEntity(photoId, photoName, 0.0, 0.0, TimeUtils.getTimeFast())
        }

        const val TABLE_NAME = "UPLOADED_PHOTO"

        const val PHOTO_ID_COLUMN = "PHOTO_ID"
        const val PHOTO_NAME_COLUMN = "PHOTO_NAME"
        const val LON_COLUMN = "LON"
        const val LAT_COLUMN = "LAT"
        const val UPLOADED_ON_COLUMN = "UPLOADED_ON"
    }
}