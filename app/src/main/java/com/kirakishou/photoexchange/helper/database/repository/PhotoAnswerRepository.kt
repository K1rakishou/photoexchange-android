package com.kirakishou.photoexchange.helper.database.repository

import com.kirakishou.photoexchange.helper.api.mapper.PhotoAnswerResponseMapper
import com.kirakishou.photoexchange.helper.database.MyDatabase
import com.kirakishou.photoexchange.helper.database.entity.PhotoAnswerEntity
import com.kirakishou.photoexchange.mvp.model.net.response.PhotoAnswerResponse

open class PhotoAnswerRepository(
    private val database: MyDatabase
) {
    private val photoAnswerDao = database.photoAnswerDao()

    fun insert(photoAnswerEntity: PhotoAnswerEntity): Long {
        return photoAnswerDao.insert(photoAnswerEntity)
    }

    fun insert(photoAnswer: PhotoAnswerResponse.PhotoAnswer): Long {
        val photoAnswerEntity = PhotoAnswerResponseMapper.toPhotoAnswerEntity(photoAnswer)
        return insert(photoAnswerEntity)
    }

    fun countAll(): Int {
        return photoAnswerDao.countAll().toInt()
    }
}