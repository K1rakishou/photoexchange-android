package com.kirakishou.photoexchange.helper.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.kirakishou.photoexchange.helper.database.entity.PhotoAdditionalInfoEntity.Companion.TABLE_NAME

@Entity(tableName = TABLE_NAME)
class PhotoAdditionalInfoEntity(

  @PrimaryKey
  @ColumnInfo(name = PHOTO_NAME_COLUMN)
  var photoName: String = "",

  @ColumnInfo(name = IS_FAVOURITED_COLUMN)
  var isFavourited: Boolean = false,

  @ColumnInfo(name = FAVOURITES_COUNT_COLUMN)
  var favouritesCount: Long = 0L,

  @ColumnInfo(name = IS_REPORTED_COLUMN)
  var isReported: Boolean = false,

  @ColumnInfo(name = INSERTED_ON_COLUMN)
  var insertedOn: Long = 0L
) {

  companion object {
    const val TABLE_NAME = "PHOTO_ADDITIONAL_INFO"

    const val PHOTO_NAME_COLUMN = "PHOTO_NAME"
    const val IS_FAVOURITED_COLUMN = "IS_FAVOURITED"
    const val FAVOURITES_COUNT_COLUMN = "FAVOURITES_COUNT"
    const val IS_REPORTED_COLUMN = "IS_REPORTED"
    const val INSERTED_ON_COLUMN = "INSERTED_ON"
  }
}