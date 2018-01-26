package com.kirakishou.photoexchange.helper.database.dao

import android.arch.persistence.room.Dao
import android.arch.persistence.room.Insert
import android.arch.persistence.room.OnConflictStrategy
import android.arch.persistence.room.Query
import com.kirakishou.photoexchange.helper.database.entity.PhotoAnswerEntity
import com.kirakishou.photoexchange.mwvm.model.other.Constants
import io.reactivex.Single

/**
 * Created by kirakishou on 11/14/2017.
 */

@Dao
interface PhotoAnswerDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    fun saveOne(photoAnswerEntity: PhotoAnswerEntity): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    fun saveMany(vararg photoAnswerEntityArray: PhotoAnswerEntity): List<Long>

    @Query("SELECT * FROM ${PhotoAnswerEntity.TABLE_NAME} " +
            "WHERE (:arg2 - created_on) < ${Constants.SEVEN_DAYS} " +
            "ORDER BY created_on DESC " +
            "LIMIT :arg1 OFFSET :arg0")
    fun findPage(page: Int, count: Int, currentTime: Long): Single<List<PhotoAnswerEntity>>

    @Query("SELECT * FROM ${PhotoAnswerEntity.TABLE_NAME}")
    fun findAll(): Single<List<PhotoAnswerEntity>>

    @Query("SELECT COUNT(photo_id) FROM ${PhotoAnswerEntity.TABLE_NAME}")
    fun countAll(): Single<Long>

    @Query("DELETE FROM ${PhotoAnswerEntity.TABLE_NAME} WHERE photo_name = :arg0")
    fun deleteOne(photoName: String): Int
}