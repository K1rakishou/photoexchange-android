package com.kirakishou.photoexchange.helper.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.kirakishou.photoexchange.helper.database.entity.GalleryPhotoEntity.Companion.TABLE_NAME

@Entity(tableName = TABLE_NAME)
class GalleryPhotoEntity(

  @PrimaryKey
  @ColumnInfo(name = PHOTO_NAME_COLUMN)
  var photoName: String = "",

  @ColumnInfo(name = LON_COLUMN)
  var lon: Double = -1.0,

  @ColumnInfo(name = LAT_COLUMN)
  var lat: Double = -1.0,

  @ColumnInfo(name = UPLOADED_ON_COLUMN)
  var uploadedOn: Long = 0L,

  @ColumnInfo(name = FAVOURITED_COUNT_COLUMN)
  var favouritedCount: Long = 0L,

  @ColumnInfo(name = INSERTED_ON_COLUMN, index = true)
  var insertedOn: Long = 0L
) {

  fun isEmpty(): Boolean {
    return photoName == ""
  }

  companion object {

    fun empty(): GalleryPhotoEntity {
      return GalleryPhotoEntity()
    }

    fun create(photoName: String, lon: Double, lat: Double, uploadedOn: Long,
               favouritedCount: Long, insertedOn: Long): GalleryPhotoEntity {
      return GalleryPhotoEntity(photoName, lon, lat, uploadedOn, favouritedCount, insertedOn)
    }

    const val TABLE_NAME = "GALLERY_PHOTOS"

    const val PHOTO_NAME_COLUMN = "PHOTO_NAME"
    const val LON_COLUMN = "LON"
    const val LAT_COLUMN = "LAT"
    const val UPLOADED_ON_COLUMN = "UPLOADED_ON"
    const val FAVOURITED_COUNT_COLUMN = "FAVOURITED_COUNT"
    const val INSERTED_ON_COLUMN = "INSERTED_ON"
  }
}