package com.kirakishou.photoexchange.helper.database.dao

import android.arch.persistence.room.Dao
import android.arch.persistence.room.Insert
import android.arch.persistence.room.OnConflictStrategy
import android.arch.persistence.room.Query
import com.kirakishou.photoexchange.helper.database.entity.RecipientLocationEntity
import io.reactivex.Single

/**
 * Created by kirakishou on 1/8/2018.
 */

@Dao
interface RecipientLocationDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    fun saveOne(entity: RecipientLocationEntity): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    fun saveMany(vararg entityArray: RecipientLocationEntity): List<Long>

    @Query("SELECT * FROM ${RecipientLocationEntity.TABLE_NAME} WHERE photo_name in (:arg0)")
    fun findMany(photoNameList: List<String>): Single<List<RecipientLocationEntity>>
}