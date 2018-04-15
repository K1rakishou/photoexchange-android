package com.kirakishou.photoexchange.helper.database.dao

import android.arch.persistence.room.Dao
import android.arch.persistence.room.Insert
import com.kirakishou.photoexchange.helper.database.entity.PhotoAnswerEntity

@Dao
abstract class PhotoAnswerDao {

    @Insert
    abstract fun insert(photoAnswerEntity: PhotoAnswerEntity): Long
}