package com.kirakishou.photoexchange.helper.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.kirakishou.photoexchange.helper.database.entity.PhotoAdditionalInfoEntity

@Dao
abstract class PhotoAdditionalInfoDao {

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  abstract fun save(photoAdditionalInfoEntity: PhotoAdditionalInfoEntity): Long

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  abstract fun saveMany(photoAdditionalInfoEntityList: List<PhotoAdditionalInfoEntity>): Array<Long>

  @Query("SELECT * FROM ${PhotoAdditionalInfoEntity.TABLE_NAME} " +
    "WHERE ${PhotoAdditionalInfoEntity.PHOTO_NAME_COLUMN} = :photoName")
  abstract fun find(photoName: String): PhotoAdditionalInfoEntity?

  @Query("SELECT * FROM ${PhotoAdditionalInfoEntity.TABLE_NAME} " +
    "WHERE ${PhotoAdditionalInfoEntity.PHOTO_NAME_COLUMN} IN (:photoNameList)")
  abstract fun findMany(photoNameList: List<String>): List<PhotoAdditionalInfoEntity>

  @Query("UPDATE ${PhotoAdditionalInfoEntity.TABLE_NAME} " +
    "SET ${PhotoAdditionalInfoEntity.FAVOURITES_COUNT_COLUMN} = :favouritesCount " +
    "WHERE ${PhotoAdditionalInfoEntity.PHOTO_NAME_COLUMN} = :photoName")
  abstract fun updateFavouritesCount(photoName: String, favouritesCount: Long): Int
}