package com.kirakishou.photoexchange.helper.repository.dao

import android.arch.persistence.room.Dao
import android.arch.persistence.room.Insert
import android.arch.persistence.room.OnConflictStrategy
import android.arch.persistence.room.Query
import com.kirakishou.photoexchange.helper.repository.entity.SentPhotoEntity
import io.reactivex.Flowable
import io.reactivex.Single

/**
 * Created by kirakishou on 11/8/2017.
 */

@Dao
interface SentPhotosDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun saveOne(sentPhotoEntity: SentPhotoEntity): Long

    @Query("SELECT * FROM ${SentPhotoEntity.TABLE_NAME} WHERE user_id = :arg0 ORDER BY created_on ASC LIMIT 1")
    fun findLastSaved(userId: String): Single<SentPhotoEntity>
}