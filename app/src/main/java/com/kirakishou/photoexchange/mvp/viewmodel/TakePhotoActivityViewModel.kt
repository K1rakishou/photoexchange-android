package com.kirakishou.photoexchange.mvp.viewmodel

import android.widget.Toast
import com.kirakishou.photoexchange.base.BaseViewModel
import com.kirakishou.photoexchange.helper.concurrency.coroutine.CoroutineThreadPoolProvider
import com.kirakishou.photoexchange.helper.database.repository.PhotosRepository
import com.kirakishou.photoexchange.helper.database.repository.SettingsRepository
import com.kirakishou.photoexchange.mvp.model.MyPhoto
import com.kirakishou.photoexchange.mvp.view.TakePhotoActivityView
import kotlinx.coroutines.experimental.async
import kotlinx.coroutines.experimental.rx2.await
import timber.log.Timber

/**
 * Created by kirakishou on 11/7/2017.
 */
class TakePhotoActivityViewModel(
    view: TakePhotoActivityView,
    private val coroutinesPool: CoroutineThreadPoolProvider,
    private val photosRepository: PhotosRepository,
    private val settingsRepository: SettingsRepository
) : BaseViewModel<TakePhotoActivityView>(view) {

    private val tag = "[${this::class.java.simpleName}] "

    override fun attach() {
        async(coroutinesPool.BG()) {
            photosRepository.init()
        }
    }

    override fun detach() {
        clearView()
        Timber.tag(tag).d("View cleared")
    }

    fun takePhoto() {
        async(coroutinesPool.BG()) {
            var myPhoto: MyPhoto = MyPhoto.empty()

            try {
                view?.hideControls()
                settingsRepository.generateUserIdIfNotExists()

                val file = photosRepository.createFile()
                val takePhotoStatus = view?.takePhoto(file)?.await() ?: false
                if (!takePhotoStatus) {
                    return@async
                }

                myPhoto = photosRepository.saveTakenPhoto(file)
                if (myPhoto.isEmpty()) {
                    photosRepository.deleteMyPhoto(myPhoto)
                    view?.showToast("Could not take photo (database error)", Toast.LENGTH_LONG)
                    view?.showControls()
                    return@async
                }

                view?.onPhotoTaken(myPhoto)
            } catch (error: Throwable) {
                Timber.tag(tag).e(error)
                photosRepository.deleteMyPhoto(myPhoto)
                view?.showToast("Could not take photo (database error)", Toast.LENGTH_LONG)
                view?.showControls()
            }
        }
    }

    override fun onCleared() {
        Timber.tag(tag).d("onCleared()")

        super.onCleared()
    }
}
















