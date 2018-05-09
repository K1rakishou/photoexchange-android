package com.kirakishou.photoexchange.helper.database.dao

import android.arch.persistence.room.Dao
import android.arch.persistence.room.Insert
import android.arch.persistence.room.OnConflictStrategy
import android.arch.persistence.room.Query
import com.kirakishou.photoexchange.helper.database.entity.GalleryPhotoEntity

@Dao
abstract class GalleryPhotoDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract fun saveMany(galleryPhotos: List<GalleryPhotoEntity>): Array<Long>

    @Query("SELECT * FROM ${GalleryPhotoEntity.TABLE_NAME} " +
        "WHERE ${GalleryPhotoEntity.GALLERY_PHOTO_ID_COLUMN} IN (:galleryPhotoIds)")
    abstract fun findMany(galleryPhotoIds: List<Long>): List<GalleryPhotoEntity>
}