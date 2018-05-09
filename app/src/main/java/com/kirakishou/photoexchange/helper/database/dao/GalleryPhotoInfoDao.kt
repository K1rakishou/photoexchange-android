package com.kirakishou.photoexchange.helper.database.dao

import android.arch.persistence.room.Dao
import android.arch.persistence.room.Insert
import android.arch.persistence.room.OnConflictStrategy
import android.arch.persistence.room.Query
import com.kirakishou.photoexchange.helper.database.MyDatabase
import com.kirakishou.photoexchange.helper.database.entity.GalleryPhotoInfoEntity

@Dao
abstract class GalleryPhotoInfoDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract fun save(galleryPhotoInfoEntity: GalleryPhotoInfoEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract fun saveMany(galleryPhotoInfoEntityList: List<GalleryPhotoInfoEntity>): Array<Long>

    @Query("SELECT * FROM ${GalleryPhotoInfoEntity.TABLE_NAME} " +
        "WHERE ${GalleryPhotoInfoEntity.GALLERY_PHOTO_ID_COLUMN} = :galleryPhotoId")
    abstract fun find(galleryPhotoId: Long): GalleryPhotoInfoEntity?

    @Query("SELECT * FROM ${GalleryPhotoInfoEntity.TABLE_NAME} " +
        "WHERE " +
        "${GalleryPhotoInfoEntity.GALLERY_PHOTO_ID_COLUMN} IN (:galleryPhotoIds) " +
        " AND " +
        "${GalleryPhotoInfoEntity.LAST_UPDATE_TIME_COLUMN} > :lastUpdateTime")
    abstract fun findMany(galleryPhotoIds: List<Long>, lastUpdateTime: Long): List<GalleryPhotoInfoEntity>

    @Query("DELETE FROM ${GalleryPhotoInfoEntity.TABLE_NAME} " +
        "WHERE ${GalleryPhotoInfoEntity.GALLERY_PHOTO_ID_COLUMN} = :galleryPhotoId")
    abstract fun delete(galleryPhotoId: Long)
}