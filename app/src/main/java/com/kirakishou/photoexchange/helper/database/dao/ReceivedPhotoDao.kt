package com.kirakishou.photoexchange.helper.database.dao

import android.arch.persistence.room.Dao
import android.arch.persistence.room.Insert
import android.arch.persistence.room.OnConflictStrategy
import android.arch.persistence.room.Query
import com.kirakishou.photoexchange.helper.database.entity.ReceivedPhotoEntity

@Dao
abstract class ReceivedPhotoDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract fun save(receivedPhotoEntity: ReceivedPhotoEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract fun saveMany(receivedPhotoEntityList: List<ReceivedPhotoEntity>): Array<Long>

    @Query("SELECT * FROM ${ReceivedPhotoEntity.TABLE_NAME}")
    abstract fun findAll(): List<ReceivedPhotoEntity>

    @Query("SELECT * FROM ${ReceivedPhotoEntity.TABLE_NAME} " +
        "WHERE ${ReceivedPhotoEntity.ID_COLUMN} IN (:receivedPhotoIdsList)")
    abstract fun findMany(receivedPhotoIdsList: List<Long>): List<ReceivedPhotoEntity>

    @Query("SELECT COUNT(*) FROM ${ReceivedPhotoEntity.TABLE_NAME}")
    abstract fun countAll(): Long

    @Query("DELETE FROM ${ReceivedPhotoEntity.TABLE_NAME} " +
        "WHERE ${ReceivedPhotoEntity.INSERTED_ON_COLUMN} < :time")
    abstract fun deleteOlderThan(time: Long)

    @Query("DELETE FROM ${ReceivedPhotoEntity.TABLE_NAME}")
    abstract fun deleteAll()
}