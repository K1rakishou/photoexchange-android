package com.kirakishou.photoexchange.mvp.viewmodel

import com.kirakishou.photoexchange.helper.CameraProvider
import com.kirakishou.photoexchange.helper.concurrency.rx.scheduler.SchedulerProvider
import com.kirakishou.photoexchange.helper.database.repository.PhotosRepository
import com.kirakishou.photoexchange.helper.database.repository.SettingsRepository
import com.kirakishou.photoexchange.mvp.model.MyPhoto
import com.kirakishou.photoexchange.mvp.model.PhotoState
import com.kirakishou.photoexchange.mvp.model.other.ErrorCode
import com.kirakishou.photoexchange.mvp.view.TakePhotoActivityView
import io.reactivex.Single
import kotlinx.coroutines.experimental.CommonPool
import kotlinx.coroutines.experimental.async
import kotlinx.coroutines.experimental.rx2.asSingle
import kotlinx.coroutines.experimental.rx2.await
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

    private val TAG = "TakePhotoActivityViewModel"

    override fun onCleared() {
        Timber.tag(TAG).d("onCleared()")

        super.onCleared()
    }

    fun takePhoto(): Single<ErrorCode.TakePhotoErrors> {
        return async {
            var myPhoto: MyPhoto = MyPhoto.empty()
            var file: File? = null

            try {
                //TODO: move userId generation in other place where it can be called even if user not taking any photos
                settingsRepository.generateUserIdIfNotExists()
                photosRepository.deleteAllWithState(PhotoState.PHOTO_TAKEN)
                photosRepository.cleanFilesDirectory()

                file = photosRepository.createFile()

                val takePhotoStatus = getView()?.takePhoto(file)
                    ?.observeOn(schedulerProvider.IO())
                    ?.timeout(3, TimeUnit.SECONDS)
                    ?.await() ?: false

                if (!takePhotoStatus) {
                    cleanUp(file, null)
                    return@async ErrorCode.TakePhotoErrors.CouldNotTakePhoto()
                }

                myPhoto = photosRepository.saveTakenPhoto(file)
                if (myPhoto.isEmpty()) {
                    cleanUp(file, myPhoto)
                    return@async ErrorCode.TakePhotoErrors.DatabaseError()
                }

                return@async ErrorCode.TakePhotoErrors.Ok(myPhoto)
            } catch (error: Exception) {
                Timber.tag(TAG).e(error)

                cleanUp(file, myPhoto)
                return@async handleException(error)
            }
        }.asSingle(CommonPool)
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
















