package com.kirakishou.photoexchange.helper.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.kirakishou.photoexchange.helper.database.entity.BlacklistedPhotoEntity

@Dao
abstract class BlacklistedPhotoDao {

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  abstract fun save(blacklistedPhotoEntity: BlacklistedPhotoEntity): Long

  @Query("SELECT * FROM ${BlacklistedPhotoEntity.TABLE_NAME} " +
    "WHERE ${BlacklistedPhotoEntity.PHOTO_NAME_COLUMN} = :photoName")
  abstract fun find(photoName: String): BlacklistedPhotoEntity?
}