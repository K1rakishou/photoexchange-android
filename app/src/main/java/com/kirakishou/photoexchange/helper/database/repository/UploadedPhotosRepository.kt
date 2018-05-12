package com.kirakishou.photoexchange.helper.database.repository

import com.kirakishou.photoexchange.helper.database.MyDatabase
import com.kirakishou.photoexchange.helper.database.mapper.UploadedPhotosMapper
import com.kirakishou.photoexchange.mvp.model.UploadedPhoto
import com.kirakishou.photoexchange.mvp.model.net.response.GetUploadedPhotosResponse

open class UploadedPhotosRepository(
    private val database: MyDatabase
) {
    private val uploadedPhotoDao = database.uploadedPhotoDao()

    fun saveMany(uploadedPhotoDataList: List<GetUploadedPhotosResponse.UploadedPhotoData>): Boolean {
        val entities = UploadedPhotosMapper.FromResponse.ToEntity.toUploadedPhotoEntities(uploadedPhotoDataList)
        return uploadedPhotoDao.saveMany(entities).size == uploadedPhotoDataList.size
    }

    fun findMany(uploadedPhotoIds: List<Long>): List<UploadedPhoto> {
        return UploadedPhotosMapper.FromEntity.ToObject.toUploadedPhotos(uploadedPhotoDao.findMany(uploadedPhotoIds))
    }
}