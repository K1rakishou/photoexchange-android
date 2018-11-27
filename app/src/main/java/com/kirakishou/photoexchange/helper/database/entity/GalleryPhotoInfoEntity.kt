package com.kirakishou.photoexchange.helper.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.kirakishou.photoexchange.helper.database.entity.GalleryPhotoInfoEntity.Companion.TABLE_NAME

@Entity(tableName = TABLE_NAME)
class GalleryPhotoInfoEntity(

  @PrimaryKey
  @ColumnInfo(name = PHOTO_NAME_COLUMN)
  var photoName: String = "",

  @ColumnInfo(name = IS_FAVOURITED_COLUMN)
  var isFavourited: Boolean = false,

  @ColumnInfo(name = FAVOURITES_COUNT)
  var favouritesCount: Long = 0,

  @ColumnInfo(name = IS_REPORTED_COLUMN)
  var isReported: Boolean = false,

  @ColumnInfo(name = INSERTED_ON_COLUMN)
  var insertedOn: Long = 0L
) {

  fun isEmpty(): Boolean {
    return photoName == ""
  }

  companion object {

    fun empty(): GalleryPhotoInfoEntity {
      return GalleryPhotoInfoEntity("")
    }

    fun create(
      photoName: String,
      isFavourited: Boolean,
      favouritesCount: Long,
      isReported: Boolean,
      insertedOn: Long
    ): GalleryPhotoInfoEntity {
      return GalleryPhotoInfoEntity(
        photoName,
        isFavourited,
        favouritesCount,
        isReported,
        insertedOn
      )
    }

    const val TABLE_NAME = "GALLERY_PHOTO_INFO"

    const val PHOTO_NAME_COLUMN = "PHOTO_NAME"
    const val FAVOURITES_COUNT = "FAVOURITES_COUNT"
    const val IS_FAVOURITED_COLUMN = "IS_FAVOURITED"
    const val IS_REPORTED_COLUMN = "IS_REPORTED"
    const val INSERTED_ON_COLUMN = "INSERTED_ON"
  }
}