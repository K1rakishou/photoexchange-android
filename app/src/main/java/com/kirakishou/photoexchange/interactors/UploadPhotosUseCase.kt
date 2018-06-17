package com.kirakishou.photoexchange.interactors

import com.kirakishou.photoexchange.helper.api.ApiClient
import com.kirakishou.photoexchange.helper.database.MyDatabase
import com.kirakishou.photoexchange.helper.database.repository.TakenPhotosRepository
import com.kirakishou.photoexchange.helper.database.repository.UploadedPhotosRepository
import com.kirakishou.photoexchange.helper.extension.safe
import com.kirakishou.photoexchange.helper.intercom.event.UploadedPhotosFragmentEvent
import com.kirakishou.photoexchange.helper.util.BitmapUtils
import com.kirakishou.photoexchange.helper.util.FileUtils
import com.kirakishou.photoexchange.helper.util.TimeUtils
import com.kirakishou.photoexchange.mvp.model.PhotoState
import com.kirakishou.photoexchange.mvp.model.TakenPhoto
import com.kirakishou.photoexchange.mvp.model.UploadedPhoto
import com.kirakishou.photoexchange.mvp.model.exception.GeneralException
import com.kirakishou.photoexchange.mvp.model.exception.PhotoUploadingException
import com.kirakishou.photoexchange.mvp.model.net.response.UploadPhotoResponse
import com.kirakishou.photoexchange.mvp.model.other.ErrorCode
import com.kirakishou.photoexchange.mvp.model.other.LonLat
import io.reactivex.Observable
import io.reactivex.ObservableEmitter
import io.reactivex.Single
import timber.log.Timber
import java.io.File

class UploadPhotosUseCase(
    private val database: MyDatabase,
    private val takenPhotosRepository: TakenPhotosRepository,
    private val uploadedPhotosRepository: UploadedPhotosRepository,
    private val apiClient: ApiClient,
    private val timeUtils: TimeUtils
) {
    private val TAG = "UploadPhotosUseCase"

    fun uploadPhoto(photo: TakenPhoto, userId: String, location: LonLat, emitter: ObservableEmitter<UploadedPhotosFragmentEvent.PhotoUploadEvent>) {
        try {
            if (photo.photoTempFile == null || !photo.photoTempFile!!.exists()) {
                Timber.tag(TAG).e("Photo does not exists on disk! ${photo.photoTempFile?.absoluteFile
                    ?: "(No photoTempFile)"}")

                throw PhotoUploadingException.PhotoDoesNotExistOnDisk()
            }

            val photoFile = File.createTempFile("rotated_photo", ".tmp")
            takenPhotosRepository.updatePhotoState(photo.id, PhotoState.PHOTO_UPLOADING)

            if (!BitmapUtils.rotatePhoto(photo.photoTempFile!!, photoFile)) {
                throw PhotoUploadingException.CouldNotRotatePhoto()
            }

            uploadPhoto(photoFile, location, userId, photo.isPublic, photo, emitter)
                .map { response ->
                    val errorCode = response.errorCode as ErrorCode.UploadPhotoErrors

                    when (errorCode) {
                        is ErrorCode.UploadPhotoErrors.Ok -> {
                            return@map handlePhotoUploaded(photo, location, response)
                        }

                        else -> {
                            Timber.tag(TAG).d("Could not upload photo with photoId ${photo.id}")
                            emitter.onNext(UploadedPhotosFragmentEvent.PhotoUploadEvent.OnKnownError(errorCode))
                            return@map false
                        }
                    }
                }
                .doOnEvent { _, _ -> FileUtils.deleteFile(photoFile) }
                //since we subscribe in a singleton - we don't really need to care about disposing the disposable
                .subscribe({ uploaded ->
                    if (uploaded) {
                        emitter.onNext(UploadedPhotosFragmentEvent.PhotoUploadEvent.OnUploaded(photo))
                    }

                    emitter.onComplete()
                }, { error ->
                    takenPhotosRepository.updatePhotoState(photo.id, PhotoState.FAILED_TO_UPLOAD)
                    emitter.onNext(UploadedPhotosFragmentEvent.PhotoUploadEvent.OnUnknownError(error))
                })
        } catch (error: Throwable) {
            Timber.tag(TAG).e(error)

            if (error is PhotoUploadingException) {
                when (error) {
                    is PhotoUploadingException.PhotoDoesNotExistOnDisk,
                    is PhotoUploadingException.CouldNotRotatePhoto,
                    is PhotoUploadingException.DatabaseException,
                    is PhotoUploadingException.ApiException -> {
                        val errorCode = when (error) {
                            is PhotoUploadingException.PhotoDoesNotExistOnDisk -> ErrorCode.UploadPhotoErrors.LocalNoPhotoFileOnDisk()
                            is PhotoUploadingException.CouldNotRotatePhoto -> ErrorCode.UploadPhotoErrors.LocalCouldNotRotatePhoto()
                            is PhotoUploadingException.DatabaseException -> ErrorCode.UploadPhotoErrors.LocalDatabaseError()
                            is PhotoUploadingException.ApiException -> error.remoteErrorCode
                        }

                        emitter.onNext(UploadedPhotosFragmentEvent.PhotoUploadEvent.OnKnownError(errorCode))
                    }
                }
            }

            emitter.onNext(UploadedPhotosFragmentEvent.PhotoUploadEvent.OnUnknownError(error))
        }
    }

    private fun uploadPhoto(rotatedPhotoFile: File, location: LonLat, userId: String, isPublic: Boolean, photo: TakenPhoto,
                            emitter: ObservableEmitter<UploadedPhotosFragmentEvent.PhotoUploadEvent>): Single<UploadPhotoResponse> {
        return apiClient.uploadPhoto(rotatedPhotoFile.absolutePath, location, userId, isPublic, object : PhotoUploadProgressCallback {
            override fun onProgress(progress: Int) {
                emitter.onNext(UploadedPhotosFragmentEvent.PhotoUploadEvent.OnProgress(photo, progress))
            }
        })
    }

    private fun handlePhotoUploaded(photo: TakenPhoto, location: LonLat, response: UploadPhotoResponse): Boolean {
        val photoId = response.photoId
        val photoName = response.photoName

        val dbResult = database.transactional {
            val updateResult1 = takenPhotosRepository.deletePhotoById(photo.id)
            val updateResult2 = uploadedPhotosRepository.save(photoId, photoName, location.lon, location.lat, timeUtils.getTimeFast())

            Timber.tag(TAG).d("updateResult1 = $updateResult1, updateResult2 = $updateResult2")
            return@transactional updateResult1 && updateResult2
        }

        if (!dbResult) {
            throw PhotoUploadingException.DatabaseException()
        }

        return true
    }

    interface PhotoUploadProgressCallback {
        fun onProgress(progress: Int)
    }
}