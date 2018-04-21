package com.kirakishou.photoexchange.interactors

import com.kirakishou.photoexchange.helper.api.ApiClient
import com.kirakishou.photoexchange.helper.database.MyDatabase
import com.kirakishou.photoexchange.helper.database.repository.PhotosRepository
import com.kirakishou.photoexchange.helper.util.BitmapUtils
import com.kirakishou.photoexchange.helper.util.FileUtils
import com.kirakishou.photoexchange.mvp.model.MyPhoto
import com.kirakishou.photoexchange.mvp.model.PhotoState
import com.kirakishou.photoexchange.mvp.model.PhotoUploadingEvent
import com.kirakishou.photoexchange.mvp.model.net.response.UploadPhotoResponse
import com.kirakishou.photoexchange.mvp.model.other.LonLat
import com.kirakishou.photoexchange.mvp.model.other.ErrorCode
import com.kirakishou.photoexchange.service.UploadPhotoServiceCallbacks
import io.reactivex.Single
import timber.log.Timber
import java.io.File
import java.lang.ref.WeakReference

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
                val rotatedPhotoFile = handlePhotoUploadingStart(callbacks, photo)

                try {
                    if (BitmapUtils.rotatePhoto(photo.photoTempFile, rotatedPhotoFile)) {
                        val response = uploadPhoto(rotatedPhotoFile, location, userId, callbacks, photo).blockingGet()
                        val errorCode = ErrorCode.fromInt<ErrorCode.UploadPhotoErrors>(response.serverErrorCode)
                        when (errorCode) {
                            is ErrorCode.UploadPhotoErrors.Ok -> {
                                if (!handlePhotoUploaded(photo, response, callbacks)) {
                                    handleFailedPhoto(photo, callbacks)
                                }
                            }

                            else -> handleFailedPhoto(photo, callbacks)
                        }
                    }
                } finally {
                    FileUtils.deleteFile(rotatedPhotoFile)
                }
            } catch (error: Throwable) {
                Timber.e(error)
                handleFailedPhoto(photo, callbacks)
            }
        }
    }

    private fun handlePhotoUploadingStart(callbacks: WeakReference<UploadPhotoServiceCallbacks>?, photo: MyPhoto): File {
        val rotatedPhotoFile = File.createTempFile("rotated_photo", ".tmp")
        callbacks?.get()?.onUploadingEvent(PhotoUploadingEvent.OnPhotoUploadingStart(photo))
        return rotatedPhotoFile
    }

    private fun uploadPhoto(rotatedPhotoFile: File, location: LonLat, userId: String, callbacks: WeakReference<UploadPhotoServiceCallbacks>?, photo: MyPhoto): Single<UploadPhotoResponse> {
        return apiClient.uploadPhoto(rotatedPhotoFile.absolutePath, location, userId, object : PhotoUploadProgressCallback {
            override fun onProgress(progress: Int) {
                callbacks?.get()?.onUploadingEvent(PhotoUploadingEvent.OnProgress(photo, progress))
            }
        })
    }

    private fun handlePhotoUploaded(photo: MyPhoto, response: UploadPhotoResponse, callbacks: WeakReference<UploadPhotoServiceCallbacks>?): Boolean {
        photo.photoState = PhotoState.PHOTO_UPLOADED
        photo.photoName = response.photoName
        photo.photoTempFile = null
        callbacks?.get()?.onUploadingEvent(PhotoUploadingEvent.OnUploaded(photo))

        return database.transactional {
            val deleteResult = myPhotosRepository.deleteTempFileById(photo.id)
            val updateResult1 = myPhotosRepository.updatePhotoState(photo.id, PhotoState.PHOTO_UPLOADED)
            val updateResult2 = myPhotosRepository.updateSetTempFileId(photo.id, null)
            val updateResult3 = myPhotosRepository.updateSetPhotoName(photo.id, response.photoName)

            return@transactional deleteResult && updateResult1 && updateResult2 && updateResult3
        }
    }

    private fun handleFailedPhoto(photo: MyPhoto, callbacks: WeakReference<UploadPhotoServiceCallbacks>?) {
        myPhotosRepository.updatePhotoState(photo.id, PhotoState.FAILED_TO_UPLOAD)
        photo.photoState = PhotoState.FAILED_TO_UPLOAD

        callbacks?.get()?.onUploadingEvent(PhotoUploadingEvent.OnFailedToUpload(photo))
    }

    interface PhotoUploadProgressCallback {
        fun onProgress(progress: Int)
    }
}