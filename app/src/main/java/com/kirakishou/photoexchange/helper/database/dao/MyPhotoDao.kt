package com.kirakishou.photoexchange.helper.database.dao

import android.arch.persistence.room.Dao
import android.arch.persistence.room.Insert
import android.arch.persistence.room.Query
import com.kirakishou.photoexchange.helper.database.entity.MyPhotoEntity

/**
 * Created by kirakishou on 3/3/2018.
 */

@Dao
abstract class MyPhotoDao {

    @Insert
    abstract fun insert(myPhotoEntity: MyPhotoEntity): Long

    @Query("SELECT * FROM ${MyPhotoEntity.TABLE_NAME} " +
        "WHERE ${MyPhotoEntity.ID_COLUMN} = :id")
    abstract fun findById(id: Long): MyPhotoEntity?

    @Query("SELECT * FROM ${MyPhotoEntity.TABLE_NAME} " +
        "WHERE ${MyPhotoEntity.ID_COLUMN} = :id")
    abstract fun deleteById(id: Long): Int

    @Query("SELECT * FROM ${MyPhotoEntity.TABLE_NAME}")
    abstract fun findAll(): List<MyPhotoEntity>
}