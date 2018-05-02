package com.kirakishou.photoexchange.helper.database.repository

import com.kirakishou.photoexchange.helper.database.MyDatabase
import com.kirakishou.photoexchange.helper.database.entity.MyPhotoEntity
import com.kirakishou.photoexchange.helper.database.entity.TempFileEntity
import com.kirakishou.photoexchange.helper.database.mapper.MyPhotoEntityMapper
import com.kirakishou.photoexchange.mvp.model.MyPhoto
import com.kirakishou.photoexchange.mvp.model.PhotoState
import timber.log.Timber
import java.io.File
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

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
    private val lock = ReentrantLock()

    init {
        createTempFilesDirIfNotExists()
    }

    fun saveTakenPhoto(file: File): MyPhoto {
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

    fun updateMakePhotoPublic(takenPhotoId: Long): Boolean {
        return myPhotoDao.updateSetPhotoPublic(takenPhotoId) == 1
    }

    fun deleteMyPhoto(myPhoto: MyPhoto?): Boolean {
        if (myPhoto == null) {
            return true
        }

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

    fun findPhotosByStateAndUpdateState(oldState: PhotoState, newState: PhotoState): List<MyPhoto> {
        return lock.withLock {
            var photos = mutableListOf<MyPhotoEntity>()

            database.transactional {
                photos = myPhotoDao.findOnePhotoWithState(oldState)
                if (photos.isEmpty()) {
                    return@transactional false
                }

                for (photo in photos) {
                    if (myPhotoDao.updateSetNewPhotoState(photo.id!!, newState) != 1) {
                        return@transactional false
                    }
                }

                return@transactional true
            }

            for (photo in photos) {
                photo.id?.let { photoId ->
                    val tempFileEntity = findTempFileById(photoId)
                    return@let MyPhotoEntityMapper.toMyPhoto(photo, tempFileEntity)
                }
            }
            return@withLock photos.asSequence()
                .filter { it.id != null }
                .map { photo ->
                    val tempFileEntity = findTempFileById(photo.id!!)
                    MyPhotoEntityMapper.toMyPhoto(photo, tempFileEntity)
                }
                .toList()
        }
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

    fun findByPhotoIdByName(photoName: String): Long {
        return myPhotoDao.findPhotoIdByName(photoName) ?: -1L
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

    private fun findTempFileById(id: Long): TempFileEntity {
        return tempFileDao.findById(id) ?: TempFileEntity.empty()
    }

    fun deleteTempFileById(id: Long): Boolean {
        val tempFileEntity = tempFileDao.findById(id)
            ?: return true

        deleteFileIfExists(File(tempFileEntity.filePath))
        return tempFileDao.deleteById(id) > 0
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

        for (filePath in filePaths) {
            if (filePath.name.startsWith(".")) {
                continue
            }

            val found = tempFileDao.findByFilePath(filePath.absolutePath) != null
            if (!found) {
                deleteFileIfExists(filePath)
            }
        }
    }
}