package com.kirakishou.photoexchange.mvp.viewmodel

import android.widget.Toast
import com.kirakishou.photoexchange.base.BaseViewModel
import com.kirakishou.photoexchange.helper.concurrency.coroutine.CoroutineThreadPoolProvider
import com.kirakishou.photoexchange.helper.database.repository.PhotosRepository
import com.kirakishou.photoexchange.mvp.model.PhotoState
import com.kirakishou.photoexchange.mvp.view.ViewTakenPhotoActivityView
import kotlinx.coroutines.experimental.async
import timber.log.Timber
import java.lang.ref.WeakReference

/**
 * Created by kirakishou on 3/9/2018.
 */
class ViewTakenPhotoActivityViewModel(
    view: WeakReference<ViewTakenPhotoActivityView>,
    private val coroutinePool: CoroutineThreadPoolProvider,
    private val photosRepository: PhotosRepository
) : BaseViewModel<ViewTakenPhotoActivityView>(view) {

    private val tag = "[${this::class.java.simpleName}] "

    init {
        Timber.tag(tag).e("$tag init")
    }

    override fun onAttached() {
        Timber.tag(tag).d("onAttached()")
    }

    override fun onCleared() {
        Timber.tag(tag).d("onCleared()")

        super.onCleared()
    }

    fun updatePhotoState(takenPhotoId: Long) {
        async(coroutinePool.BG()) {
            try {
                getView()?.hideControls()

                val updateResult = photosRepository.updatePhotoState(takenPhotoId, PhotoState.PHOTO_TO_BE_UPLOADED)
                if (!updateResult) {
                    getView()?.showToast("Could not update photo in the database (database error)", Toast.LENGTH_LONG)
                    getView()?.showControls()
                    return@async
                }

                getView()?.onPhotoUpdated()

            } catch (error: Throwable) {
                Timber.tag(tag).e(error)
                getView()?.showToast("Could not update photo in the database (database error)", Toast.LENGTH_LONG)
                getView()?.showControls()
            }
        }
    }
}