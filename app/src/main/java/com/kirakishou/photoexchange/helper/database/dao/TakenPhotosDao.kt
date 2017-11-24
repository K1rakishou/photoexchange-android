package com.kirakishou.photoexchange.helper.database.dao

import android.arch.persistence.room.Dao
import android.arch.persistence.room.Insert
import android.arch.persistence.room.OnConflictStrategy
import android.arch.persistence.room.Query
import com.kirakishou.photoexchange.helper.database.MyDatabase
import com.kirakishou.photoexchange.helper.database.entity.TakenPhotoEntity
import io.reactivex.Single

/**
 * Created by kirakishou on 11/10/2017.
 */

@Dao
interface TakenPhotosDao {

    @Insert
    fun saveOne(takenPhotoEntity: TakenPhotoEntity): Long

    @Query("SELECT * FROM ${TakenPhotoEntity.TABLE_NAME} WHERE id = :arg0")
    fun findOne(id: Long): Single<TakenPhotoEntity>

    @Query("SELECT * FROM ${TakenPhotoEntity.TABLE_NAME} WHERE is_uploading = ${MyDatabase.SQLITE_TRUE}")
    fun findAllQueuedUp(): Single<List<TakenPhotoEntity>>

    @Query("SELECT * FROM ${TakenPhotoEntity.TABLE_NAME}")
    fun findAll(): Single<List<TakenPhotoEntity>>

    @Query("UPDATE ${TakenPhotoEntity.TABLE_NAME} SET is_uploading = :arg0 WHERE id = :arg1")
    fun updateOneSetIsUploading(isUploading: Boolean, id: Long)

    @Query("DELETE FROM ${TakenPhotoEntity.TABLE_NAME} WHERE id = :arg0")
    fun deleteOne(id: Long): Int

    @Query("DELETE FROM ${TakenPhotoEntity.TABLE_NAME} WHERE is_uploading = ${MyDatabase.SQLITE_FALSE}")
    fun deleteAll(): Int
}