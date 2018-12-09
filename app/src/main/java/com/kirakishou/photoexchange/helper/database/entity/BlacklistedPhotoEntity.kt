package com.kirakishou.photoexchange.helper.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.kirakishou.photoexchange.helper.database.entity.BlacklistedPhotoEntity.Companion.TABLE_NAME

@Entity(tableName = TABLE_NAME)
class BlacklistedPhotoEntity(

  @PrimaryKey
  @ColumnInfo(name = PHOTO_NAME_COLUMN)
  var photoName: String = "",

  @ColumnInfo(name = BLACKLISTED_ON_COLUMN)
  var blacklistedOn: Long = 0L

) {

  fun isEmpty(): Boolean {
    return photoName == ""
  }

  companion object {

    fun empty(): BlacklistedPhotoEntity {
      return BlacklistedPhotoEntity()
    }

    fun create(photoName: String, time: Long): BlacklistedPhotoEntity {
      return BlacklistedPhotoEntity(photoName, time)
    }

    const val TABLE_NAME = "BLACKLISTED_PHOTOS"

    const val PHOTO_NAME_COLUMN = "PHOTO_NAME"
    const val BLACKLISTED_ON_COLUMN = "BLACKLISTED_ON"
  }
}