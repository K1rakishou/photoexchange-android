package com.kirakishou.photoexchange.helper.database.repository

import com.kirakishou.photoexchange.helper.database.MyDatabase
import com.kirakishou.photoexchange.helper.database.isSuccess
import com.kirakishou.photoexchange.helper.database.mapper.ReceivedPhotosMapper
import com.kirakishou.photoexchange.helper.util.TimeUtils
import com.kirakishou.photoexchange.mvp.model.ReceivedPhoto
import com.kirakishou.photoexchange.mvp.model.net.response.GetReceivedPhotosResponse
import com.kirakishou.photoexchange.mvp.model.net.response.ReceivedPhotosResponse

open class ReceivedPhotosRepository(
    private val database: MyDatabase,
    private val timeUtils: TimeUtils,
    private val receivedPhotoMaxCacheLiveTime: Long
) {
    private val receivedPhotosDao = database.receivedPhotoDao()

    fun save(receivedPhoto: GetReceivedPhotosResponse.ReceivedPhoto): Long {
        return receivedPhotosDao.save(ReceivedPhotosMapper.FromObject.toPhotoAnswerEntity(receivedPhoto))
    }

    fun save(receivedPhoto: ReceivedPhotosResponse.ReceivedPhoto): Boolean {
        return receivedPhotosDao.save(ReceivedPhotosMapper.FromResponse.ReceivedPhotos.toReceivedPhotoEntity(receivedPhoto))
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

    fun findMany(receivedPhotoIds: List<Long>): MutableList<ReceivedPhoto> {
        return ReceivedPhotosMapper.FromEntity.toReceivedPhotos(receivedPhotosDao.findMany(receivedPhotoIds))
    }

    fun deleteOld() {
        val now = timeUtils.getTimeFast()
        receivedPhotosDao.deleteOlderThan(now - receivedPhotoMaxCacheLiveTime)
    }
}