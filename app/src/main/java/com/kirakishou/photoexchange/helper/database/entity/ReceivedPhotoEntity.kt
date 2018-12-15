package com.kirakishou.photoexchange.helper.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.kirakishou.photoexchange.helper.database.entity.ReceivedPhotoEntity.Companion.TABLE_NAME

@Entity(tableName = TABLE_NAME)
class ReceivedPhotoEntity(

  @PrimaryKey
  @ColumnInfo(name = UPLOADED_PHOTO_NAME_COLUMN)
  var uploadedPhotoName: String = "",

  @ColumnInfo(name = RECEIVED_PHOTO_NAME_COLUMN)
  var receivedPhotoName: String= "",

  @ColumnInfo(name = LON_COLUMN)
  var lon: Double = 0.0,

  @ColumnInfo(name = LAT_COLUMN)
  var lat: Double = 0.0,

  @ColumnInfo(name = UPLOADED_ON_COLUMN)
  var uploadedOn: Long = 0L,

  @ColumnInfo(name = INSERTED_ON_COLUMN)
  var insertedOn: Long = 0L
) {

  companion object {
    fun empty(): ReceivedPhotoEntity {
      return ReceivedPhotoEntity()
    }

    fun create(uploadedPhotoName: String, receivedPhotoName: String,
               lon: Double = 0.0, lat: Double = 0.0, uploadedOn: Long, insertedOn: Long): ReceivedPhotoEntity {
      return ReceivedPhotoEntity(uploadedPhotoName, receivedPhotoName, lon, lat, uploadedOn, insertedOn)
    }

    const val TABLE_NAME = "RECEIVED_PHOTO"

    const val UPLOADED_PHOTO_NAME_COLUMN = "UPLOADED_PHOTO_NAME"
    const val RECEIVED_PHOTO_NAME_COLUMN = "RECEIVED_PHOTO_NAME"
    const val LON_COLUMN = "LON"
    const val LAT_COLUMN = "LAT"
    const val UPLOADED_ON_COLUMN = "UPLOADED_ON"
    const val INSERTED_ON_COLUMN = "INSERTED_ON"
  }
}