package com.kirakishou.photoexchange.helper.database.dao

import android.arch.persistence.room.Dao
import android.arch.persistence.room.Insert
import android.arch.persistence.room.OnConflictStrategy
import android.arch.persistence.room.Query
import com.kirakishou.photoexchange.helper.database.entity.TakenPhotoEntity
import io.reactivex.Single

/**
 * Created by kirakishou on 11/10/2017.
 */

@Dao
interface TakenPhotosDao {

    @Insert
    fun saveOne(takenPhotoEntity: TakenPhotoEntity): Long

    @Query("SELECT * FROM ${TakenPhotoEntity.TABLE_NAME}")
    fun findAll(): Single<List<TakenPhotoEntity>>

    @Query("DELETE FROM ${TakenPhotoEntity.TABLE_NAME}")
    fun deleteAll(): Int
}