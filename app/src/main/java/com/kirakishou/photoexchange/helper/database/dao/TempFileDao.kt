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
        "WHERE ${TempFileEntity.PHOTO_OWNER_ID_COLUMN} = :arg0")
    abstract fun findById(id: Long): TempFileEntity

    @Query("DELETE FROM ${TempFileEntity.TABLE_NAME} " +
        "WHERE ${TempFileEntity.FILE_PATH_COLUMN} = :arg0")
    abstract fun deleteByFilePath(path: String): Long
}