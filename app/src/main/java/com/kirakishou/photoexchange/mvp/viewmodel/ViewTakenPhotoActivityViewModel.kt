package com.kirakishou.photoexchange.mvp.viewmodel

import android.widget.Toast
import com.kirakishou.photoexchange.base.BaseViewModel
import com.kirakishou.photoexchange.helper.concurrency.coroutine.CoroutineThreadPoolProvider
import com.kirakishou.photoexchange.helper.database.repository.MyPhotoRepository
import com.kirakishou.photoexchange.mvp.model.MyPhoto
import com.kirakishou.photoexchange.mvp.model.state.PhotoState
import com.kirakishou.photoexchange.mvp.view.ViewTakenPhotoActivityView
import kotlinx.coroutines.experimental.async
import timber.log.Timber

/**
 * Created by kirakishou on 3/9/2018.
 */
class ViewTakenPhotoActivityViewModel(
    view: ViewTakenPhotoActivityView,
    private val coroutinePool: CoroutineThreadPoolProvider,
    private val myPhotoRepository: MyPhotoRepository
) : BaseViewModel<ViewTakenPhotoActivityView>(view) {

    private val tag = "[${this::class.java.simpleName}] "

    override fun init() {

    }

    override fun tearDown() {
        clearView()
        Timber.tag(tag).d("View cleared")
    }

    fun updatePhotoState(takenPhoto: MyPhoto) {
        async(coroutinePool.BG()) {
            getView()?.hideControls()

            try {
                val updatedPhoto = myPhotoRepository.updatePhotoState(takenPhoto, PhotoState.PHOTO_UPLOADING)
                if (updatedPhoto.isEmpty()) {
                    getView()?.showToast("Could not update photo in the database (database error)", Toast.LENGTH_LONG)
                    getView()?.showControls()
                    return@async
                }

                getView()?.onPhotoUpdated(takenPhoto)

            } catch (error: Throwable) {
                Timber.tag(tag).e(error)
                getView()?.showToast("Could not update photo in the database (database error)", Toast.LENGTH_LONG)
                getView()?.showControls()
            }
        }
    }

    override fun onCleared() {
        Timber.tag(tag).d("onCleared()")

        super.onCleared()
    }
}