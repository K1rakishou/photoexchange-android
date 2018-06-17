package com.kirakishou.photoexchange.helper.database.dao

import android.arch.persistence.room.*
import com.kirakishou.photoexchange.helper.database.entity.CachedPhotoIdEntity

@Dao
abstract class CachedPhotoIdDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract fun insert(cachedPhotoIdEntity: CachedPhotoIdEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract fun insertMany(cachedPhotoIdEntityList: List<CachedPhotoIdEntity>): Array<Long>

    @Query("SELECT * FROM ${CachedPhotoIdEntity.TABLE_NAME} " +
        "WHERE " +
        " ${CachedPhotoIdEntity.PHOTO_ID_COLUMN} = :photoId " +
        "AND " +
        " ${CachedPhotoIdEntity.PHOTO_TYPE_COLUMN} = :photoType")
    @TypeConverters(CachedPhotoIdEntity.CachedPhotoIdTypeConverter::class)
    abstract fun findByPhotoIdAndType(photoId: Long, photoType: CachedPhotoIdEntity.PhotoType): CachedPhotoIdEntity?

    @Query("SELECT * FROM ${CachedPhotoIdEntity.TABLE_NAME} " +
        "WHERE " +
        " ${CachedPhotoIdEntity.PHOTO_ID_COLUMN} < :lastId " +
        "AND " +
        " ${CachedPhotoIdEntity.PHOTO_TYPE_COLUMN} = :photoType " +
        "ORDER BY ${CachedPhotoIdEntity.PHOTO_ID_COLUMN} DESC " +
        "LIMIT :count")
    @TypeConverters(CachedPhotoIdEntity.CachedPhotoIdTypeConverter::class)
    abstract fun findOnePageByType(lastId: Long, photoType: CachedPhotoIdEntity.PhotoType, count: Int): List<CachedPhotoIdEntity>

    @Query("SELECT * FROM ${CachedPhotoIdEntity.TABLE_NAME} " +
        "ORDER BY ${CachedPhotoIdEntity.PHOTO_ID_COLUMN} DESC")
    abstract fun findAll(): List<CachedPhotoIdEntity>

    @Query("DELETE FROM ${CachedPhotoIdEntity.TABLE_NAME}")
    abstract fun deleteAll()
}