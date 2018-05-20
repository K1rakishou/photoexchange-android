package com.kirakishou.photoexchange.helper.database.entity

import android.arch.persistence.room.ColumnInfo
import android.arch.persistence.room.Entity
import android.arch.persistence.room.PrimaryKey
import com.kirakishou.photoexchange.helper.database.entity.ReceivedPhotoEntity.Companion.TABLE_NAME

@Entity(tableName = TABLE_NAME)
class ReceivedPhotoEntity(

    @PrimaryKey
    @ColumnInfo(name = ID_COLUMN)
    var id: Long? = null,

    @ColumnInfo(name = UPLOADED_PHOTO_NAME_COLUMN)
    var uploadedPhotoName: String? = null,

    @ColumnInfo(name = RECEIVED_PHOTO_NAME_COLUMN)
    var receivedPhotoName: String? = null,

    @ColumnInfo(name = LON_COLUMN)
    var lon: Double? = null,

    @ColumnInfo(name = LAT_COLUMN)
    var lat: Double? = null
) {

    fun isEmpty(): Boolean {
        return id == null
    }

    companion object {

        fun empty(): ReceivedPhotoEntity {
            return ReceivedPhotoEntity()
        }

        fun create(photoId: Long, uploadedPhotoName: String,  receivedPhotoName: String, lon: Double = 0.0, lat: Double = 0.0): ReceivedPhotoEntity {
            return ReceivedPhotoEntity(photoId, uploadedPhotoName, receivedPhotoName, lon, lat)
        }

        const val TABLE_NAME = "RECEIVED_PHOTO"

        const val ID_COLUMN = "ID"
        const val UPLOADED_PHOTO_NAME_COLUMN = "UPLOADED_PHOTO_NAME"
        const val RECEIVED_PHOTO_NAME_COLUMN = "RECEIVED_PHOTO_NAME"
        const val LON_COLUMN = "LON"
        const val LAT_COLUMN = "LAT"
    }
}