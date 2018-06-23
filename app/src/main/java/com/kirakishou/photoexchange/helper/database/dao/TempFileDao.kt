package com.kirakishou.photoexchange.helper.database.dao

import android.arch.persistence.room.Dao
import android.arch.persistence.room.Insert
import android.arch.persistence.room.Query
import com.kirakishou.photoexchange.helper.database.entity.TempFileEntity

/**
 * Created by kirakishou on 3/3/2018.
 */

@Dao
abstract class TempFileDao {

    @Insert
    abstract fun insert(tempFileEntity: TempFileEntity): Long

    @Query("SELECT * FROM ${TempFileEntity.TABLE_NAME} " +
        "WHERE " +
        " ${TempFileEntity.ID_COLUMN} = :id " +
        "AND " +
        " ${TempFileEntity.DELETED_ON_COLUMN} = 0")
    abstract fun findById(id: Long): TempFileEntity?

    @Query("SELECT * FROM ${TempFileEntity.TABLE_NAME}")
    abstract fun findAll(): List<TempFileEntity>

    @Query("SELECT * FROM ${TempFileEntity.TABLE_NAME} " +
        "WHERE " +
        "${TempFileEntity.DELETED_ON_COLUMN} > 0 " +
        " AND " +
        "${TempFileEntity.DELETED_ON_COLUMN} < :time")
    abstract fun findDeletedOld(time: Long): List<TempFileEntity>

    @Query("SELECT * FROM ${TempFileEntity.TABLE_NAME} " +
        "WHERE ${TempFileEntity.FILE_PATH_COLUMN} = :filePath")
    abstract fun findByFilePath(filePath: String): TempFileEntity?

    @Query("SELECT * FROM ${TempFileEntity.TABLE_NAME} " +
        "WHERE ${TempFileEntity.TAKEN_PHOTO_ID_COLUMN} = ${TempFileEntity.DEFAULT_TAKEN_PHOTO_ID}")
    abstract fun findAllEmpty(): List<TempFileEntity>

    @Query("SELECT * FROM ${TempFileEntity.TABLE_NAME} " +
        "WHERE ${TempFileEntity.DELETED_ON_COLUMN} > 0 " +
        "ORDER BY ${TempFileEntity.DELETED_ON_COLUMN} ASC LIMIT :count")
    abstract fun findOldest(count: Int): List<TempFileEntity>

    @Query("UPDATE ${TempFileEntity.TABLE_NAME} " +
        "SET ${TempFileEntity.DELETED_ON_COLUMN} = :time " +
        "WHERE ${TempFileEntity.FILE_PATH_COLUMN} = :path")
    abstract fun markDeletedByFilePath(path: String, time: Long): Int

    @Query("UPDATE ${TempFileEntity.TABLE_NAME} " +
        "SET ${TempFileEntity.DELETED_ON_COLUMN} = :time " +
        "WHERE ${TempFileEntity.ID_COLUMN} = :id")
    abstract fun markDeletedById(id: Long, time: Long): Int

    @Query("UPDATE ${TempFileEntity.TABLE_NAME} " +
        "SET ${TempFileEntity.TAKEN_PHOTO_ID_COLUMN} = :takenPhotoId " +
        "WHERE ${TempFileEntity.ID_COLUMN} = :id")
    abstract fun updateTakenPhotoId(id: Long, takenPhotoId: Long): Int

    @Query("DELETE FROM ${TempFileEntity.TABLE_NAME} " +
        "WHERE ${TempFileEntity.ID_COLUMN} = :id")
    abstract fun deleteForReal(id: Long): Int
}