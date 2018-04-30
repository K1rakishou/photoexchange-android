package com.kirakishou.photoexchange.mvp.viewmodel

import com.kirakishou.photoexchange.helper.CameraProvider
import com.kirakishou.photoexchange.helper.concurrency.rx.scheduler.SchedulerProvider
import com.kirakishou.photoexchange.helper.database.repository.PhotosRepository
import com.kirakishou.photoexchange.helper.database.repository.SettingsRepository
import com.kirakishou.photoexchange.mvp.model.MyPhoto
import com.kirakishou.photoexchange.mvp.model.PhotoState
import com.kirakishou.photoexchange.mvp.model.other.ErrorCode
import com.kirakishou.photoexchange.mvp.view.TakePhotoActivityView
import io.reactivex.Observable
import timber.log.Timber
import java.io.File
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException

/**
 * Created by kirakishou on 11/7/2017.
 */
class TakePhotoActivityViewModel(
    private val schedulerProvider: SchedulerProvider,
    private val photosRepository: PhotosRepository,
    private val settingsRepository: SettingsRepository
) : BaseViewModel<TakePhotoActivityView>() {

    private val tag = "TakePhotoActivityViewModel"

    override fun onCleared() {
        Timber.tag(tag).d("onCleared()")

        super.onCleared()
    }

    fun takePhoto(): Observable<ErrorCode.TakePhotoErrors> {
        return Observable.fromCallable {
            var myPhoto: MyPhoto = MyPhoto.empty()
            var file: File? = null

            try {
                settingsRepository.generateUserIdIfNotExists()
                photosRepository.deleteAllWithState(PhotoState.PHOTO_TAKEN)
                photosRepository.cleanFilesDirectory()

                file = photosRepository.createFile()

                val takePhotoStatus = getView()?.takePhoto(file)
                    ?.observeOn(schedulerProvider.IO())
                    ?.timeout(3, TimeUnit.SECONDS)
                    ?.blockingGet() ?: false

                if (!takePhotoStatus) {
                    cleanUp(file, null)
                    return@fromCallable ErrorCode.TakePhotoErrors.CouldNotTakePhoto()
                }

                myPhoto = photosRepository.saveTakenPhoto(file)
                if (myPhoto.isEmpty()) {
                    cleanUp(file, myPhoto)
                    return@fromCallable ErrorCode.TakePhotoErrors.DatabaseError()
                }

                return@fromCallable ErrorCode.TakePhotoErrors.Ok(myPhoto)
            } catch (error: Exception) {
                Timber.tag(tag).e(error)

                cleanUp(file, myPhoto)
                return@fromCallable handleException(error)
            }
        }
    }

    private fun cleanUp(file: File?, photo: MyPhoto?) {
        photosRepository.deleteFileIfExists(file)
        photosRepository.deleteMyPhoto(photo)
    }

    private fun handleException(error: Exception): ErrorCode.TakePhotoErrors {
        return when (error.cause) {
            null -> ErrorCode.TakePhotoErrors.DatabaseError()

            else -> when (error.cause!!) {
                is CameraProvider.CameraIsNotAvailable -> ErrorCode.TakePhotoErrors.CameraIsNotAvailable()
                is CameraProvider.CameraIsNotStartedException -> ErrorCode.TakePhotoErrors.CameraIsNotStartedException()
                is TimeoutException -> ErrorCode.TakePhotoErrors.TimeoutException()
                else -> ErrorCode.TakePhotoErrors.UnknownError()
            }
        }
    }
}
















