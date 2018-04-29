package com.kirakishou.photoexchange.mvp.viewmodel

import android.widget.Toast
import com.kirakishou.photoexchange.helper.CameraProvider
import com.kirakishou.photoexchange.helper.concurrency.rx.scheduler.SchedulerProvider
import com.kirakishou.photoexchange.helper.database.repository.PhotosRepository
import com.kirakishou.photoexchange.helper.database.repository.SettingsRepository
import com.kirakishou.photoexchange.mvp.model.MyPhoto
import com.kirakishou.photoexchange.mvp.model.PhotoState
import com.kirakishou.photoexchange.mvp.view.TakePhotoActivityView
import io.reactivex.Completable
import io.reactivex.rxkotlin.plusAssign
import timber.log.Timber
import java.lang.ref.WeakReference
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

    private val tag = "[${this::class.java.simpleName}] "

    override fun onCleared() {
        Timber.tag(tag).d("onCleared()")

        super.onCleared()
    }

    fun takePhoto() {
        compositeDisposable += Completable.fromAction {
            var myPhoto: MyPhoto = MyPhoto.empty()

            try {
                getView()?.hideControls()

                settingsRepository.generateUserIdIfNotExists()
                photosRepository.deleteAllWithState(PhotoState.PHOTO_TAKEN)
                photosRepository.cleanFilesDirectory()

                val file = photosRepository.createFile()
                val takePhotoStatus = getView()?.takePhoto(file)
                    ?.observeOn(schedulerProvider.IO())
                    ?.timeout(3, TimeUnit.SECONDS)
                    ?.blockingGet() ?: false
                if (!takePhotoStatus) {
                    return@fromAction
                }

                myPhoto = photosRepository.saveTakenPhoto(file)

                if (myPhoto.isEmpty()) {
                    photosRepository.deleteMyPhoto(myPhoto)
                    getView()?.showToast("Could not take photo (database error)", Toast.LENGTH_LONG)
                    getView()?.showControls()
                    return@fromAction
                }

                getView()?.onPhotoTaken(myPhoto)
            } catch (error: Exception) {
                Timber.tag(tag).e(error)

                photosRepository.deleteMyPhoto(myPhoto)
                handleException(error)
                getView()?.showControls()
            }
        }.subscribeOn(schedulerProvider.IO())
            .observeOn(schedulerProvider.IO())
            .subscribe()
    }

    private fun handleException(error: Exception) {
        when (error.cause) {
            null -> getView()?.showToast("Could not take photo (database error)", Toast.LENGTH_LONG)

            else -> when (error.cause!!) {
                is CameraProvider.CameraIsNotAvailable -> {
                    getView()?.showToast("Could not take photo (camera is not available)", Toast.LENGTH_LONG)
                }
                is CameraProvider.CameraIsNotStartedException -> {
                    getView()?.showToast("Could not take photo (camera is not started)", Toast.LENGTH_LONG)
                }
                is TimeoutException -> {
                    getView()?.showToast("Could not take photo (exceeded maximum camera wait time)", Toast.LENGTH_LONG)
                }
                else -> getView()?.showToast("Could not take photo (unknown error)", Toast.LENGTH_LONG)
            }
        }
    }
}
















