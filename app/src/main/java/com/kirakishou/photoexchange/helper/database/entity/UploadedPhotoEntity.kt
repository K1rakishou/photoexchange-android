package com.kirakishou.photoexchange.helper.database.entity

import android.arch.persistence.room.ColumnInfo
import android.arch.persistence.room.Entity
import android.arch.persistence.room.PrimaryKey
import com.kirakishou.photoexchange.helper.database.entity.UploadedPhotoEntity.Companion.TABLE_NAME
import com.kirakishou.photoexchange.helper.util.TimeUtils

@Entity(tableName = TABLE_NAME)
class UploadedPhotoEntity(

    @PrimaryKey
    @ColumnInfo(name = PHOTO_NAME_COLUMN)
    var photoName: String = "",

    @ColumnInfo(name = REMOTE_PHOTO_ID_COLUMN, index = true)
    var remotePhotoId: Long? = null,

    @ColumnInfo(name = UPLOADER_LON_COLUMN)
    var uploaderLon: Double = 0.0,

    @ColumnInfo(name = UPLOADER_LAT_COLUMN)
    var uploaderLat: Double = 0.0,

    @ColumnInfo(name = HAS_RECEIVER_INFO_COLUMN)
    var hasReceiverInfo: Boolean = false,

    @ColumnInfo(name = UPLOADED_ON_COLUMN)
    var uploadedOn: Long? = null
) {

    fun isEmpty() : Boolean {
        return photoName.isEmpty()
    }

    companion object {

        fun empty(): UploadedPhotoEntity {
            return UploadedPhotoEntity()
        }

        fun create(photoName: String, photoId: Long, lon: Double, lat: Double, hasReceiverInfo: Boolean, uploadedOn: Long): UploadedPhotoEntity {
            return UploadedPhotoEntity(photoName, photoId, lon, lat, hasReceiverInfo, uploadedOn)
        }

        const val TABLE_NAME = "UPLOADED_PHOTO"

        const val REMOTE_PHOTO_ID_COLUMN = "REMOTE_PHOTO_ID"
        const val PHOTO_NAME_COLUMN = "PHOTO_NAME"
        const val UPLOADER_LON_COLUMN = "UPLOADER_LON"
        const val UPLOADER_LAT_COLUMN = "UPLOADER_LAT"
        const val HAS_RECEIVER_INFO_COLUMN = "HAS_RECEIVER_INFO"
        const val UPLOADED_ON_COLUMN = "UPLOADED_ON"
    }
}