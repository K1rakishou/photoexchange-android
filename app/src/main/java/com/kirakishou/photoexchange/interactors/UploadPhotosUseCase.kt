package com.kirakishou.photoexchange.interactors

import com.kirakishou.photoexchange.helper.api.ApiClient
import com.kirakishou.photoexchange.helper.database.MyDatabase
import com.kirakishou.photoexchange.helper.database.repository.TakenPhotosRepository
import com.kirakishou.photoexchange.helper.database.repository.UploadedPhotosRepository
import com.kirakishou.photoexchange.helper.intercom.event.UploadedPhotosFragmentEvent
import com.kirakishou.photoexchange.helper.util.BitmapUtils
import com.kirakishou.photoexchange.helper.util.FileUtils
import com.kirakishou.photoexchange.helper.util.TimeUtils
import com.kirakishou.photoexchange.mvp.model.PhotoState
import com.kirakishou.photoexchange.mvp.model.TakenPhoto
import com.kirakishou.photoexchange.mvp.model.exception.PhotoUploadingException
import com.kirakishou.photoexchange.mvp.model.net.response.UploadPhotoResponse
import com.kirakishou.photoexchange.mvp.model.other.ErrorCode
import com.kirakishou.photoexchange.mvp.model.other.LonLat
import io.reactivex.Observable
import io.reactivex.ObservableEmitter
import io.reactivex.Single
import timber.log.Timber
import java.io.File

open class UploadPhotosUseCase(
    private val database: MyDatabase,
    private val takenPhotosRepository: TakenPhotosRepository,
    private val uploadedPhotosRepository: UploadedPhotosRepository,
    private val apiClient: ApiClient,
    private val timeUtils: TimeUtils,
    private val bitmapUtils: BitmapUtils,
    private val fileUtils: FileUtils
) {
    private val TAG = "UploadPhotosUseCase"

    open fun uploadPhoto(photo: TakenPhoto, userId: String, location: LonLat): Observable<UploadedPhotosFragmentEvent.PhotoUploadEvent> {
        return Observable.create { emitter -> doUpload(photo, location, userId, emitter) }
    }

    private fun doUpload(
        photo: TakenPhoto,
        location: LonLat,
        userId: String,
        emitter: ObservableEmitter<UploadedPhotosFragmentEvent.PhotoUploadEvent>
    ) {
        try {
            if (!photo.fileExists()) {
                val path = photo.photoTempFile?.absolutePath ?: "(No photoTempFile)"
                Timber.tag(TAG).e("Photo does not exists on disk! $path")

                throw PhotoUploadingException.PhotoDoesNotExistOnDisk()
            }

            val photoFile = fileUtils.createTempFile("rotated_photo", ".tmp")

            try {
                if (!takenPhotosRepository.updatePhotoState(photo.id, PhotoState.PHOTO_UPLOADING)) {
                    throw PhotoUploadingException.CouldNotUpdatePhotoState()
                }

                if (!bitmapUtils.rotatePhoto(photo.photoTempFile!!, photoFile)) {
                    throw PhotoUploadingException.CouldNotRotatePhoto()
                }

                uploadPhoto(photoFile, location, userId, photo.isPublic, photo, emitter)
                    .map { response -> handleResponse(response, photo, location, emitter) }
                    .doOnEvent { _, _ -> fileUtils.deleteFile(photoFile) }
                    //since we subscribe in a singleton - we don't really need to care about disposing the disposable
                    .subscribe(
                        { uploaded -> handleOnNext(uploaded, emitter, photo) },
                        { error -> handleOnError(photo, emitter, error) }
                    )
            } catch (error: Throwable) {
                fileUtils.deleteFile(photoFile)
                throw error
            }
        } catch (error: Throwable) {
            handleOnError(photo, emitter, error)
        }
    }

    private fun handleOnError(photo: TakenPhoto, emitter: ObservableEmitter<UploadedPhotosFragmentEvent.PhotoUploadEvent>, error: Throwable) {
        Timber.tag(TAG).e(error)
        takenPhotosRepository.updatePhotoState(photo.id, PhotoState.FAILED_TO_UPLOAD)

        val errorCode = tryToFigureOutExceptionErrorCode(error)
        if (errorCode != null) {
            emitter.onNext(UploadedPhotosFragmentEvent.PhotoUploadEvent.OnKnownError(errorCode))
        } else {
            emitter.onNext(UploadedPhotosFragmentEvent.PhotoUploadEvent.OnUnknownError(error))
        }

        emitter.onComplete()
    }

    private fun handleOnNext(uploaded: Boolean, emitter: ObservableEmitter<UploadedPhotosFragmentEvent.PhotoUploadEvent>, photo: TakenPhoto) {
        if (uploaded) {
            emitter.onNext(UploadedPhotosFragmentEvent.PhotoUploadEvent.OnUploaded(photo))
        }

        emitter.onComplete()
    }

    private fun handleResponse(
        response: UploadPhotoResponse,
        photo: TakenPhoto,
        location: LonLat,
        emitter: ObservableEmitter<UploadedPhotosFragmentEvent.PhotoUploadEvent>
    ): Boolean {
        val errorCode = response.errorCode as ErrorCode.UploadPhotoErrors
        when (errorCode) {
            is ErrorCode.UploadPhotoErrors.Ok -> {
                return handlePhotoUploaded(photo, location, response)
            }

            else -> {
                Timber.tag(TAG).d("Could not upload photo with photoId ${photo.id}")
                emitter.onNext(UploadedPhotosFragmentEvent.PhotoUploadEvent.OnKnownError(errorCode))
                return false
            }
        }
    }

    private fun tryToFigureOutExceptionErrorCode(error: Throwable): ErrorCode.UploadPhotoErrors? {
        return when (error) {
            is PhotoUploadingException.PhotoDoesNotExistOnDisk -> ErrorCode.UploadPhotoErrors.LocalNoPhotoFileOnDisk()
            is PhotoUploadingException.CouldNotRotatePhoto -> ErrorCode.UploadPhotoErrors.LocalCouldNotRotatePhoto()
            is PhotoUploadingException.DatabaseException -> ErrorCode.UploadPhotoErrors.LocalDatabaseError()
            is PhotoUploadingException.CouldNotUpdatePhotoState -> ErrorCode.UploadPhotoErrors.LocalCouldNotUpdatePhotoState()
            is PhotoUploadingException.ApiException -> error.remoteErrorCode
            else -> null
        }
    }

    private fun uploadPhoto(
        rotatedPhotoFile: File,
        location: LonLat,
        userId: String,
        isPublic: Boolean,
        photo: TakenPhoto,
        emitter: ObservableEmitter<UploadedPhotosFragmentEvent.PhotoUploadEvent>
    ): Single<UploadPhotoResponse> {
        return apiClient.uploadPhoto(rotatedPhotoFile.absolutePath, location, userId, isPublic, object : PhotoUploadProgressCallback {
            override fun onProgress(progress: Int) {
                emitter.onNext(UploadedPhotosFragmentEvent.PhotoUploadEvent.OnProgress(photo, progress))
            }
        })
    }

    private fun handlePhotoUploaded(photo: TakenPhoto, location: LonLat, response: UploadPhotoResponse): Boolean {
        val photoId = response.photoId
        val photoName = response.photoName

        if (!tryToMovePhotoToUploaded(photo, photoId, photoName, location)) {
            throw PhotoUploadingException.DatabaseException()
        }

        return true
    }

    private fun tryToMovePhotoToUploaded(photo: TakenPhoto, photoId: Long, photoName: String, location: LonLat): Boolean {
        return database.transactional {
            val updateResult1 = takenPhotosRepository.deletePhotoById(photo.id)
            val updateResult2 = uploadedPhotosRepository.save(photoId, photoName, location.lon,
                location.lat, timeUtils.getTimeFast())

            return@transactional updateResult1 && updateResult2
        }
    }

    interface PhotoUploadProgressCallback {
        fun onProgress(progress: Int)
    }
}