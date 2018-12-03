package com.kirakishou.photoexchange.helper.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.kirakishou.photoexchange.helper.database.entity.UploadedPhotoEntity

@Dao
abstract class UploadedPhotoDao {

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  abstract fun saveMany(uploadedPhotoEntityList: List<UploadedPhotoEntity>): Array<Long>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  abstract fun save(uploadedPhotoEntity: UploadedPhotoEntity): Long

  @Query("SELECT * FROM ${UploadedPhotoEntity.TABLE_NAME} " +
    "WHERE ${UploadedPhotoEntity.UPLOADED_ON_COLUMN} < :time " +
    "ORDER BY ${UploadedPhotoEntity.UPLOADED_ON_COLUMN} DESC " +
    "LIMIT :count")
  abstract fun getPage(time: Long, count: Int): List<UploadedPhotoEntity>

  @Query("SELECT * FROM ${UploadedPhotoEntity.TABLE_NAME} " +
    "WHERE ${UploadedPhotoEntity.PHOTO_NAME_COLUMN} = :photoName")
  abstract fun findByPhotoName(photoName: String): UploadedPhotoEntity?

  @Query("SELECT * FROM ${UploadedPhotoEntity.TABLE_NAME} " +
    "WHERE ${UploadedPhotoEntity.PHOTO_ID_COLUMN} IN (:photoIds) " +
    "ORDER BY ${UploadedPhotoEntity.PHOTO_ID_COLUMN} DESC")
  abstract fun findMany(photoIds: List<Long>): List<UploadedPhotoEntity>

  @Query("SELECT * FROM ${UploadedPhotoEntity.TABLE_NAME} " +
    "WHERE " +
    " ${UploadedPhotoEntity.RECEIVER_LON_COLUMN} IS NOT NULL " +
    "AND " +
    " ${UploadedPhotoEntity.RECEIVER_LAT_COLUMN} IS NOT NULL")
  abstract fun findAllWithReceiverInfo(): List<UploadedPhotoEntity>

  @Query("SELECT * FROM ${UploadedPhotoEntity.TABLE_NAME} " +
    "WHERE " +
    " ${UploadedPhotoEntity.RECEIVER_LON_COLUMN} IS NULL " +
    "AND " +
    " ${UploadedPhotoEntity.RECEIVER_LAT_COLUMN} IS NULL")
  abstract fun findAllWithoutReceiverInfo(): List<UploadedPhotoEntity>

  @Query("UPDATE ${UploadedPhotoEntity.TABLE_NAME} " +
    "SET " +
    " ${UploadedPhotoEntity.RECEIVER_LON_COLUMN} = :receiverLon, " +
    " ${UploadedPhotoEntity.RECEIVER_LAT_COLUMN} = :receiverLat, " +
    " ${UploadedPhotoEntity.RECEIVER_PHOTO_NAME_COLUMN} = :receivedPhotoName " +
    "WHERE ${UploadedPhotoEntity.PHOTO_NAME_COLUMN} = :uploadedPhotoName")
  abstract fun updateReceiverInfo(
    uploadedPhotoName: String,
    receivedPhotoName: String,
    receiverLon: Double,
    receiverLat: Double
  ): Int

  @Query("SELECT * FROM ${UploadedPhotoEntity.TABLE_NAME}")
  abstract fun findAll(): List<UploadedPhotoEntity>

  @Query("SELECT COUNT(*) FROM ${UploadedPhotoEntity.TABLE_NAME}")
  abstract fun count(): Long

  @Query("DELETE FROM ${UploadedPhotoEntity.TABLE_NAME} " +
    "WHERE ${UploadedPhotoEntity.INSERTED_ON_COLUMN} < :time")
  abstract fun deleteOlderThan(time: Long)

  @Query("DELETE FROM ${UploadedPhotoEntity.TABLE_NAME}")
  abstract fun deleteAll()
}