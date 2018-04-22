package com.kirakishou.photoexchange.interactors

import com.kirakishou.photoexchange.helper.api.ApiClient
import com.kirakishou.photoexchange.helper.database.MyDatabase
import com.kirakishou.photoexchange.helper.database.repository.PhotosRepository
import com.kirakishou.photoexchange.helper.util.BitmapUtils
import com.kirakishou.photoexchange.helper.util.FileUtils
import com.kirakishou.photoexchange.mvp.model.MyPhoto
import com.kirakishou.photoexchange.mvp.model.PhotoState
import com.kirakishou.photoexchange.mvp.model.PhotoUploadEvent
import com.kirakishou.photoexchange.mvp.model.net.response.UploadPhotoResponse
import com.kirakishou.photoexchange.mvp.model.other.LonLat
import com.kirakishou.photoexchange.mvp.model.other.ErrorCode
import com.kirakishou.photoexchange.service.UploadPhotoServiceCallbacks
import io.reactivex.Observable
import io.reactivex.Single
import timber.log.Timber
import java.io.File
import java.lang.ref.WeakReference

class UploadPhotosUseCase(
    private val database: MyDatabase,
    private val myPhotosRepository: PhotosRepository,
    private val apiClient: ApiClient
) {
    fun uploadPhotos(userId: String, location: LonLat, callbacks: WeakReference<UploadPhotoServiceCallbacks>?): Observable<Unit> {
        return Observable.fromCallable {
            while (true) {
                val photo = myPhotosRepository.findPhotoByStateAndUpdateState(PhotoState.PHOTO_QUEUED_UP, PhotoState.PHOTO_UPLOADING)
                    ?: break

                try {
                    val rotatedPhotoFile = handlePhotoUploadingStart(callbacks, photo)

                    try {
                        if (BitmapUtils.rotatePhoto(photo.photoTempFile, rotatedPhotoFile)) {
                            val response = uploadPhoto(rotatedPhotoFile, location, userId, callbacks, photo).blockingGet()
                            val errorCode = response.errorCode as ErrorCode.UploadPhotoErrors
                            when (errorCode) {
                                is ErrorCode.UploadPhotoErrors.Remote.Ok -> {
                                    if (!handlePhotoUploaded(photo, response, callbacks)) {
                                        handleFailedPhoto(photo, callbacks, errorCode)
                                    }
                                }

                                else -> handleFailedPhoto(photo, callbacks, errorCode)
                            }
                        }
                    } finally {
                        FileUtils.deleteFile(rotatedPhotoFile)
                    }
                } catch (error: Exception) {
                    Timber.e(error)
                    handleUnknownError(photo, callbacks, error)
                }
            }
        }
    }

    private fun handlePhotoUploadingStart(callbacks: WeakReference<UploadPhotoServiceCallbacks>?, photo: MyPhoto): File {
        val rotatedPhotoFile = File.createTempFile("rotated_photo", ".tmp")
        callbacks?.get()?.onUploadingEvent(PhotoUploadEvent.OnPhotoUploadStart(photo))
        return rotatedPhotoFile
    }

    private fun uploadPhoto(rotatedPhotoFile: File, location: LonLat, userId: String, callbacks: WeakReference<UploadPhotoServiceCallbacks>?, photo: MyPhoto): Single<UploadPhotoResponse> {
        return apiClient.uploadPhoto(rotatedPhotoFile.absolutePath, location, userId, object : PhotoUploadProgressCallback {
            override fun onProgress(progress: Int) {
                callbacks?.get()?.onUploadingEvent(PhotoUploadEvent.OnProgress(photo, progress))
            }
        })
    }

    private fun handlePhotoUploaded(photo: MyPhoto, response: UploadPhotoResponse, callbacks: WeakReference<UploadPhotoServiceCallbacks>?): Boolean {
        photo.photoState = PhotoState.PHOTO_UPLOADED
        photo.photoName = response.photoName
        photo.photoTempFile = null

        val dbResult = database.transactional {
            val deleteResult = myPhotosRepository.deleteTempFileById(photo.id)
            val updateResult1 = myPhotosRepository.updatePhotoState(photo.id, PhotoState.PHOTO_UPLOADED)
            val updateResult2 = myPhotosRepository.updateSetTempFileId(photo.id, null)
            val updateResult3 = myPhotosRepository.updateSetPhotoName(photo.id, response.photoName)

            return@transactional deleteResult && updateResult1 && updateResult2 && updateResult3
        }

        if (dbResult) {
            callbacks?.get()?.onUploadingEvent(PhotoUploadEvent.OnUploaded(photo))
        }

        return dbResult
    }

    private fun handleFailedPhoto(photo: MyPhoto, callbacks: WeakReference<UploadPhotoServiceCallbacks>?, errorCode: ErrorCode.UploadPhotoErrors) {
        myPhotosRepository.updatePhotoState(photo.id, PhotoState.FAILED_TO_UPLOAD)
        photo.photoState = PhotoState.FAILED_TO_UPLOAD

        callbacks?.get()?.onUploadingEvent(PhotoUploadEvent.OnFailedToUpload(photo, errorCode))
    }

    private fun handleUnknownError(photo: MyPhoto, callbacks: WeakReference<UploadPhotoServiceCallbacks>?, error: Throwable) {
        myPhotosRepository.updatePhotoState(photo.id, PhotoState.FAILED_TO_UPLOAD)
        photo.photoState = PhotoState.FAILED_TO_UPLOAD

        callbacks?.get()?.onUploadingEvent(PhotoUploadEvent.OnUnknownError(error))
    }

    interface PhotoUploadProgressCallback {
        fun onProgress(progress: Int)
    }
}