package com.kirakishou.photoexchange.interactors

import com.kirakishou.photoexchange.helper.api.ApiClient
import com.kirakishou.photoexchange.helper.database.MyDatabase
import com.kirakishou.photoexchange.helper.database.repository.PhotosRepository
import com.kirakishou.photoexchange.helper.util.BitmapUtils
import com.kirakishou.photoexchange.helper.util.FileUtils
import com.kirakishou.photoexchange.mvp.model.PhotoState
import com.kirakishou.photoexchange.mvp.model.PhotoUploadingEvent
import com.kirakishou.photoexchange.mvp.model.other.LonLat
import com.kirakishou.photoexchange.mvp.model.other.ServerErrorCode
import com.kirakishou.photoexchange.service.UploadPhotoServiceCallbacks
import timber.log.Timber
import java.io.File
import java.lang.ref.WeakReference
import java.util.concurrent.atomic.AtomicBoolean

class UploadPhotosUseCase(
    private val database: MyDatabase,
    private val myPhotosRepository: PhotosRepository,
    private val apiClient: ApiClient
) {
    fun uploadPhotos(userId: String, location: LonLat, callbacks: WeakReference<UploadPhotoServiceCallbacks>?) {
        while (true) {
            val photo = myPhotosRepository.findPhotoByStateAndUpdateState(PhotoState.PHOTO_QUEUED_UP, PhotoState.PHOTO_UPLOADING)
                ?: break

            try {
                val queuedUpPhotosCount = myPhotosRepository.countAllByState(PhotoState.PHOTO_QUEUED_UP).toInt()
                callbacks?.get()?.onUploadingEvent(PhotoUploadingEvent.OnPhotoUploadingStart(photo, queuedUpPhotosCount))

                val rotatedPhotoFile = File.createTempFile("rotated_photo", ".tmp")

                try {
                    if (BitmapUtils.rotatePhoto(photo.photoTempFile, rotatedPhotoFile)) {
                        val response = apiClient.uploadPhoto(photo.id, rotatedPhotoFile.absolutePath, location, userId, callbacks).blockingGet()
                        val errorCode = ServerErrorCode.from(response.serverErrorCode)

                        if (errorCode == ServerErrorCode.OK) {
                            photo.photoState = PhotoState.PHOTO_UPLOADED
                            photo.photoName = response.photoName
                            photo.photoTempFile = null
                            callbacks?.get()?.onUploadingEvent(PhotoUploadingEvent.OnUploaded(photo))

                            val isAllOk =  database.transactional {
                                val deleteResult = myPhotosRepository.deleteTempFileById(photo.id)
                                val updateResult1 = myPhotosRepository.updatePhotoState(photo.id, PhotoState.PHOTO_UPLOADED)
                                val updateResult2 = myPhotosRepository.updateSetTempFileId(photo.id, null)
                                val updateResult3 = myPhotosRepository.updateSetPhotoName(photo.id, response.photoName)

                                return@transactional deleteResult && updateResult1 && updateResult2 && updateResult3
                            }

                            if (isAllOk) {
                                continue
                            }
                        }
                    }
                } finally {
                    FileUtils.deleteFile(rotatedPhotoFile)
                }

                myPhotosRepository.updatePhotoState(photo.id, PhotoState.FAILED_TO_UPLOAD)
                photo.photoState = PhotoState.FAILED_TO_UPLOAD

                callbacks?.get()?.onUploadingEvent(PhotoUploadingEvent.OnFailedToUpload(photo))
            } catch (error: Throwable) {
                Timber.e(error)

                myPhotosRepository.updatePhotoState(photo.id, PhotoState.FAILED_TO_UPLOAD)
                photo.photoState = PhotoState.FAILED_TO_UPLOAD

                callbacks?.get()?.onUploadingEvent(PhotoUploadingEvent.OnFailedToUpload(photo))
            }
        }
    }
}