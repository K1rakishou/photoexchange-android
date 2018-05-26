package com.kirakishou.photoexchange.interactors

import com.kirakishou.photoexchange.helper.api.ApiClient
import com.kirakishou.photoexchange.helper.database.MyDatabase
import com.kirakishou.photoexchange.helper.database.repository.TakenPhotosRepository
import com.kirakishou.photoexchange.helper.database.repository.UploadedPhotosRepository
import com.kirakishou.photoexchange.helper.util.BitmapUtils
import com.kirakishou.photoexchange.helper.util.FileUtils
import com.kirakishou.photoexchange.helper.util.TimeUtils
import com.kirakishou.photoexchange.mvp.model.PhotoState
import com.kirakishou.photoexchange.mvp.model.PhotoUploadEvent
import com.kirakishou.photoexchange.mvp.model.TakenPhoto
import com.kirakishou.photoexchange.mvp.model.UploadedPhoto
import com.kirakishou.photoexchange.mvp.model.net.response.UploadPhotoResponse
import com.kirakishou.photoexchange.mvp.model.other.ErrorCode
import com.kirakishou.photoexchange.mvp.model.other.LonLat
import com.kirakishou.photoexchange.service.UploadPhotoServiceCallbacks
import io.reactivex.Single
import kotlinx.coroutines.experimental.rx2.await
import kotlinx.coroutines.experimental.rx2.rxSingle
import kotlinx.coroutines.experimental.sync.Mutex
import kotlinx.coroutines.experimental.sync.withLock
import timber.log.Timber
import java.io.File
import java.lang.ref.WeakReference

class UploadPhotosUseCase(
    private val database: MyDatabase,
    private val takenPhotosRepository: TakenPhotosRepository,
    private val uploadedPhotosRepository: UploadedPhotosRepository,
    private val apiClient: ApiClient
) {
    private val TAG = "UploadPhotosUseCase"
    private val mutex = Mutex()

    fun uploadPhotos(userId: String, location: LonLat, callbacks: WeakReference<UploadPhotoServiceCallbacks>?): Single<Boolean> {
        return rxSingle {
            return@rxSingle mutex.withLock {
                Timber.tag(TAG).d("Start photo uploading")

                val photosToUpload = takenPhotosRepository.findPhotosByStateAndUpdateState(PhotoState.PHOTO_QUEUED_UP, PhotoState.PHOTO_UPLOADING)
                if (photosToUpload.isEmpty()) {
                    return@withLock true
                }

                val results = arrayListOf<Boolean>()

                for (photo in photosToUpload) {
                    val rotatedPhotoFile = handlePhotoUploadingStart(callbacks, photo)

                    try {
                        if (photo.photoTempFile == null || !photo.photoTempFile!!.exists()) {
                            Timber.tag(TAG).e("Photo does not exists on disk! ${photo.photoTempFile?.absoluteFile ?: "(No photoTempFile)"}")
                            continue
                        }

                        if (BitmapUtils.rotatePhoto(photo.photoTempFile!!, rotatedPhotoFile)) {
                            Timber.tag(TAG).d("Photo rotated. Starting the uploading routine...")

                            val response = try {
                                uploadPhoto(rotatedPhotoFile, location, userId, photo.isPublic, callbacks, photo).await()
                            } catch (error: RuntimeException) {
                                if (error.cause == null && error.cause !is InterruptedException) {
                                    throw error
                                }

                                UploadPhotoResponse.error(ErrorCode.UploadPhotoErrors.LocalInterrupted())
                            }

                            val errorCode = response.errorCode as ErrorCode.UploadPhotoErrors
                            results += errorCode is ErrorCode.UploadPhotoErrors.Ok

                            when (errorCode) {
                                is ErrorCode.UploadPhotoErrors.Ok -> {
                                    Timber.tag(TAG).d("Photo uploaded. Saving the result")

                                    if (!handlePhotoUploaded(photo, location, response, callbacks)) {
                                        Timber.tag(TAG).d("Could not save photo uploading result")
                                        handleFailedPhoto(photo, callbacks, errorCode)
                                    }
                                }

                                is ErrorCode.UploadPhotoErrors.LocalInterrupted -> {
                                    Timber.tag(TAG).d("Interrupted exception")
                                    handleFailedPhoto(photo, callbacks, errorCode)
                                }

                                else -> {
                                    Timber.tag(TAG).d("Could not upload photo with photoId ${photo.id}")
                                    handleFailedPhoto(photo, callbacks, errorCode)
                                }
                            }
                        } else {
                            Timber.tag(TAG).d("Could not rotate photo with photoId ${photo.id}")
                        }
                    } catch (error: Exception) {
                        Timber.tag(TAG).e(error)

                        results += false
                        handleUnknownError(photo, callbacks, error)
                    } finally {
                        FileUtils.deleteFile(rotatedPhotoFile)
                    }
                }

                return@withLock results.none { !it }
            }
        }
    }

    private fun handlePhotoUploadingStart(callbacks: WeakReference<UploadPhotoServiceCallbacks>?, photo: TakenPhoto): File {
        val rotatedPhotoFile = File.createTempFile("rotated_photo", ".tmp")
        callbacks?.get()?.onUploadingEvent(PhotoUploadEvent.OnPhotoUploadStart(photo))
        return rotatedPhotoFile
    }

    private fun uploadPhoto(rotatedPhotoFile: File, location: LonLat, userId: String, isPublic: Boolean, callbacks: WeakReference<UploadPhotoServiceCallbacks>?, photo: TakenPhoto): Single<UploadPhotoResponse> {
        return apiClient.uploadPhoto(rotatedPhotoFile.absolutePath, location, userId, isPublic, object : PhotoUploadProgressCallback {
            override fun onProgress(progress: Int) {
                callbacks?.get()?.onUploadingEvent(PhotoUploadEvent.OnProgress(photo, progress))
            }
        })
    }

    private fun handlePhotoUploaded(photo: TakenPhoto, location: LonLat,
                                    response: UploadPhotoResponse, callbacks: WeakReference<UploadPhotoServiceCallbacks>?): Boolean {
        photo.photoName = response.photoName

        val dbResult = database.transactional {
            val updateResult1 = takenPhotosRepository.deletePhotoById(photo.id)
            val updateResult2 = uploadedPhotosRepository.save(photo, location.lon, location.lat, TimeUtils.getTimeFast())

            Timber.tag(TAG).d("updateResult1 = $updateResult1, updateResult2 = $updateResult2")
            return@transactional updateResult1 && updateResult2
        }

        if (dbResult) {
            callbacks?.get()?.onUploadingEvent(PhotoUploadEvent.OnUploaded(UploadedPhoto(photo.id, photo.photoName!!,
                location.lon, location.lat, false, TimeUtils.getTimeFast())))
        }

        return dbResult
    }

    private fun handleFailedPhoto(photo: TakenPhoto, callbacks: WeakReference<UploadPhotoServiceCallbacks>?, errorCode: ErrorCode.UploadPhotoErrors) {
        takenPhotosRepository.updatePhotoState(photo.id, PhotoState.FAILED_TO_UPLOAD)
        photo.photoState = PhotoState.FAILED_TO_UPLOAD

        callbacks?.get()?.onUploadingEvent(PhotoUploadEvent.OnFailedToUpload(photo, errorCode))
    }

    private fun handleUnknownError(photo: TakenPhoto, callbacks: WeakReference<UploadPhotoServiceCallbacks>?, error: Throwable) {
        takenPhotosRepository.updatePhotoState(photo.id, PhotoState.FAILED_TO_UPLOAD)
        photo.photoState = PhotoState.FAILED_TO_UPLOAD

        callbacks?.get()?.onUploadingEvent(PhotoUploadEvent.OnUnknownError(error))
    }

    interface PhotoUploadProgressCallback {
        fun onProgress(progress: Int)
    }
}