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
import com.kirakishou.photoexchange.mvp.model.UploadedPhoto
import com.kirakishou.photoexchange.mvp.model.net.response.UploadPhotoResponse
import com.kirakishou.photoexchange.mvp.model.other.ErrorCode
import com.kirakishou.photoexchange.mvp.model.other.LonLat
import com.kirakishou.photoexchange.service.UploadPhotoServiceCallbacks
import com.kirakishou.photoexchange.service.UploadPhotoServicePresenter
import io.reactivex.Observable
import io.reactivex.Single
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

    fun uploadPhoto(photo: TakenPhoto, userId: String, location: LonLat, callbacks: WeakReference<UploadPhotoServiceCallbacks>?): Observable<UploadedPhoto> {
        return Observable.just(Unit)
            .concatMap {
                if (photo.photoTempFile == null || !photo.photoTempFile!!.exists()) {
                    Timber.tag(TAG).e("Photo does not exists on disk! ${photo.photoTempFile?.absoluteFile
                        ?: "(No photoTempFile)"}")
                    throw UploadPhotoServicePresenter.PhotoUploadingException.PhotoDoesNotExistOnDisk(photo)
                }

                return@concatMap Observable.fromCallable { File.createTempFile("rotated_photo", ".tmp") }
                    .concatMap { photoFile ->
                        val rotatedPhotoObservable = Observable.fromCallable {
                            takenPhotosRepository.updatePhotoState(photo.id, PhotoState.PHOTO_UPLOADING)

                            if (!BitmapUtils.rotatePhoto(photo.photoTempFile!!, photoFile)) {
                                throw UploadPhotoServicePresenter.PhotoUploadingException.CouldNotRotatePhoto(photo)
                            }

                            return@fromCallable photoFile
                        }

                        return@concatMap rotatedPhotoObservable.concatMap { rotatedPhoto ->
                            uploadPhoto(rotatedPhoto, location, userId, photo.isPublic, callbacks, photo)
                                .map { response ->
                                    val errorCode = response.errorCode as ErrorCode.UploadPhotoErrors

                                    return@map when (errorCode) {
                                        is ErrorCode.UploadPhotoErrors.Ok -> {
                                            handlePhotoUploaded(photo, location, response)
                                        }

                                        else -> {
                                            Timber.tag(TAG).d("Could not upload photo with photoId ${photo.id}")
                                            throw UploadPhotoServicePresenter.PhotoUploadingException.RemoteServerException(errorCode, photo)
                                        }
                                    }
                                }
                                .toObservable()
                        }
                        .doOnEach { FileUtils.deleteFile(photoFile) }
                    }
            }
    }

    private fun uploadPhoto(rotatedPhotoFile: File, location: LonLat, userId: String, isPublic: Boolean, callbacks: WeakReference<UploadPhotoServiceCallbacks>?, photo: TakenPhoto): Single<UploadPhotoResponse> {
        return apiClient.uploadPhoto(rotatedPhotoFile.absolutePath, location, userId, isPublic, object : PhotoUploadProgressCallback {
            override fun onProgress(progress: Int) {
                callbacks?.get()?.onUploadingEvent(UploadedPhotosFragmentEvent.PhotoUploadEvent.OnProgress(photo, progress))
            }
        })
    }

    private fun handlePhotoUploaded(photo: TakenPhoto, location: LonLat, response: UploadPhotoResponse): UploadedPhoto {
        photo.photoName = response.photoName

        val dbResult = database.transactional {
            val updateResult1 = takenPhotosRepository.deletePhotoById(photo.id)
            val updateResult2 = uploadedPhotosRepository.save(photo, location.lon, location.lat, TimeUtils.getTimeFast())

            Timber.tag(TAG).d("updateResult1 = $updateResult1, updateResult2 = $updateResult2")
            return@transactional updateResult1 && updateResult2
        }

        if (!dbResult) {
            throw UploadPhotoServicePresenter.PhotoUploadingException.DatabaseException(photo)
        }

        return UploadedPhoto(photo.id, photo.photoName!!, location.lon, location.lat, false, TimeUtils.getTimeFast())
    }

    interface PhotoUploadProgressCallback {
        fun onProgress(progress: Int)
    }
}