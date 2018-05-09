package com.kirakishou.photoexchange.helper.database.dao

import android.arch.persistence.room.Dao
import android.arch.persistence.room.Insert
import android.arch.persistence.room.Query
import android.arch.persistence.room.TypeConverters
import com.kirakishou.photoexchange.helper.database.MyDatabase
import com.kirakishou.photoexchange.helper.database.converter.PhotoStateConverter
import com.kirakishou.photoexchange.helper.database.entity.MyPhotoEntity
import com.kirakishou.photoexchange.mvp.model.PhotoState

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
        "WHERE ${MyPhotoEntity.PHOTO_STATE_COLUMN} = :photoState " +
        "ORDER BY ${MyPhotoEntity.TAKEN_ON_COLUMN} DESC")
    @TypeConverters(PhotoStateConverter::class)
    abstract fun findAllWithState(photoState: PhotoState): List<MyPhotoEntity>

    @Query("SELECT * FROM ${MyPhotoEntity.TABLE_NAME} " +
        "WHERE ${MyPhotoEntity.PHOTO_STATE_COLUMN} = :photoState " +
        "ORDER BY ${MyPhotoEntity.TAKEN_ON_COLUMN}")
    @TypeConverters(PhotoStateConverter::class)
    abstract fun findOnePhotoWithState(photoState: PhotoState): MutableList<MyPhotoEntity>

    @Query("SELECT * FROM ${MyPhotoEntity.TABLE_NAME} " +
            "WHERE " +
        "${MyPhotoEntity.ID_COLUMN} = :photoId " +
            " AND " +
        "${MyPhotoEntity.PHOTO_STATE_COLUMN} = :photoState")
    @TypeConverters(PhotoStateConverter::class)
    abstract fun findByIdAndState(photoId: Long, photoState: PhotoState): MyPhotoEntity?

    @Query("SELECT * FROM ${MyPhotoEntity.TABLE_NAME} " +
        "WHERE ${MyPhotoEntity.PHOTO_NAME_COLUMN} = :photoName")
    abstract fun findByName(photoName: String): MyPhotoEntity?

    @Query("SELECT ${MyPhotoEntity.ID_COLUMN} FROM ${MyPhotoEntity.TABLE_NAME} " +
        "WHERE ${MyPhotoEntity.PHOTO_NAME_COLUMN} = :photoName")
    abstract fun findPhotoIdByName(photoName: String): Long?

    @Query("SELECT * FROM ${MyPhotoEntity.TABLE_NAME}")
    abstract fun findAll(): List<MyPhotoEntity>

    @Query("SELECT COUNT(*) FROM ${MyPhotoEntity.TABLE_NAME} " +
        "WHERE ${MyPhotoEntity.PHOTO_STATE_COLUMN} = :photoState")
    @TypeConverters(PhotoStateConverter::class)
    abstract fun countAllByState(photoState: PhotoState): Long

    @Query("SELECT COUNT(*) FROM ${MyPhotoEntity.TABLE_NAME} " +
        "WHERE ${MyPhotoEntity.PHOTO_STATE_COLUMN} IN (:states)")
    @TypeConverters(PhotoStateConverter::class)
    abstract fun countAllByStates(states: Array<PhotoState>): Int

    @Query("UPDATE ${MyPhotoEntity.TABLE_NAME} " +
        "SET ${MyPhotoEntity.PHOTO_STATE_COLUMN} = :photoState " +
        "WHERE ${MyPhotoEntity.ID_COLUMN} = :id")
    @TypeConverters(PhotoStateConverter::class)
    abstract fun updateSetNewPhotoState(id: Long, photoState: PhotoState): Int

    @Query("UPDATE ${MyPhotoEntity.TABLE_NAME} " +
        "SET ${MyPhotoEntity.PHOTO_STATE_COLUMN} = :newState " +
        "WHERE ${MyPhotoEntity.PHOTO_STATE_COLUMN} = :oldState")
    @TypeConverters(PhotoStateConverter::class)
    abstract fun updateStates(oldState: PhotoState, newState: PhotoState): Int

    @Query("UPDATE ${MyPhotoEntity.TABLE_NAME} " +
        "SET ${MyPhotoEntity.TEMP_FILE_ID_COLUMN} = :newTempFileId " +
        "WHERE ${MyPhotoEntity.ID_COLUMN} = :photoId")
    abstract fun updateSetTempFileId(photoId: Long, newTempFileId: Long?): Int

    @Query("UPDATE ${MyPhotoEntity.TABLE_NAME} " +
        "SET ${MyPhotoEntity.PHOTO_NAME_COLUMN} = :photoName " +
        "WHERE ${MyPhotoEntity.ID_COLUMN} = :photoId")
    abstract fun updateSetPhotoName(photoId: Long, photoName: String): Int

    @Query("UPDATE ${MyPhotoEntity.TABLE_NAME} " +
        "SET ${MyPhotoEntity.IS_PUBLIC_COLUMN} = ${MyDatabase.SQLITE_TRUE} " +
        "WHERE ${MyPhotoEntity.ID_COLUMN} = :photoId")
    abstract fun updateSetPhotoPublic(photoId: Long): Int

    @Query("DELETE FROM ${MyPhotoEntity.TABLE_NAME} " +
        "WHERE ${MyPhotoEntity.ID_COLUMN} = :id")
    abstract fun deleteById(id: Long): Int
}