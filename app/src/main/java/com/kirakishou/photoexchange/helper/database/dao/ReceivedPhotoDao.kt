package com.kirakishou.photoexchange.helper.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.kirakishou.photoexchange.helper.database.entity.ReceivedPhotoEntity

@Dao
abstract class ReceivedPhotoDao {

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  abstract fun save(receivedPhotoEntity: ReceivedPhotoEntity): Long

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  abstract fun saveMany(receivedPhotoEntityList: List<ReceivedPhotoEntity>): Array<Long>

  @Query("SELECT * FROM ${ReceivedPhotoEntity.TABLE_NAME} " +
    "WHERE ${ReceivedPhotoEntity.UPLOADED_ON_COLUMN} < :time " +
    "ORDER BY ${ReceivedPhotoEntity.UPLOADED_ON_COLUMN} DESC " +
    "LIMIT :count")
  abstract fun getPage(time: Long, count: Int): List<ReceivedPhotoEntity>

  @Query("SELECT * FROM ${ReceivedPhotoEntity.TABLE_NAME}")
  abstract fun findAll(): List<ReceivedPhotoEntity>

  @Query("SELECT * FROM ${ReceivedPhotoEntity.TABLE_NAME} " +
    "WHERE ${ReceivedPhotoEntity.ID_COLUMN} IN (:receivedPhotoIdsList) " +
    "ORDER BY ${ReceivedPhotoEntity.ID_COLUMN} DESC")
  abstract fun findMany(receivedPhotoIdsList: List<Long>): List<ReceivedPhotoEntity>

  @Query("SELECT COUNT(*) FROM ${ReceivedPhotoEntity.TABLE_NAME}")
  abstract fun countAll(): Long

  @Query("DELETE FROM ${ReceivedPhotoEntity.TABLE_NAME} " +
    "WHERE ${ReceivedPhotoEntity.UPLOADED_ON_COLUMN} < :time")
  abstract fun deleteOlderThan(time: Long)

  @Query("DELETE FROM ${ReceivedPhotoEntity.TABLE_NAME}")
  abstract fun deleteAll()
}