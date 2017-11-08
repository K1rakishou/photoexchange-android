package com.kirakishou.photoexchange.helper.database.dao

import android.arch.persistence.room.Dao
import android.arch.persistence.room.Insert
import android.arch.persistence.room.OnConflictStrategy
import android.arch.persistence.room.Query
import com.kirakishou.photoexchange.helper.database.entity.TakenPhotoEntity
import io.reactivex.Single

/**
 * Created by kirakishou on 11/8/2017.
 */

@Dao
interface TakenPhotosDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun saveOne(takenPhotoEntity: TakenPhotoEntity): Long

    @Query("SELECT * FROM ${TakenPhotoEntity.TABLE_NAME} WHERE was_sent IS FALSE ORDER BY created_on ASC LIMIT 1")
    fun findLastSaved(): Single<TakenPhotoEntity>
}