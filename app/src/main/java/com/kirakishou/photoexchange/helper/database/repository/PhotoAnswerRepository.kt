package com.kirakishou.photoexchange.helper.database.repository

import com.kirakishou.photoexchange.helper.database.MyDatabase
import com.kirakishou.photoexchange.helper.database.entity.PhotoAnswerEntity
import com.kirakishou.photoexchange.helper.database.mapper.PhotoAnswerMapper
import com.kirakishou.photoexchange.mvp.model.net.response.PhotoAnswerResponse

open class PhotoAnswerRepository(
    private val database: MyDatabase
) {
    private val photoAnswerDao = database.photoAnswerDao()

    fun insert(photoAnswerEntity: PhotoAnswerEntity): Boolean {
        return photoAnswerDao.insert(photoAnswerEntity) > 0L
    }

    fun insert(photoAnswerJsonObject: PhotoAnswerResponse.PhotoAnswerJsonObject): Boolean {
        val photoAnswerEntity = PhotoAnswerMapper.toPhotoAnswerEntity(photoAnswerJsonObject)
        return insert(photoAnswerEntity)
    }

    fun countAll(): Int {
        return photoAnswerDao.countAll().toInt()
    }
}