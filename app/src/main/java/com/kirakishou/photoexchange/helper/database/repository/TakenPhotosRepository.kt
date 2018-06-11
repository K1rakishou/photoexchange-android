package com.kirakishou.photoexchange.helper.database.repository

import com.kirakishou.photoexchange.helper.database.MyDatabase
import com.kirakishou.photoexchange.helper.database.entity.TakenPhotoEntity
import com.kirakishou.photoexchange.helper.database.entity.TempFileEntity
import com.kirakishou.photoexchange.helper.database.isFail
import com.kirakishou.photoexchange.helper.database.isSuccess
import com.kirakishou.photoexchange.helper.database.mapper.TakenPhotosMapper
import com.kirakishou.photoexchange.helper.util.TimeUtils
import com.kirakishou.photoexchange.mvp.model.TakenPhoto
import com.kirakishou.photoexchange.mvp.model.PhotoState
import com.kirakishou.photoexchange.mvp.model.other.LonLat
import java.util.concurrent.TimeUnit

/**
 * Created by kirakishou on 3/3/2018.
 */
open class TakenPhotosRepository(
    private val timeUtils: TimeUtils,
    private val database: MyDatabase,
    private val tempFileRepository: TempFileRepository
) {
    private val TAG = "TakenPhotosRepository"
    private val takenPhotoDao = database.takenPhotoDao()

    init {
        tempFileRepository.init()
    }

    fun saveTakenPhoto(tempFile: TempFileEntity): TakenPhoto {
        var photo = TakenPhoto.empty()

        val transactionResult = database.transactional {
            val myPhotoEntity = TakenPhotoEntity.create(tempFile.id!!, false, timeUtils.getTimeFast())
            val insertedPhotoId = takenPhotoDao.insert(myPhotoEntity)

            if (insertedPhotoId.isFail()) {
                return@transactional false
            }

            myPhotoEntity.id = insertedPhotoId
            photo = TakenPhotosMapper.toMyPhoto(myPhotoEntity, tempFile)

            return@transactional tempFileRepository.updateTakenPhotoId(tempFile, insertedPhotoId).isSuccess()
        }

        if (!transactionResult) {
            tempFileRepository.markDeletedById(tempFile)
            return TakenPhoto.empty()
        }

        return photo
    }

    fun updatePhotoState(photoId: Long, newPhotoState: PhotoState): Boolean {
        return takenPhotoDao.updateSetNewPhotoState(photoId, newPhotoState) == 1
    }

    fun updateMakePhotoPublic(takenPhotoId: Long): Boolean {
        return takenPhotoDao.updateSetPhotoPublic(takenPhotoId) == 1
    }

    fun updateAllPhotosLocation(location: LonLat) {
        if (location.isEmpty()) {
            return
        }

        val allPhotosWithEmptyLocation = takenPhotoDao.findAllWithEmptyLocation()
        if (allPhotosWithEmptyLocation.isEmpty()) {
            return
        }

        database.transactional {
            for (photo in allPhotosWithEmptyLocation) {
                if (takenPhotoDao.updatePhotoLocation(photo.id!!, location.lon, location.lat) != 1) {
                    return@transactional false
                }
            }

            return@transactional true
        }
    }

    fun hasPhotosWithEmptyLocation(): Boolean {
        return takenPhotoDao.findAllWithEmptyLocation().isNotEmpty()
    }

    fun findById(id: Long): TakenPhoto {
        val myPhotoEntity = takenPhotoDao.findById(id) ?: TakenPhotoEntity.empty()
        val tempFileEntity = findTempFileById(id)

        return TakenPhotosMapper.toMyPhoto(myPhotoEntity, tempFileEntity)
    }

    fun findAll(): List<TakenPhoto> {
        val allMyPhotos = arrayListOf<TakenPhoto>()
        val allMyPhotoEntities = takenPhotoDao.findAll()

        for (myPhotoEntity in allMyPhotoEntities) {
            myPhotoEntity.id?.let { myPhotoId ->
                val tempFile = findTempFileById(myPhotoId)

                TakenPhotosMapper.toMyPhoto(myPhotoEntity, tempFile).let { myPhoto ->
                    allMyPhotos += myPhoto
                }
            }
        }

        return allMyPhotos
    }

    fun countAllByState(state: PhotoState): Int {
        return takenPhotoDao.countAllByState(state).toInt()
    }

    fun countAllByStates(states: Array<PhotoState>): Int {
        return takenPhotoDao.countAllByStates(states)
    }

    fun updateStates(oldState: PhotoState, newState: PhotoState) {
        takenPhotoDao.updateStates(oldState, newState)
    }

    fun findAllByState(state: PhotoState): List<TakenPhoto> {
        val resultList = mutableListOf<TakenPhoto>()

        database.transactional {
            val allPhotoReadyToUploading = takenPhotoDao.findAllWithState(state)

            for (photo in allPhotoReadyToUploading) {
                val tempFileEntity = findTempFileById(photo.id!!)
                resultList += TakenPhotosMapper.toMyPhoto(photo, tempFileEntity)
            }

            return@transactional true
        }

        return resultList
    }

    fun deleteMyPhoto(takenPhoto: TakenPhoto?): Boolean {
        if (takenPhoto == null) {
            return true
        }

        if (takenPhoto.isEmpty()) {
            return true
        }

        return deletePhotoById(takenPhoto.id)
    }

    fun deletePhotoByName(photoName: String): Boolean {
        val photoId = takenPhotoDao.findPhotoIdByName(photoName)
        if (photoId == null) {
            return true
        }

        return deletePhotoById(photoId)
    }

    fun deletePhotoById(photoId: Long): Boolean {
        return database.transactional {
            if (takenPhotoDao.deleteById(photoId).isFail()) {
                return@transactional false
            }

            return@transactional deleteTempFileById(photoId)
        }
    }

    fun deleteAllWithState(photoState: PhotoState): Boolean {
        return database.transactional {
            val myPhotosList = takenPhotoDao.findAllWithState(photoState)

            for (myPhoto in myPhotosList) {
                if (!deletePhotoById(myPhoto.id!!)) {
                    return@transactional false
                }
            }

            return@transactional true
        }
    }

    private fun deleteTempFileById(id: Long): Boolean {
        val tempFileEntity = tempFileRepository.findById(id)
        if (tempFileEntity.isEmpty()) {
            //has already been deleted
            return true
        }

        if (tempFileRepository.markDeletedById(id).isFail()) {
            return false
        }

        return true
    }

    private fun findTempFileById(id: Long): TempFileEntity {
        return tempFileRepository.findById(id)
    }

    fun findTempFile(id: Long): TempFileEntity {
        return tempFileRepository.findById(id)
    }
}