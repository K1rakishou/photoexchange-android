package com.kirakishou.photoexchange.helper.database.repository

import com.kirakishou.photoexchange.helper.database.MyDatabase
import com.kirakishou.photoexchange.helper.database.entity.ReceivedPhotoEntity
import com.kirakishou.photoexchange.helper.database.isSuccess
import com.kirakishou.photoexchange.helper.database.mapper.ReceivedPhotosMapper
import com.kirakishou.photoexchange.helper.util.TimeUtils
import com.kirakishou.photoexchange.mvp.model.ReceivedPhoto
import com.kirakishou.photoexchange.mvp.model.net.response.GetReceivedPhotosResponse
import com.kirakishou.photoexchange.mvp.model.net.response.ReceivedPhotosResponse
import timber.log.Timber

open class ReceivedPhotosRepository(
    private val database: MyDatabase,
    private val timeUtils: TimeUtils,
    private val receivedPhotoMaxCacheLiveTime: Long
) {
    private val TAG = "ReceivedPhotosRepository"
    private val receivedPhotosDao = database.receivedPhotoDao()

    fun save(receivedPhoto: GetReceivedPhotosResponse.ReceivedPhoto): Long {
        val now = timeUtils.getTimeFast()
        return receivedPhotosDao.save(ReceivedPhotosMapper.FromObject.toReceivedPhotoEntity(now, receivedPhoto))
    }

    fun save(receivedPhoto: ReceivedPhotosResponse.ReceivedPhoto): Boolean {
        val now = timeUtils.getTimeFast()
        return receivedPhotosDao.save(ReceivedPhotosMapper.FromResponse.ReceivedPhotos.toReceivedPhotoEntity(now, receivedPhoto))
            .isSuccess()
    }

    fun saveMany(receivedPhotos: List<GetReceivedPhotosResponse.ReceivedPhoto>): Boolean {
        val time = timeUtils.getTimeFast()
        return receivedPhotosDao.saveMany(ReceivedPhotosMapper.FromResponse.GetReceivedPhotos.toReceivedPhotoEntities(time, receivedPhotos))
            .size == receivedPhotos.size
    }

    fun count(): Int {
        return receivedPhotosDao.countAll().toInt()
    }

    fun findAll(): List<ReceivedPhoto> {
        return ReceivedPhotosMapper.FromEntity.toReceivedPhotos(receivedPhotosDao.findAll())
    }

    fun findAllTest(): List<ReceivedPhotoEntity> {
        return receivedPhotosDao.findAll()
    }

    fun findMany(receivedPhotoIds: List<Long>): MutableList<ReceivedPhoto> {
        return ReceivedPhotosMapper.FromEntity.toReceivedPhotos(receivedPhotosDao.findMany(receivedPhotoIds))
    }

    fun deleteOld() {
        val oldCount = findAllTest().size
        val now = timeUtils.getTimeFast()
        receivedPhotosDao.deleteOlderThan(now - receivedPhotoMaxCacheLiveTime)

        val newCount = findAllTest().size
        if (newCount < oldCount) {
            Timber.tag(TAG).d("Deleted ${newCount - oldCount} receivedPhotos from the cache")
        }
    }

    fun deleteAll() {
        receivedPhotosDao.deleteAll()
    }
}