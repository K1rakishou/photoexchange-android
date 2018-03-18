package com.kirakishou.photoexchange.helper.database.repository

import com.kirakishou.photoexchange.helper.api.ApiClient
import com.kirakishou.photoexchange.helper.database.MyDatabase
import com.kirakishou.photoexchange.helper.database.entity.MyPhotoEntity
import com.kirakishou.photoexchange.helper.database.entity.TempFileEntity
import com.kirakishou.photoexchange.helper.database.mapper.MyPhotoMapper
import com.kirakishou.photoexchange.helper.util.BitmapUtils
import com.kirakishou.photoexchange.helper.util.FileUtils
import com.kirakishou.photoexchange.mvp.model.MyPhoto
import com.kirakishou.photoexchange.mvp.model.PhotoState
import com.kirakishou.photoexchange.mvp.model.PhotoUploadingEvent
import com.kirakishou.photoexchange.mvp.model.other.LonLat
import com.kirakishou.photoexchange.mvp.model.other.ServerErrorCode
import com.kirakishou.photoexchange.service.UploadPhotoServiceCallbacks
import timber.log.Timber
import java.io.File
import java.lang.ref.WeakReference

/**
 * Created by kirakishou on 3/3/2018.
 */
open class PhotosRepository(
    private val filesDir: String,
    private val database: MyDatabase,
    private val apiClient: ApiClient
) {
    private val myPhotoDao = database.myPhotoDao()
    private val tempFileDao = database.tempFileDao()

    fun init() {
        createTempFilesDirIfNotExists()
    }

    suspend fun uploadPhotos(userId: String, location: LonLat, callbacks: WeakReference<UploadPhotoServiceCallbacks>?) {
        if (callbacks == null) {
            return
        }

        try {
            val photos = findAllByState(PhotoState.PHOTO_TO_BE_UPLOADED)
            callbacks.get()?.onUploadingEvent(PhotoUploadingEvent.onPrepare())

            for (photo in photos) {
                try {
                    updatePhotoState(photo.id, PhotoState.PHOTO_UPLOADING)
                    callbacks.get()?.onUploadingEvent(PhotoUploadingEvent.onPhotoUploadingStart(photo))

                    val rotatedPhotoFile = File.createTempFile("rotated_photo", ".tmp")

                    try {
                        if (BitmapUtils.rotatePhoto(photo.photoTempFile!!.absolutePath, rotatedPhotoFile)) {
                            val response = apiClient.uploadPhoto(photo.id, rotatedPhotoFile.absolutePath, location, userId, callbacks).await()
                            val errorCode = ServerErrorCode.from(response.serverErrorCode)

                            if (errorCode == ServerErrorCode.OK) {
                                var isAllOk = false

                                database.transactional {
                                    val deleteResult = deleteTempFileById(photo.id)
                                    val updateResult1 = updatePhotoState(photo.id, PhotoState.PHOTO_UPLOADED)
                                    val updateResult2 = updateSetTempFileId(photo.id, null)
                                    val updateResult3 = updateSetPhotoName(photo.id, response.photoName)

                                    isAllOk = deleteResult && updateResult1 && updateResult2 && updateResult3
                                    return@transactional isAllOk
                                }

                                if (isAllOk) {
                                    photo.photoState = PhotoState.PHOTO_UPLOADED
                                    photo.photoName = response.photoName
                                    photo.photoTempFile = null

                                    callbacks.get()?.onUploadingEvent(PhotoUploadingEvent.onUploaded(photo))
                                    continue
                                }
                            }
                        }
                    } finally {
                        FileUtils.deleteFile(rotatedPhotoFile)
                    }

                    updatePhotoState(photo.id, PhotoState.PHOTO_TO_BE_UPLOADED)
                    callbacks.get()?.onUploadingEvent(PhotoUploadingEvent.onFailedToUpload(photo))
                } catch (error: Throwable) {
                    Timber.e(error)
                    updatePhotoState(photo.id, PhotoState.PHOTO_TO_BE_UPLOADED)
                    callbacks.get()?.onUploadingEvent(PhotoUploadingEvent.onFailedToUpload(photo))
                }
            }

            callbacks.get()?.onUploadingEvent(PhotoUploadingEvent.onEnd())
        } catch (error: Throwable) {
            Timber.e(error)
            callbacks.get()?.onUploadingEvent(PhotoUploadingEvent.onUnknownError())
        }
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
        return MyPhotoMapper.toMyPhoto(myPhotoEntity, tempFileEntity)
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

    private fun findById(id: Long): MyPhoto {
        val myPhotoEntity = myPhotoDao.findById(id) ?: MyPhotoEntity.empty()
        val tempFileEntity = findTempFileById(id)

        return MyPhotoMapper.toMyPhoto(myPhotoEntity, tempFileEntity)
    }

    private fun findAll(): List<MyPhoto> {
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

        return allMyPhotos
    }

    fun countAllByState(state: PhotoState): Long {
        return myPhotoDao.countAllByState(state)
    }

    fun findAllByState(oldState: PhotoState): List<MyPhoto> {
        val allPhotoReadyToUploading = myPhotoDao.findAllWithState(oldState)
        val resultList = mutableListOf<MyPhoto>()

        database.transactional {
            for (photo in allPhotoReadyToUploading) {
                val tempFileEntity = findTempFileById(photo.id!!)
                if (tempFileEntity.isEmpty()) {
                    return@transactional false
                }

                resultList += MyPhotoMapper.toMyPhoto(photo, tempFileEntity)
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

    //TODO: make faster by deleting entities in batches
    private fun deleteAllWithState(photoState: PhotoState): Boolean {
        val myPhotosList = myPhotoDao.findAllWithState(photoState)

        for (myPhoto in myPhotosList) {
            if (!deleteById(myPhoto.id!!)) {
                return false
            }
        }

        return true
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

    private fun deleteTempFileById(id: Long): Boolean {
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