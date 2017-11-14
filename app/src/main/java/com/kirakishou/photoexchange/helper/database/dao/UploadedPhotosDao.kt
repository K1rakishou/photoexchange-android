package com.kirakishou.photoexchange.helper.database.dao

import android.arch.persistence.room.Dao
import android.arch.persistence.room.Insert
import android.arch.persistence.room.OnConflictStrategy
import android.arch.persistence.room.Query
import com.kirakishou.photoexchange.helper.database.MyDatabase
import com.kirakishou.photoexchange.helper.database.entity.UploadedPhotoEntity
import io.reactivex.Flowable
import io.reactivex.Maybe
import io.reactivex.Single

/**
 * Created by kirakishou on 11/8/2017.
 */

@Dao
interface UploadedPhotosDao {

    @Insert
    fun saveOne(uploadedPhotoEntity: UploadedPhotoEntity): Long

    @Query("SELECT * FROM ${UploadedPhotoEntity.TABLE_NAME} " +
            "WHERE id = :arg0 " +
            "LIMIT 1")
    fun findOne(photoId: Long): Flowable<UploadedPhotoEntity>

    @Query("SELECT * FROM ${UploadedPhotoEntity.TABLE_NAME} " +
            "ORDER BY created_on DESC " +
            "LIMIT :arg1 OFFSET :arg0")
    fun findPage(page: Int, count: Int): Single<List<UploadedPhotoEntity>>

    @Query("SELECT * FROM ${UploadedPhotoEntity.TABLE_NAME} " +
            "ORDER BY created_on DESC ")
    fun findAll(): Single<List<UploadedPhotoEntity>>

    @Query("DELETE FROM ${UploadedPhotoEntity.TABLE_NAME} WHERE id = :arg0")
    fun deleteOne(id: Long): Int

    @Query("DELETE FROM ${UploadedPhotoEntity.TABLE_NAME}")
    fun deleteAll(): Int
}