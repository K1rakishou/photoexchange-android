package com.kirakishou.photoexchange.helper.database.repository

import com.kirakishou.photoexchange.helper.database.MyDatabase
import com.kirakishou.photoexchange.helper.database.entity.ReceivedPhotoEntity
import com.kirakishou.photoexchange.helper.database.mapper.ReceivedPhotosMapper
import com.kirakishou.photoexchange.mvp.model.ReceivedPhoto
import com.kirakishou.photoexchange.mvp.model.net.response.ReceivedPhotosResponse

open class ReceivedPhotosRepository(
    private val database: MyDatabase
) {
    private val photoAnswerDao = database.receivedPhotoDao()

    fun insert(receivedPhotoEntity: ReceivedPhotoEntity): Long {
        return photoAnswerDao.insert(receivedPhotoEntity)
    }

    fun insert(receivedPhotos: ReceivedPhotosResponse.ReceivedPhoto): Long {
        val photoAnswerEntity = ReceivedPhotosMapper.toPhotoAnswerEntity(receivedPhotos)
        return insert(photoAnswerEntity)
    }

    fun countAll(): Int {
        return photoAnswerDao.countAll().toInt()
    }

    fun findAll(): List<ReceivedPhoto> {
        val allPhotos = photoAnswerDao.findAll()
        return allPhotos.map { ReceivedPhotosMapper.toPhotoAnswer(it) }
    }
}