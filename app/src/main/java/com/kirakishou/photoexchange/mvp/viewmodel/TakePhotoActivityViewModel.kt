package com.kirakishou.photoexchange.mvp.viewmodel

import com.kirakishou.photoexchange.helper.CameraProvider
import com.kirakishou.photoexchange.helper.concurrency.rx.scheduler.SchedulerProvider
import com.kirakishou.photoexchange.helper.database.repository.PhotosRepository
import com.kirakishou.photoexchange.mvp.model.TakenPhoto
import com.kirakishou.photoexchange.mvp.model.PhotoState
import com.kirakishou.photoexchange.mvp.model.other.ErrorCode
import com.kirakishou.photoexchange.mvp.view.TakePhotoActivityView
import io.reactivex.Single
import io.reactivex.subjects.PublishSubject
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
    private val photosRepository: PhotosRepository
) : BaseViewModel<TakePhotoActivityView>() {

    private val TAG = "TakePhotoActivityViewModel"

    val errorCodesSubject = PublishSubject.create<ErrorCode>().toSerialized()

    override fun onCleared() {
        Timber.tag(TAG).d("onCleared()")

        super.onCleared()
    }

    fun takePhoto(): Single<ErrorCode.TakePhotoErrors> {
        return async {
            var takenPhoto: TakenPhoto = TakenPhoto.empty()
            var file: File? = null

            try {
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

                takenPhoto = photosRepository.saveTakenPhoto(file)
                if (takenPhoto.isEmpty()) {
                    cleanUp(file, takenPhoto)
                    return@async ErrorCode.TakePhotoErrors.DatabaseError()
                }

                return@async ErrorCode.TakePhotoErrors.Ok(takenPhoto)
            } catch (error: Exception) {
                Timber.tag(TAG).e(error)

                cleanUp(file, takenPhoto)
                return@async handleException(error)
            }
        }.asSingle(CommonPool)
    }

    private fun cleanUp(file: File?, photo: TakenPhoto?) {
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
















