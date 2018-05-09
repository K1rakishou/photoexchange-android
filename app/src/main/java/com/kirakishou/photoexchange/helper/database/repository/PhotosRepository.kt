package com.kirakishou.photoexchange.helper.database.repository

import com.kirakishou.photoexchange.helper.database.MyDatabase
import com.kirakishou.photoexchange.helper.database.entity.MyPhotoEntity
import com.kirakishou.photoexchange.helper.database.entity.TempFileEntity
import com.kirakishou.photoexchange.helper.database.isFail
import com.kirakishou.photoexchange.helper.database.mapper.MyPhotoMapper
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
    private val tag = "PhotosRepository"
    private val myPhotoDao = database.myPhotoDao()
    private val tempFileDao = database.tempFileDao()

    init {
        createTempFilesDirIfNotExists()
    }

    fun saveTakenPhoto(file: File): MyPhoto {
        var photo = MyPhoto.empty()

        database.transactional {
            val tempFileEntity = TempFileEntity.create(file.absolutePath)
            val tempFileId = tempFileDao.insert(tempFileEntity)
            if (tempFileId <= 0L) {
                photo = MyPhoto.empty()
                return@transactional false
            }

            val myPhotoEntity = MyPhotoEntity.create(tempFileId, false)
            val myPhotoId = myPhotoDao.insert(myPhotoEntity)

            myPhotoEntity.id = myPhotoId
            photo = MyPhotoMapper.toMyPhoto(myPhotoEntity, tempFileEntity)

            return@transactional true
        }

        return photo
    }

    fun updateSetPhotoName(photoId: Long, photoName: String): Boolean {
        return database.transactional {
            myPhotoDao.updateSetPhotoName(photoId, photoName) == 1
        }
    }

    fun updateSetTempFileId(photoId: Long, newTempFileId: Long?): Boolean {
        return database.transactional {
            myPhotoDao.updateSetTempFileId(photoId, newTempFileId) == 1
        }
    }

    fun updatePhotoState(photoId: Long, newPhotoState: PhotoState): Boolean {
        return database.transactional {
            myPhotoDao.updateSetNewPhotoState(photoId, newPhotoState) == 1
        }
    }

    fun updatePhotoState(photoName: String, newPhotoState: PhotoState): Boolean {
        return database.transactional {
            val photoId = myPhotoDao.findByName(photoName)?.id
                ?: return@transactional false

            return@transactional myPhotoDao.updateSetNewPhotoState(photoId, newPhotoState) == 1
        }
    }

    fun updateMakePhotoPublic(takenPhotoId: Long): Boolean {
        return database.transactional {
            return myPhotoDao.updateSetPhotoPublic(takenPhotoId) == 1
        }
    }

    fun createFile(): File {
        val fullPathFile = File(filesDir)
        return File.createTempFile("file_", ".tmp", fullPathFile)
    }

    fun findById(id: Long): MyPhoto {
        var photo = MyPhoto.empty()

        database.transactional {
            val myPhotoEntity = myPhotoDao.findById(id) ?: MyPhotoEntity.empty()
            val tempFileEntity = findTempFileById(id)

            photo = MyPhotoMapper.toMyPhoto(myPhotoEntity, tempFileEntity)
            return@transactional true
        }

        return photo
    }

    fun findAll(): List<MyPhoto> {
        var photos = emptyList<MyPhoto>()

        database.transactional {
            val allMyPhotos = arrayListOf<MyPhoto>()
            val allMyPhotoEntities = myPhotoDao.findAll()

            for (myPhotoEntity in allMyPhotoEntities) {
                myPhotoEntity.id?.let { myPhotoId ->
                    val tempFile = findTempFileById(myPhotoId)

                    MyPhotoMapper.toMyPhoto(myPhotoEntity, tempFile).let { myPhoto ->
                        allMyPhotos += myPhoto
                    }
                }
            }

            photos = allMyPhotos
            return@transactional true
        }

        return photos
    }

    fun countAllByState(state: PhotoState): Int {
        var count = 0

        database.transactional {
            count = myPhotoDao.countAllByState(state).toInt()
            return@transactional true
        }

        return count
    }

    fun countAllByStates(states: Array<PhotoState>): Int {
        var count = 0

        database.transactional {
            count = myPhotoDao.countAllByStates(states)
            return@transactional true
        }

        return count
    }

    fun updateStates(oldState: PhotoState, newState: PhotoState) {
        myPhotoDao.updateStates(oldState, newState)
    }

    fun findPhotosByStateAndUpdateState(oldState: PhotoState, newState: PhotoState): List<MyPhoto> {
        var resultPhotos = emptyList<MyPhoto>()

        database.transactional {
            val photos = myPhotoDao.findOnePhotoWithState(oldState)
            if (photos.isEmpty()) {
                return@transactional false
            }

            for (photo in photos) {
                if (myPhotoDao.updateSetNewPhotoState(photo.id!!, newState) != 1) {
                    return@transactional false
                }
            }

            for (photo in photos) {
                photo.id?.let { photoId ->
                    val tempFileEntity = findTempFileById(photoId)
                    return@let MyPhotoMapper.toMyPhoto(photo, tempFileEntity)
                }
            }

            resultPhotos = photos.asSequence()
                .filter { it.id != null }
                .map { photo ->
                    val tempFileEntity = findTempFileById(photo.id!!)
                    MyPhotoMapper.toMyPhoto(photo, tempFileEntity)
                }
                .toList()

            return@transactional true
        }

        return resultPhotos
    }

    fun findAllByState(state: PhotoState): List<MyPhoto> {
        val resultList = mutableListOf<MyPhoto>()

        database.transactional {
            val allPhotoReadyToUploading = myPhotoDao.findAllWithState(state)

            for (photo in allPhotoReadyToUploading) {
                val tempFileEntity = findTempFileById(photo.id!!)
                resultList += MyPhotoMapper.toMyPhoto(photo, tempFileEntity)
            }

            return@transactional true
        }

        return resultList
    }

    fun findByPhotoIdByName(photoName: String): Long {
        var photoId = -1L

        database.transactional {
            photoId = myPhotoDao.findPhotoIdByName(photoName) ?: -1L
            return@transactional true
        }

        return photoId
    }

    fun deleteMyPhoto(myPhoto: MyPhoto?): Boolean {
        if (myPhoto == null) {
            return true
        }

        if (myPhoto.isEmpty()) {
            return true
        }

        return deletePhotoById(myPhoto.id)
    }

    fun deletePhotoById(photoId: Long): Boolean {
        return database.transactional {
            if (myPhotoDao.deleteById(photoId).isFail()) {
                return@transactional false
            }

            return@transactional deleteTempFileById(photoId)
        }
    }

    fun deleteAllWithState(photoState: PhotoState): Boolean {
        return database.transactional {
            val myPhotosList = myPhotoDao.findAllWithState(photoState)

            for (myPhoto in myPhotosList) {
                if (!deletePhotoById(myPhoto.id!!)) {
                    return@transactional false
                }
            }

            return@transactional true
        }
    }

    fun deleteTempFileById(id: Long): Boolean {
        return database.transactional {
            val tempFileEntity = tempFileDao.findById(id)
                ?: return@transactional true

            if (tempFileDao.deleteById(id).isFail()) {
                return@transactional false
            }

            deleteFileIfExists(File(tempFileEntity.filePath))
            return@transactional true
        }
    }

    private fun findTempFileById(id: Long): TempFileEntity {
        var tempFileEntity = TempFileEntity.empty()

        database.transactional {
            tempFileEntity = tempFileDao.findById(id) ?: TempFileEntity.empty()
            return@transactional true
        }

        return tempFileEntity
    }

    private fun createTempFilesDirIfNotExists() {
        val fullPathFile = File(filesDir)
        if (!fullPathFile.exists()) {
            if (!fullPathFile.mkdirs()) {
                Timber.tag(tag).w("Could not create directory ${fullPathFile.absolutePath}")
            }
        }
    }

    fun deleteFileIfExists(file: File?) {
        if (file == null) {
            return
        }

        if (file.exists()) {
            if (!file.delete()) {
                Timber.tag(tag).w("Could not delete file path: ${file.absoluteFile}")
            }
        }
    }

    fun cleanFilesDirectory() {
        val directory = File(filesDir)
        val filePaths = directory.listFiles()

        database.transactional {
            for (filePath in filePaths) {
                if (filePath.name.startsWith(".")) {
                    continue
                }

                val found = tempFileDao.findByFilePath(filePath.absolutePath) != null
                if (!found) {
                    deleteFileIfExists(filePath)
                }
            }

            return@transactional true
        }
    }
}