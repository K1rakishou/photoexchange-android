package com.kirakishou.photoexchange.helper.database.dao

import android.arch.persistence.room.Dao
import android.arch.persistence.room.Insert
import android.arch.persistence.room.OnConflictStrategy
import android.arch.persistence.room.Query
import com.kirakishou.photoexchange.helper.database.MyDatabase
import com.kirakishou.photoexchange.helper.database.entity.UploadedPhotoEntity

@Dao
abstract class UploadedPhotoDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract fun saveMany(uploadedPhotoEntityList: List<UploadedPhotoEntity>): Array<Long>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract fun save(uploadedPhotoEntity: UploadedPhotoEntity): Long

    @Query("SELECT * FROM ${UploadedPhotoEntity.TABLE_NAME} " +
        "WHERE ${UploadedPhotoEntity.PHOTO_NAME_COLUMN} = :photoName")
    abstract fun findByPhotoName(photoName: String): UploadedPhotoEntity?

    @Query("SELECT * FROM ${UploadedPhotoEntity.TABLE_NAME} " +
        "WHERE ${UploadedPhotoEntity.PHOTO_ID_COLUMN} IN (:photoIds) " +
        "ORDER BY ${UploadedPhotoEntity.PHOTO_ID_COLUMN} DESC")
    abstract fun findMany(photoIds: List<Long>): List<UploadedPhotoEntity>

    @Query("SELECT * FROM ${UploadedPhotoEntity.TABLE_NAME} " +
        "WHERE ${UploadedPhotoEntity.HAS_RECEIVER_INFO_COLUMN} = ${MyDatabase.SQLITE_TRUE}")
    abstract fun findAllWithReceiverInfo(): List<UploadedPhotoEntity>

    @Query("SELECT * FROM ${UploadedPhotoEntity.TABLE_NAME} " +
        "WHERE ${UploadedPhotoEntity.HAS_RECEIVER_INFO_COLUMN} = ${MyDatabase.SQLITE_FALSE}")
    abstract fun findAllWithoutReceiverInfo(): List<UploadedPhotoEntity>

    @Query("UPDATE ${UploadedPhotoEntity.TABLE_NAME} " +
        "SET ${UploadedPhotoEntity.HAS_RECEIVER_INFO_COLUMN} = ${MyDatabase.SQLITE_TRUE} " +
        "WHERE ${UploadedPhotoEntity.PHOTO_NAME_COLUMN} = :uploadedPhotoName")
    abstract fun updateReceiverInfo(uploadedPhotoName: String): Int

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