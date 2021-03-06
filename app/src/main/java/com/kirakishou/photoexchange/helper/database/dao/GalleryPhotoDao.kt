package com.kirakishou.photoexchange.helper.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.kirakishou.photoexchange.helper.database.entity.GalleryPhotoEntity

@Dao
abstract class GalleryPhotoDao {

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  abstract fun save(galleryPhoto: GalleryPhotoEntity): Long

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  abstract fun saveMany(galleryPhotos: List<GalleryPhotoEntity>): Array<Long>

  @Query(
    "SELECT * FROM ${GalleryPhotoEntity.TABLE_NAME} " +
      "WHERE " +
      " ${GalleryPhotoEntity.UPLOADED_ON_COLUMN} < :lastUploadedOn " +
      "AND " +
      " ${GalleryPhotoEntity.INSERTED_ON_COLUMN} >= :deletionTime " +
      "ORDER BY ${GalleryPhotoEntity.UPLOADED_ON_COLUMN} DESC " +
      "LIMIT :count"
  )
  abstract fun getPage(lastUploadedOn: Long, deletionTime: Long, count: Int): List<GalleryPhotoEntity>

  @Query(
    "SELECT * FROM ${GalleryPhotoEntity.TABLE_NAME} " +
      "WHERE ${GalleryPhotoEntity.PHOTO_NAME_COLUMN} = :photoName"
  )
  abstract fun find(photoName: String): GalleryPhotoEntity?

  @Query(
    "SELECT * FROM ${GalleryPhotoEntity.TABLE_NAME} " +
      "WHERE ${GalleryPhotoEntity.PHOTO_NAME_COLUMN} IN (:photoNameList) " +
      "ORDER BY ${GalleryPhotoEntity.UPLOADED_ON_COLUMN} DESC"
  )
  abstract fun findMany(photoNameList: List<String>): List<GalleryPhotoEntity>

  @Query("SELECT * FROM ${GalleryPhotoEntity.TABLE_NAME}")
  abstract fun findAll(): List<GalleryPhotoEntity>

  @Query(
    "DELETE FROM ${GalleryPhotoEntity.TABLE_NAME} " +
      "WHERE ${GalleryPhotoEntity.PHOTO_NAME_COLUMN} = :photoName"
  )
  abstract fun deleteByPhotoName(photoName: String)

  @Query(
    "DELETE FROM ${GalleryPhotoEntity.TABLE_NAME} " +
      "WHERE ${GalleryPhotoEntity.INSERTED_ON_COLUMN} < :time"
  )
  abstract fun deleteOlderThan(time: Long)

  @Query("DELETE FROM ${GalleryPhotoEntity.TABLE_NAME}")
  abstract fun deleteAll()
}