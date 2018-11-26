package com.kirakishou.photoexchange.helper.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.kirakishou.photoexchange.helper.database.entity.UploadedPhotoEntity.Companion.TABLE_NAME
import com.kirakishou.photoexchange.mvp.model.photo.UploadedPhoto

@Entity(tableName = TABLE_NAME)
class UploadedPhotoEntity(

  @PrimaryKey
  @ColumnInfo(name = PHOTO_NAME_COLUMN)
  var photoName: String = "",

  @ColumnInfo(name = PHOTO_ID_COLUMN, index = true)
  var photoId: Long? = null,

  @ColumnInfo(name = UPLOADER_LON_COLUMN)
  var uploaderLon: Double = 0.0,

  @ColumnInfo(name = UPLOADER_LAT_COLUMN)
  var uploaderLat: Double = 0.0,

  @ColumnInfo(name = RECEIVER_LON_COLUMN)
  var receiverLon: Double? = null,

  @ColumnInfo(name = RECEIVER_LAT_COLUMN)
  var receiverLat: Double? = null,

  @ColumnInfo(name = UPLOADED_ON_COLUMN)
  var uploadedOn: Long? = null,

  @ColumnInfo(name = INSERTED_ON_COLUMN)
  var insertedOn: Long? = null
) {

  fun isEmpty(): Boolean {
    return photoName.isEmpty()
  }

  companion object {

    fun empty(): UploadedPhotoEntity {
      return UploadedPhotoEntity()
    }

    fun createWithoutReceiverInfo(photoName: String,
                                  takenPhotoId: Long,
                                  lon: Double,
                                  lat: Double,
                                  uploadedOn: Long,
                                  insertedOn: Long
    ): UploadedPhotoEntity {
      return UploadedPhotoEntity(
        photoName,
        takenPhotoId,
        lon,
        lat,
        null,
        null,
        uploadedOn,
        insertedOn
      )
    }

    fun create(photoName: String,
               takenPhotoId: Long,
               lon: Double,
               lat: Double,
               receiverLon: Double?,
               receiverLat: Double?,
               uploadedOn: Long,
               insertedOn: Long
    ): UploadedPhotoEntity {
      return UploadedPhotoEntity(
        photoName,
        takenPhotoId,
        lon,
        lat,
        receiverLon,
        receiverLat,
        uploadedOn,
        insertedOn
      )
    }

    const val TABLE_NAME = "UPLOADED_PHOTO"

    const val PHOTO_ID_COLUMN = "PHOTO_ID"
    const val PHOTO_NAME_COLUMN = "PHOTO_NAME"
    const val UPLOADER_LON_COLUMN = "UPLOADER_LON"
    const val UPLOADER_LAT_COLUMN = "UPLOADER_LAT"
    const val RECEIVER_LON_COLUMN = "RECEIVER_LON"
    const val RECEIVER_LAT_COLUMN = "RECEIVER_LAT"
    const val UPLOADED_ON_COLUMN = "UPLOADED_ON"
    const val INSERTED_ON_COLUMN = "INSERTED_ON"
  }
}