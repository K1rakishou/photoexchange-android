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
import io.reactivex.Maybe
import io.reactivex.Single
import kotlinx.coroutines.experimental.CommonPool
import kotlinx.coroutines.experimental.async
import kotlinx.coroutines.experimental.rx2.asMaybe
import kotlinx.coroutines.experimental.rx2.await
import timber.log.Timber
import java.io.File
import java.lang.ref.WeakReference

class UploadPhotosUseCase(
    private val database: MyDatabase,
    private val myPhotosRepository: PhotosRepository,
    private val apiClient: ApiClient
) {
    private val tag = "UploadPhotosUseCase"

    fun uploadPhotos(userId: String, location: LonLat, callbacks: WeakReference<UploadPhotoServiceCallbacks>?): Maybe<Unit> {
        return async {
            Timber.tag(tag).d("Start photo uploading")

            val photosToUpload = myPhotosRepository.findPhotosByStateAndUpdateState(PhotoState.PHOTO_QUEUED_UP, PhotoState.PHOTO_UPLOADING)
            for (photo in photosToUpload) {
                val rotatedPhotoFile = handlePhotoUploadingStart(callbacks, photo)

                try {
                    if (photo.photoTempFile == null || !photo.photoTempFile!!.exists()) {
                        Timber.tag(tag).e("Photo does not exists on disk! ${photo.photoTempFile?.absoluteFile ?: "(No photoTempFile)"}")
                        continue
                    }

                    if (BitmapUtils.rotatePhoto(photo.photoTempFile!!, rotatedPhotoFile)) {
                        Timber.tag(tag).d("Photo rotated. Starting the uploading routine...")

                        val response = try {
                            uploadPhoto(rotatedPhotoFile, location, userId, photo.isPublic, callbacks, photo).await()
                        } catch (error: RuntimeException) {
                            if (error.cause == null && error.cause !is InterruptedException) {
                                throw error
                            }

                            Timber.tag(tag).d("Interrupted")
                            UploadPhotoResponse.error(ErrorCode.UploadPhotoErrors.Local.Interrupted())
                        }

                        val errorCode = response.errorCode as ErrorCode.UploadPhotoErrors
                        when (errorCode) {
                            is ErrorCode.UploadPhotoErrors.Remote.Ok -> {
                                Timber.tag(tag).d("Photo uploaded. Saving the result")

                                if (!handlePhotoUploaded(photo, response, callbacks)) {
                                    Timber.tag(tag).d("Could not save photo uploading result")
                                    handleFailedPhoto(photo, callbacks, errorCode)
                                }
                            }

                            is ErrorCode.UploadPhotoErrors.Local.Interrupted -> {
                                Timber.tag(tag).d("Interrupted exception")
                                handleFailedPhoto(photo, callbacks, errorCode)
                            }

                            else -> {
                                Timber.tag(tag).d("Could not upload photo with id ${photo.id}")
                                handleFailedPhoto(photo, callbacks, errorCode)
                            }
                        }
                    } else {
                        Timber.tag(tag).d("Could not rotate photo with id ${photo.id}")
                    }
                } catch (error: Exception) {
                    Timber.tag(tag).e(error)
                    handleUnknownError(photo, callbacks, error)
                } finally {
                    FileUtils.deleteFile(rotatedPhotoFile)
                }
            }
        }.asMaybe(CommonPool)
    }

    private fun handlePhotoUploadingStart(callbacks: WeakReference<UploadPhotoServiceCallbacks>?, photo: MyPhoto): File {
        val rotatedPhotoFile = File.createTempFile("rotated_photo", ".tmp")
        callbacks?.get()?.onUploadingEvent(PhotoUploadEvent.OnPhotoUploadStart(photo))
        return rotatedPhotoFile
    }

    private fun uploadPhoto(rotatedPhotoFile: File, location: LonLat, userId: String, isPublic: Boolean, callbacks: WeakReference<UploadPhotoServiceCallbacks>?, photo: MyPhoto): Single<UploadPhotoResponse> {
        return apiClient.uploadPhoto(rotatedPhotoFile.absolutePath, location, userId, isPublic, object : PhotoUploadProgressCallback {
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
            val updateResult1 = myPhotosRepository.updatePhotoState(photo.id, PhotoState.PHOTO_UPLOADED)
            val updateResult2 = myPhotosRepository.updateSetTempFileId(photo.id, null)
            val updateResult3 = myPhotosRepository.updateSetPhotoName(photo.id, response.photoName)

            Timber.tag(tag).d("updateResult1 = $updateResult1, updateResult2 = $updateResult2, updateResult3 = $updateResult3")
            return@transactional updateResult1 && updateResult2 && updateResult3
        }

        if (dbResult) {
            myPhotosRepository.deleteTempFileById(photo.id)
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