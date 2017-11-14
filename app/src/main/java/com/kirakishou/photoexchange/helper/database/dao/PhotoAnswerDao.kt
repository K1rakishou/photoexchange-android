package com.kirakishou.photoexchange.helper.database.dao

import android.arch.persistence.room.Dao
import android.arch.persistence.room.Insert
import android.arch.persistence.room.OnConflictStrategy
import com.kirakishou.photoexchange.helper.database.entity.PhotoAnswerEntity

/**
 * Created by kirakishou on 11/14/2017.
 */

@Dao
interface PhotoAnswerDao {

    @Insert
    fun saveMany(vararg photoAnswerEntityArray: PhotoAnswerEntity): List<Long>
}