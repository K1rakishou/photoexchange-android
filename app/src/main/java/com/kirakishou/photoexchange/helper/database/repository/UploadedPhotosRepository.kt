package com.kirakishou.photoexchange.helper.database.repository

import com.kirakishou.photoexchange.helper.database.MyDatabase
import com.kirakishou.photoexchange.helper.database.entity.UploadedPhotoEntity
import com.kirakishou.photoexchange.helper.database.isSuccess
import com.kirakishou.photoexchange.helper.database.mapper.UploadedPhotosMapper
import com.kirakishou.photoexchange.mvp.model.UploadedPhoto
import com.kirakishou.photoexchange.mvp.model.net.response.GetUploadedPhotosResponse

open class UploadedPhotosRepository(
    private val database: MyDatabase
) {
    private val uploadedPhotoDao = database.uploadedPhotoDao()

    private fun save(uploadedPhotoEntity: UploadedPhotoEntity): Boolean {
        val cachedPhoto = uploadedPhotoDao.findByPhotoName(uploadedPhotoEntity.photoName)
        if (cachedPhoto != null) {
            uploadedPhotoEntity.photoId = cachedPhoto.photoId
        }

        return uploadedPhotoDao.save(uploadedPhotoEntity).isSuccess()
    }

    open fun saveMany(uploadedPhotoDataList: List<GetUploadedPhotosResponse.UploadedPhotoData>): Boolean {
        return database.transactional {
            for (uploadedPhotoData in uploadedPhotoDataList) {
                val photo = UploadedPhotosMapper.FromResponse.ToEntity.toUploadedPhotoEntity(uploadedPhotoData)
                if (!save(photo)) {
                    return@transactional false
                }
            }

            return@transactional true
        }
    }

    open fun findMany(photoIds: List<Long>): List<UploadedPhoto> {
        return UploadedPhotosMapper.FromEntity.ToObject.toUploadedPhotos(uploadedPhotoDao.findMany(photoIds))
    }

    open fun save(photoId: Long, photoName: String, lon: Double, lat: Double, uploadedOn: Long): Boolean {
        val uploadedPhotoEntity = UploadedPhotosMapper.FromObject.ToEntity.toUploadedPhotoEntity(photoId, photoName, lon, lat, uploadedOn)
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

    fun updateReceiverInfo(uploadedPhotoName: String): Boolean {
        return uploadedPhotoDao.updateReceiverInfo(uploadedPhotoName) == 1
    }
}