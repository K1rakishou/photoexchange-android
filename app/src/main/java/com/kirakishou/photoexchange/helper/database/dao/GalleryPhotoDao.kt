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
        "WHERE ${GalleryPhotoEntity.GALLERY_PHOTO_ID_COLUMN} IN (:galleryPhotoIds) " +
        "ORDER BY ${GalleryPhotoEntity.GALLERY_PHOTO_ID_COLUMN} DESC")
    abstract fun findMany(galleryPhotoIds: List<Long>): List<GalleryPhotoEntity>

    @Query("SELECT * FROM ${GalleryPhotoEntity.TABLE_NAME} " +
        "WHERE ${GalleryPhotoEntity.PHOTO_NAME_COLUMN} = :photoName")
    abstract fun findByPhotoName(photoName: String): GalleryPhotoEntity?

    @Query("UPDATE ${GalleryPhotoEntity.TABLE_NAME} " +
        "SET ${GalleryPhotoEntity.FAVOURITED_COUNT_COLUMN} = :favouritesCount " +
        "WHERE ${GalleryPhotoEntity.PHOTO_NAME_COLUMN} = :photoName")
    abstract fun updateFavouritesCount(photoName: String, favouritesCount: Long): Int

    @Query("DELETE FROM ${GalleryPhotoEntity.TABLE_NAME} " +
        "WHERE ${GalleryPhotoEntity.INSERTED_ON_COLUMN} < :time")
    abstract fun deleteOlderThan(time: Long)

    @Query("SELECT * FROM ${GalleryPhotoEntity.TABLE_NAME}")
    abstract fun findAll(): List<GalleryPhotoEntity>
}