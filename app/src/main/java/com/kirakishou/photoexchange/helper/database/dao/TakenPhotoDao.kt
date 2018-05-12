package com.kirakishou.photoexchange.helper.database.dao

import android.arch.persistence.room.Dao
import android.arch.persistence.room.Insert
import android.arch.persistence.room.Query
import android.arch.persistence.room.TypeConverters
import com.kirakishou.photoexchange.helper.database.MyDatabase
import com.kirakishou.photoexchange.helper.database.converter.PhotoStateConverter
import com.kirakishou.photoexchange.helper.database.entity.TakenPhotoEntity
import com.kirakishou.photoexchange.mvp.model.PhotoState

/**
 * Created by kirakishou on 3/3/2018.
 */

@Dao
abstract class TakenPhotoDao {

    @Insert
    abstract fun insert(takenPhotoEntity: TakenPhotoEntity): Long

    @Query("SELECT * FROM ${TakenPhotoEntity.TABLE_NAME} " +
        "WHERE ${TakenPhotoEntity.ID_COLUMN} = :id")
    abstract fun findById(id: Long): TakenPhotoEntity?

    @Query("SELECT * FROM ${TakenPhotoEntity.TABLE_NAME} " +
        "WHERE ${TakenPhotoEntity.PHOTO_STATE_COLUMN} = :photoState " +
        "ORDER BY ${TakenPhotoEntity.TAKEN_ON_COLUMN} DESC")
    @TypeConverters(PhotoStateConverter::class)
    abstract fun findAllWithState(photoState: PhotoState): List<TakenPhotoEntity>

    @Query("SELECT * FROM ${TakenPhotoEntity.TABLE_NAME} " +
        "WHERE ${TakenPhotoEntity.PHOTO_STATE_COLUMN} = :photoState " +
        "ORDER BY ${TakenPhotoEntity.TAKEN_ON_COLUMN}")
    @TypeConverters(PhotoStateConverter::class)
    abstract fun findOnePhotoWithState(photoState: PhotoState): MutableList<TakenPhotoEntity>

    @Query("SELECT * FROM ${TakenPhotoEntity.TABLE_NAME} " +
            "WHERE " +
        "${TakenPhotoEntity.ID_COLUMN} = :photoId " +
            " AND " +
        "${TakenPhotoEntity.PHOTO_STATE_COLUMN} = :photoState")
    @TypeConverters(PhotoStateConverter::class)
    abstract fun findByIdAndState(photoId: Long, photoState: PhotoState): TakenPhotoEntity?

    @Query("SELECT * FROM ${TakenPhotoEntity.TABLE_NAME} " +
        "WHERE ${TakenPhotoEntity.PHOTO_NAME_COLUMN} = :photoName")
    abstract fun findByName(photoName: String): TakenPhotoEntity?

    @Query("SELECT ${TakenPhotoEntity.ID_COLUMN} FROM ${TakenPhotoEntity.TABLE_NAME} " +
        "WHERE ${TakenPhotoEntity.PHOTO_NAME_COLUMN} = :photoName")
    abstract fun findPhotoIdByName(photoName: String): Long?

    @Query("SELECT * FROM ${TakenPhotoEntity.TABLE_NAME}")
    abstract fun findAll(): List<TakenPhotoEntity>

    @Query("SELECT COUNT(*) FROM ${TakenPhotoEntity.TABLE_NAME} " +
        "WHERE ${TakenPhotoEntity.PHOTO_STATE_COLUMN} = :photoState")
    @TypeConverters(PhotoStateConverter::class)
    abstract fun countAllByState(photoState: PhotoState): Long

    @Query("SELECT COUNT(*) FROM ${TakenPhotoEntity.TABLE_NAME} " +
        "WHERE ${TakenPhotoEntity.PHOTO_STATE_COLUMN} IN (:states)")
    @TypeConverters(PhotoStateConverter::class)
    abstract fun countAllByStates(states: Array<PhotoState>): Int

    @Query("UPDATE ${TakenPhotoEntity.TABLE_NAME} " +
        "SET ${TakenPhotoEntity.PHOTO_STATE_COLUMN} = :photoState " +
        "WHERE ${TakenPhotoEntity.ID_COLUMN} = :id")
    @TypeConverters(PhotoStateConverter::class)
    abstract fun updateSetNewPhotoState(id: Long, photoState: PhotoState): Int

    @Query("UPDATE ${TakenPhotoEntity.TABLE_NAME} " +
        "SET ${TakenPhotoEntity.PHOTO_STATE_COLUMN} = :newState " +
        "WHERE ${TakenPhotoEntity.PHOTO_STATE_COLUMN} = :oldState")
    @TypeConverters(PhotoStateConverter::class)
    abstract fun updateStates(oldState: PhotoState, newState: PhotoState): Int

    @Query("UPDATE ${TakenPhotoEntity.TABLE_NAME} " +
        "SET ${TakenPhotoEntity.TEMP_FILE_ID_COLUMN} = :newTempFileId " +
        "WHERE ${TakenPhotoEntity.ID_COLUMN} = :photoId")
    abstract fun updateSetTempFileId(photoId: Long, newTempFileId: Long?): Int

    @Query("UPDATE ${TakenPhotoEntity.TABLE_NAME} " +
        "SET ${TakenPhotoEntity.PHOTO_NAME_COLUMN} = :photoName " +
        "WHERE ${TakenPhotoEntity.ID_COLUMN} = :photoId")
    abstract fun updateSetPhotoName(photoId: Long, photoName: String): Int

    @Query("UPDATE ${TakenPhotoEntity.TABLE_NAME} " +
        "SET ${TakenPhotoEntity.IS_PUBLIC_COLUMN} = ${MyDatabase.SQLITE_TRUE} " +
        "WHERE ${TakenPhotoEntity.ID_COLUMN} = :photoId")
    abstract fun updateSetPhotoPublic(photoId: Long): Int

    @Query("DELETE FROM ${TakenPhotoEntity.TABLE_NAME} " +
        "WHERE ${TakenPhotoEntity.ID_COLUMN} = :id")
    abstract fun deleteById(id: Long): Int
}