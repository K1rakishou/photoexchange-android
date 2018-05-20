package com.kirakishou.photoexchange.helper.database.repository

import com.kirakishou.photoexchange.helper.database.MyDatabase
import com.kirakishou.photoexchange.helper.database.isSuccess
import com.kirakishou.photoexchange.helper.database.mapper.ReceivedPhotosMapper
import com.kirakishou.photoexchange.mvp.model.ReceivedPhoto
import com.kirakishou.photoexchange.mvp.model.net.response.GetReceivedPhotosResponse
import com.kirakishou.photoexchange.mvp.model.net.response.ReceivedPhotosResponse

open class ReceivedPhotosRepository(
    private val database: MyDatabase
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
        return receivedPhotosDao.saveMany(ReceivedPhotosMapper.FromResponse.GetReceivedPhotos.toReceivedPhotoEntities(receivedPhotos))
            .size == receivedPhotos.size
    }

    fun countAll(): Int {
        return receivedPhotosDao.countAll().toInt()
    }

    fun findAll(): List<ReceivedPhoto> {
        return ReceivedPhotosMapper.FromEntity.toReceivedPhotos(receivedPhotosDao.findAll())
    }

    fun findMany(receivedPhotoIds: List<Long>): List<ReceivedPhoto> {
        return ReceivedPhotosMapper.FromEntity.toReceivedPhotos( receivedPhotosDao.findMany(receivedPhotoIds))
    }
}