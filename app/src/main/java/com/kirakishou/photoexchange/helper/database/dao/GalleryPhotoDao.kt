package com.kirakishou.photoexchange.helper.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.kirakishou.photoexchange.helper.database.entity.GalleryPhotoEntity

@Dao
abstract class GalleryPhotoDao {

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  abstract fun saveMany(galleryPhotos: List<GalleryPhotoEntity>): Array<Long>

  @Query("SELECT * FROM ${GalleryPhotoEntity.TABLE_NAME} " +
    "WHERE ${GalleryPhotoEntity.UPLOADED_ON_COLUMN} < :time " +
    "ORDER BY ${GalleryPhotoEntity.UPLOADED_ON_COLUMN} DESC " +
    "LIMIT :count")
  abstract fun getPage(time: Long, count: Int): List<GalleryPhotoEntity>

  @Query("SELECT * FROM ${GalleryPhotoEntity.TABLE_NAME} " +
    "WHERE ${GalleryPhotoEntity.PHOTO_NAME_COLUMN} = :photoName")
  abstract fun find(photoName: String): GalleryPhotoEntity?

  @Query("SELECT * FROM ${GalleryPhotoEntity.TABLE_NAME} " +
    "WHERE ${GalleryPhotoEntity.PHOTO_NAME_COLUMN} IN (:photoNameList) " +
    "ORDER BY ${GalleryPhotoEntity.UPLOADED_ON_COLUMN} DESC")
  abstract fun findMany(photoNameList: List<String>): List<GalleryPhotoEntity>

  @Query("UPDATE ${GalleryPhotoEntity.TABLE_NAME} " +
    "SET ${GalleryPhotoEntity.FAVOURITED_COUNT_COLUMN} = :favouritesCount " +
    "WHERE ${GalleryPhotoEntity.PHOTO_NAME_COLUMN} = :photoName")
  abstract fun updateFavouritesCount(photoName: String, favouritesCount: Long): Int

  @Query("DELETE FROM ${GalleryPhotoEntity.TABLE_NAME} " +
    "WHERE ${GalleryPhotoEntity.INSERTED_ON_COLUMN} < :time")
  abstract fun deleteOlderThan(time: Long)

  @Query("SELECT * FROM ${GalleryPhotoEntity.TABLE_NAME}")
  abstract fun findAll(): List<GalleryPhotoEntity>

  @Query("DELETE FROM ${GalleryPhotoEntity.TABLE_NAME}")
  abstract fun deleteAll()
}