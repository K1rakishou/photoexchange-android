package com.kirakishou.photoexchange.helper.database.dao

import android.arch.persistence.room.Dao
import android.arch.persistence.room.Insert
import android.arch.persistence.room.Query
import com.kirakishou.photoexchange.helper.database.entity.ReceivedPhotoEntity

@Dao
abstract class ReceivedPhotoDao {

    @Insert
    abstract fun insert(receivedPhotoEntity: ReceivedPhotoEntity): Long

    @Query("SELECT COUNT(*) FROM ${ReceivedPhotoEntity.TABLE_NAME}")
    abstract fun countAll(): Long

    @Query("SELECT * FROM ${ReceivedPhotoEntity.TABLE_NAME}")
    abstract fun findAll(): List<ReceivedPhotoEntity>
}