package com.kirakishou.photoexchange.helper.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.kirakishou.photoexchange.helper.database.converter.PhotoStateConverter
import com.kirakishou.photoexchange.helper.database.entity.TakenPhotoEntity.Companion.TABLE_NAME
import com.kirakishou.photoexchange.mvrx.model.PhotoState

/**
 * Created by kirakishou on 3/3/2018.
 */

@Entity(tableName = TABLE_NAME)
class TakenPhotoEntity(

  @PrimaryKey(autoGenerate = true)
  @ColumnInfo(name = ID_COLUMN)
  var id: Long? = null,

  @ColumnInfo(name = TEMP_FILE_ID_COLUMN)
  var tempFileId: Long? = null,

  @ColumnInfo(name = IS_PUBLIC_COLUMN)
  var isPublic: Boolean = false,

  @ColumnInfo(name = PHOTO_NAME_COLUMN, index = true)
  var photoName: String? = null,

  @ColumnInfo(name = LON_COLUMN)
  var lon: Double = 0.0,

  @ColumnInfo(name = LAT_COLUMN)
  var lat: Double = 0.0,

  @ColumnInfo(name = PHOTO_STATE_COLUMN)
  @field:TypeConverters(PhotoStateConverter::class)
  var photoState: PhotoState = PhotoState.PHOTO_TAKEN,

  @ColumnInfo(name = TAKEN_ON_COLUMN, index = true)
  var takenOn: Long? = null

) {

  fun isEmpty(): Boolean {
    return id == null
  }

  companion object {

    fun empty(): TakenPhotoEntity {
      return TakenPhotoEntity()
    }

    fun create(tempFileId: Long?, isPublic: Boolean, time: Long): TakenPhotoEntity {
      return TakenPhotoEntity(null, tempFileId, isPublic, null, 0.0, 0.0, PhotoState.PHOTO_TAKEN, time)
    }

    const val TABLE_NAME = "TAKEN_PHOTO"

    const val ID_COLUMN = "ID"
    const val TEMP_FILE_ID_COLUMN = "TEMP_FILE_ID"
    const val IS_PUBLIC_COLUMN = "IS_PUBLIC"
    const val PHOTO_NAME_COLUMN = "PHOTO_NAME"
    const val LON_COLUMN = "LON"
    const val LAT_COLUMN = "LAT"
    const val PHOTO_STATE_COLUMN = "PHOTO_STATE"
    const val TAKEN_ON_COLUMN = "TAKEN_ON"
  }
}