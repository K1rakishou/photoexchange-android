package com.kirakishou.photoexchange.helper.database.repository

import com.kirakishou.photoexchange.helper.database.MyDatabase
import com.kirakishou.photoexchange.helper.database.isSuccess
import com.kirakishou.photoexchange.helper.database.mapper.UploadedPhotosMapper
import com.kirakishou.photoexchange.mvp.model.TakenPhoto
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

    fun save(photo: TakenPhoto): Boolean {
        val uploadedPhotoEntity = UploadedPhotosMapper.FromObject.ToEntity.toUploadedPhotoEntity(photo)
        return uploadedPhotoDao.save(uploadedPhotoEntity).isSuccess()
    }

    fun count(): Int {
        return uploadedPhotoDao.count().toInt()
    }

    fun findAll(): List<UploadedPhoto> {
        return UploadedPhotosMapper.FromEntity.ToObject.toUploadedPhotos(uploadedPhotoDao.findAll())
    }

    fun findAll(withReceivedInfo: Boolean): List<UploadedPhoto> {
        val entities = if (withReceivedInfo) {
            uploadedPhotoDao.findAllWithReceiverInfo()
        } else {
            uploadedPhotoDao.findAllWithoutReceiverInfo()
        }

        return UploadedPhotosMapper.FromEntity.ToObject.toUploadedPhotos(entities)
    }

    fun findByPhotoIdByName(uploadedPhotoName: String): Long {
        return uploadedPhotoDao.findByPhotoIdByName(uploadedPhotoName)
    }

    fun updateReceiverInfo(uploadedPhotoName: String, lon: Double, lat: Double): Boolean {
        return uploadedPhotoDao.updateReceiverInfo(uploadedPhotoName, lon, lat) == 1
    }
}