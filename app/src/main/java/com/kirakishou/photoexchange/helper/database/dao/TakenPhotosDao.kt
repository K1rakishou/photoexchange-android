package com.kirakishou.photoexchange.helper.database.dao

import android.arch.persistence.room.Dao
import android.arch.persistence.room.Insert
import android.arch.persistence.room.OnConflictStrategy
import android.arch.persistence.room.Query
import com.kirakishou.photoexchange.helper.database.MyDatabase
import com.kirakishou.photoexchange.helper.database.entity.TakenPhotoEntity
import io.reactivex.Flowable

/**
 * Created by kirakishou on 11/8/2017.
 */

@Dao
interface TakenPhotosDao {

    @Insert(onConflict = OnConflictStrategy.FAIL)
    fun saveOne(takenPhotoEntity: TakenPhotoEntity): Long

    @Query("SELECT * FROM ${TakenPhotoEntity.TABLE_NAME} " +
            "WHERE was_sent = ${MyDatabase.SQLITE_FALSE} AND failed_to_upload = ${MyDatabase.SQLITE_FALSE} " +
            "ORDER BY created_on ASC " +
            "LIMIT 1")
    fun findLastSaved(): Flowable<TakenPhotoEntity>

    @Query("SELECT * FROM ${TakenPhotoEntity.TABLE_NAME} " +
            "ORDER BY created_on ASC " +
            "LIMIT :arg1 OFFSET :arg0")
    fun findPage(page: Int, count: Int): Flowable<List<TakenPhotoEntity>>

    @Query("SELECT * FROM ${TakenPhotoEntity.TABLE_NAME}")
    fun findAll(): Flowable<List<TakenPhotoEntity>>
}