package com.kirakishou.photoexchange.helper.database.repository

import com.kirakishou.photoexchange.helper.database.MyDatabase
import com.kirakishou.photoexchange.helper.database.entity.MyPhotoEntity
import com.kirakishou.photoexchange.helper.database.entity.TempFileEntity
import com.kirakishou.photoexchange.helper.database.mapper.MyPhotoEntityMapper
import com.kirakishou.photoexchange.mvp.model.MyPhoto
import com.kirakishou.photoexchange.mvp.model.PhotoState
import timber.log.Timber
import java.io.File

/**
 * Created by kirakishou on 3/3/2018.
 */
open class PhotosRepository(
    private val filesDir: String,
    private val database: MyDatabase
) {
    private val myPhotoDao = database.myPhotoDao()
    private val tempFileDao = database.tempFileDao()

    fun init() {
        createTempFilesDirIfNotExists()
    }

    fun saveTakenPhoto(file: File): MyPhoto {
        deleteAllWithState(PhotoState.PHOTO_TAKEN)

        val tempFileEntity = TempFileEntity.create(file.absolutePath)
        val tempFileId = tempFileDao.insert(tempFileEntity)
        if (tempFileId <= 0L) {
            return MyPhoto.empty()
        }

        val myPhotoEntity = MyPhotoEntity.create(tempFileId, false)
        val myPhotoId = myPhotoDao.insert(myPhotoEntity)

        myPhotoEntity.id = myPhotoId
        return MyPhotoEntityMapper.toMyPhoto(myPhotoEntity, tempFileEntity)
    }

    fun updateSetPhotoName(photoId: Long, photoName: String): Boolean {
        return myPhotoDao.updateSetPhotoName(photoId, photoName) == 1
    }

    fun updateSetTempFileId(photoId: Long, newTempFileId: Long?): Boolean {
        return myPhotoDao.updateSetTempFileId(photoId, newTempFileId) == 1
    }

    fun updatePhotoState(photoId: Long, newPhotoState: PhotoState): Boolean {
        return myPhotoDao.updateSetNewPhotoState(photoId, newPhotoState) == 1
    }

    fun updatePhotoState(photoName: String, newPhotoState: PhotoState): Boolean {
        val photoId = myPhotoDao.findByName(photoName)?.id ?: return false
        return myPhotoDao.updateSetNewPhotoState(photoId, newPhotoState) == 1
    }

    fun updatePhotosStates(oldPhotoState: PhotoState, newPhotoState: PhotoState) {
        database.transactional {
            try {
                val foundPhotos = findAllByState(oldPhotoState)

                for (photo in foundPhotos) {
                    updatePhotoState(photo.id, newPhotoState)
                }

                return@transactional true
            } catch (error: Throwable) {
                Timber.e(error)
                return@transactional false
            }
        }
    }

    fun deleteMyPhoto(myPhoto: MyPhoto): Boolean {
        if (myPhoto.isEmpty()) {
            return true
        }

        return deleteById(myPhoto.id)
    }

    fun createFile(): File {
        filesDir
        val fullPathFile = File(filesDir)
        return File.createTempFile("file_", ".tmp", fullPathFile)
    }

    fun findById(id: Long): MyPhoto {
        val myPhotoEntity = myPhotoDao.findById(id) ?: MyPhotoEntity.empty()
        val tempFileEntity = findTempFileById(id)

        return MyPhotoEntityMapper.toMyPhoto(myPhotoEntity, tempFileEntity)
    }

    fun findAll(): List<MyPhoto> {
        val allMyPhotos = arrayListOf<MyPhoto>()
        val allMyPhotoEntities = myPhotoDao.findAll()

        for (myPhotoEntity in allMyPhotoEntities) {
            myPhotoEntity.id?.let { myPhotoId ->
                val tempFile = findTempFileById(myPhotoId)

                MyPhotoEntityMapper.toMyPhoto(myPhotoEntity, tempFile).let { myPhoto ->
                    allMyPhotos += myPhoto
                }
            }
        }

        return allMyPhotos
    }

    fun countAllByState(state: PhotoState): Int {
        return myPhotoDao.countAllByState(state).toInt()
    }

    fun countAllByStates(states: Array<PhotoState>): Int {
        return myPhotoDao.countAllByStates(states)
    }

    fun findPhotoByStateAndUpdateState(oldState: PhotoState, newState: PhotoState): MyPhoto? {
        var photo: MyPhotoEntity? = null

        database.transactional {
            photo = myPhotoDao.findOnePhotoWithState(oldState)
            if (photo == null || photo?.id == null) {
                return@transactional false
            }

            return@transactional myPhotoDao.updateSetNewPhotoState(photo!!.id!!, newState) == 1
        }

        return photo?.let { myPhoto ->
            myPhoto.id?.let { photoId ->
                val tempFileEntity = findTempFileById(photoId)
                return@let MyPhotoEntityMapper.toMyPhoto(myPhoto, tempFileEntity)
            }
        }
    }

    fun findOnePhotoByState(state: PhotoState): MyPhoto? {
        val photo = myPhotoDao.findOnePhotoWithState(state) ?: return null
        val tempFileEntity = findTempFileById(photo.id!!)
        return MyPhotoEntityMapper.toMyPhoto(photo, tempFileEntity)
    }

    fun findAllByState(state: PhotoState): List<MyPhoto> {
        val allPhotoReadyToUploading = myPhotoDao.findAllWithState(state)
        val resultList = mutableListOf<MyPhoto>()

        database.transactional {
            for (photo in allPhotoReadyToUploading) {
                val tempFileEntity = findTempFileById(photo.id!!)
                resultList += MyPhotoEntityMapper.toMyPhoto(photo, tempFileEntity)
            }

            return@transactional true
        }

        return resultList
    }

    private fun deleteById(id: Long): Boolean {
        val tempFileEntity = findTempFileById(id)
        var fileDeleteResult = false

        if (!tempFileEntity.isEmpty()) {
            val photoFile = File(tempFileEntity.filePath)
            if (photoFile.exists()) {
                fileDeleteResult = photoFile.delete()
            }
        }

        val myPhotoDeleteResult = myPhotoDao.deleteById(id) > 0
        val tempFileDeleteResult = deleteTempFileById(id)

        return myPhotoDeleteResult && tempFileDeleteResult && fileDeleteResult
    }

    fun deletePhotoById(photoId: Long) {
        myPhotoDao.deleteById(photoId)
    }

    fun deleteAllWithState(photoState: PhotoState): Boolean {
        return database.transactional {
            val myPhotosList = myPhotoDao.findAllWithState(photoState)

            for (myPhoto in myPhotosList) {
                if (!deleteById(myPhoto.id!!)) {
                    return@transactional false
                }
            }

            return@transactional true
        }
    }

    private fun createTempFile(): TempFileEntity {
        val file = createFile()
        if (!file.exists()) {
            return TempFileEntity.empty()
        }

        try {
            val tempFileEntity = TempFileEntity.create(file.absolutePath)
            val id = tempFileDao.insert(tempFileEntity)

            if (id <= 0L) {
                deleteFileIfExists(file)
                return TempFileEntity.empty()
            }

            return tempFileEntity
                .also { it.id = id }
        } catch (error: Throwable) {
            Timber.e(error)
            deleteFileIfExists(file)
            return TempFileEntity.empty()
        }
    }

    private fun findTempFileById(id: Long): TempFileEntity {
        return tempFileDao.findById(id) ?: TempFileEntity.empty()
    }

    private fun findAllTempFiles(): List<TempFileEntity> {
        return tempFileDao.findAll()
    }

    fun deleteTempFileById(id: Long): Boolean {
        val tempFileEntity = tempFileDao.findById(id)
            ?: return true

        deleteFileIfExists(File(tempFileEntity.filePath))
        return tempFileDao.deleteById(id) > 0
    }

    private fun createTempFilesDirIfNotExists() {
        val fullPathFile = File(filesDir)
        Timber.d(fullPathFile.absolutePath)

        if (!fullPathFile.exists()) {
            if (!fullPathFile.mkdirs()) {
                Timber.e("Could not create directory ${fullPathFile.absolutePath}")
            }
        }
    }

    private fun deleteFileIfExists(file: File) {
        if (file.exists()) {
            file.delete()
        }
    }
}