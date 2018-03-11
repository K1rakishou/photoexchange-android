package com.kirakishou.photoexchange.helper.database.dao

import android.arch.persistence.room.Dao
import android.arch.persistence.room.Insert
import android.arch.persistence.room.Query
import android.arch.persistence.room.TypeConverters
import com.kirakishou.photoexchange.helper.database.converter.PhotoStateConverter
import com.kirakishou.photoexchange.helper.database.entity.MyPhotoEntity
import com.kirakishou.photoexchange.mvp.model.state.PhotoState

/**
 * Created by kirakishou on 3/3/2018.
 */

@Dao
abstract class MyPhotoDao {

    @Insert
    abstract fun insert(myPhotoEntity: MyPhotoEntity): Long

    @Query("SELECT * FROM ${MyPhotoEntity.TABLE_NAME} " +
        "WHERE ${MyPhotoEntity.ID_COLUMN} = :id")
    abstract fun findById(id: Long): MyPhotoEntity?

    @Query("SELECT * FROM ${MyPhotoEntity.TABLE_NAME} " +
        "WHERE ${MyPhotoEntity.PHOTO_STATE_COLUMN} = :photoState")
    @TypeConverters(PhotoStateConverter::class)
    abstract fun findAllWithState(photoState: PhotoState): List<MyPhotoEntity>

    @Query("SELECT * FROM ${MyPhotoEntity.TABLE_NAME}")
    abstract fun findAll(): List<MyPhotoEntity>

    @Query("UPDATE ${MyPhotoEntity.TABLE_NAME} " +
        "SET ${MyPhotoEntity.PHOTO_STATE_COLUMN} = :photoState " +
        "WHERE ${MyPhotoEntity.ID_COLUMN} = :id")
    @TypeConverters(PhotoStateConverter::class)
    abstract fun updateSetNewPhotoState(id: Long, photoState: PhotoState): Int

    @Query("DELETE FROM ${MyPhotoEntity.TABLE_NAME} " +
        "WHERE ${MyPhotoEntity.ID_COLUMN} = :id")
    abstract fun deleteById(id: Long): Int
}