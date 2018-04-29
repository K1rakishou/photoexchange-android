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
        "WHERE ${TempFileEntity.ID_COLUMN} = :id")
    abstract fun findById(id: Long): TempFileEntity?

    @Query("SELECT * FROM ${TempFileEntity.TABLE_NAME}")
    abstract fun findAll(): List<TempFileEntity>

    @Query("SELECT * FROM ${TempFileEntity.TABLE_NAME} " +
        "WHERE ${TempFileEntity.FILE_PATH_COLUMN} = :filePath")
    abstract fun findByFilePath(filePath: String): TempFileEntity?

    @Query("DELETE FROM ${TempFileEntity.TABLE_NAME} " +
        "WHERE ${TempFileEntity.FILE_PATH_COLUMN} = :path")
    abstract fun deleteByFilePath(path: String): Int

    @Query("DELETE FROM ${TempFileEntity.TABLE_NAME} " +
        "WHERE ${TempFileEntity.ID_COLUMN} = :id")
    abstract fun deleteById(id: Long): Int
}