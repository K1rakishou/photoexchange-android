package com.kirakishou.photoexchange.helper.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.kirakishou.photoexchange.helper.database.entity.GalleryPhotoInfoEntity

@Dao
abstract class GalleryPhotoInfoDao {

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  abstract fun save(galleryPhotoInfoEntity: GalleryPhotoInfoEntity): Long

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  abstract fun saveMany(galleryPhotoInfoEntityList: List<GalleryPhotoInfoEntity>): Array<Long>

  @Query("SELECT * FROM ${GalleryPhotoInfoEntity.TABLE_NAME} " +
    "WHERE ${GalleryPhotoInfoEntity.GALLERY_PHOTO_ID_COLUMN} = :galleryPhotoId")
  abstract fun find(galleryPhotoId: Long): GalleryPhotoInfoEntity?

  @Query("SELECT * FROM ${GalleryPhotoInfoEntity.TABLE_NAME} " +
    "WHERE " +
    "${GalleryPhotoInfoEntity.GALLERY_PHOTO_ID_COLUMN} IN (:galleryPhotoIds) " +
    "ORDER BY ${GalleryPhotoInfoEntity.GALLERY_PHOTO_ID_COLUMN} DESC")
  abstract fun findMany(galleryPhotoIds: List<Long>): List<GalleryPhotoInfoEntity>

  @Query("SELECT * FROM ${GalleryPhotoInfoEntity.TABLE_NAME}")
  abstract fun findAll(): List<GalleryPhotoInfoEntity>

  @Query("DELETE FROM ${GalleryPhotoInfoEntity.TABLE_NAME} " +
    "WHERE ${GalleryPhotoInfoEntity.INSERTED_ON_COLUMN} < :time")
  abstract fun deleteOlderThan(time: Long)
}