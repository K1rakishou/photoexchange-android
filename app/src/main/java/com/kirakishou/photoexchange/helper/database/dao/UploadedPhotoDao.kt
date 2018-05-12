package com.kirakishou.photoexchange.helper.database.dao

import android.arch.persistence.room.Dao
import android.arch.persistence.room.Insert
import android.arch.persistence.room.OnConflictStrategy
import android.arch.persistence.room.Query
import com.kirakishou.photoexchange.helper.database.entity.UploadedPhotoEntity

@Dao
abstract class UploadedPhotoDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract fun saveMany(uploadedPhotoEntityList: List<UploadedPhotoEntity>): Array<Long>

    @Query("SELECT * FROM ${UploadedPhotoEntity.TABLE_NAME} " +
        "WHERE ${UploadedPhotoEntity.PHOTO_ID_COLUMN} IN (:uploadedPhotoIds)")
    abstract fun findMany(uploadedPhotoIds: List<Long>): List<UploadedPhotoEntity>
}