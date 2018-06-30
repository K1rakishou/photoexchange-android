package com.kirakishou.photoexchange.helper.database.repository

import com.kirakishou.photoexchange.helper.database.MyDatabase
import com.kirakishou.photoexchange.helper.database.entity.CachedPhotoIdEntity
import com.kirakishou.photoexchange.helper.database.isSuccess

class CachedPhotoIdRepository(
    private val database: MyDatabase
) {
    private val cachedPhotoIdDao = database.cachedPhotoIdDao()

    fun insert(photoId: Long, photoType: CachedPhotoIdEntity.PhotoType): Boolean {
        val existingCachedPhotoId = cachedPhotoIdDao.findByPhotoIdAndType(photoId, photoType)
        val id = if (existingCachedPhotoId == null) {
            null
        } else {
            existingCachedPhotoId.id!!
        }

        val cachedPhotoIdEntity = CachedPhotoIdEntity.create(id, photoId, photoType)
        return cachedPhotoIdDao.insert(cachedPhotoIdEntity).isSuccess()
    }

    fun insertMany(photoIdList: List<Long>, photoType: CachedPhotoIdEntity.PhotoType): Boolean {
        val entitiesToInsert = mutableListOf<CachedPhotoIdEntity>()
        val distinctPhotoIdList = photoIdList.distinct()

        for (photoId in distinctPhotoIdList) {
            val existingCachedPhotoId = cachedPhotoIdDao.findByPhotoIdAndType(photoId, photoType)
            val id = if (existingCachedPhotoId == null) {
                null
            } else {
                existingCachedPhotoId.id!!
            }

            entitiesToInsert += CachedPhotoIdEntity.create(id, photoId, photoType)
        }

        return cachedPhotoIdDao.insertMany(entitiesToInsert).size == distinctPhotoIdList.size
    }

    fun findOnePageByType(lastId: Long, photoType: CachedPhotoIdEntity.PhotoType, count: Int): List<Long> {
        return cachedPhotoIdDao.findOnePageByType(lastId, photoType, count)
            .map { it.photoId }
    }

    fun findAll(photoType: CachedPhotoIdEntity.PhotoType): List<Long> {
        return cachedPhotoIdDao.findAll(photoType)
            .map { it.photoId }
    }

    fun findAll(): List<CachedPhotoIdEntity> {
        return cachedPhotoIdDao.findAll()
    }

    fun isEmpty(photoType: CachedPhotoIdEntity.PhotoType): Boolean {
        return cachedPhotoIdDao.count(photoType) == 0L
    }

    fun count(photoType: CachedPhotoIdEntity.PhotoType): Int {
        return cachedPhotoIdDao.count(photoType).toInt()
    }

    fun deleteAll() {
        cachedPhotoIdDao.deleteAll()
    }

    fun deleteAll(photoType: CachedPhotoIdEntity.PhotoType) {
        cachedPhotoIdDao.deleteAll(photoType)
    }
}