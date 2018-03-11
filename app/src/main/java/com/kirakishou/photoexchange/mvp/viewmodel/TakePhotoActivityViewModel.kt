package com.kirakishou.photoexchange.mvp.viewmodel

import android.widget.Toast
import com.kirakishou.photoexchange.base.BaseViewModel
import com.kirakishou.photoexchange.helper.concurrency.coroutine.CoroutineThreadPoolProvider
import com.kirakishou.photoexchange.helper.database.entity.MyPhotoEntity
import com.kirakishou.photoexchange.helper.database.repository.MyPhotoRepository
import com.kirakishou.photoexchange.mvp.model.MyPhoto
import com.kirakishou.photoexchange.mvp.model.state.PhotoState
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
    private val myPhotoRepository: MyPhotoRepository
) : BaseViewModel<TakePhotoActivityView>(view) {

    private val tag = "[${this::class.java.simpleName}] "

    override fun init() {
        async(coroutinesPool.BG()) {
            myPhotoRepository.init()
        }
    }

    override fun tearDown() {
        clearView()
        Timber.tag(tag).d("View cleared")
    }

    fun takePhoto() {
        async(coroutinesPool.BG()) {
            var myPhoto: MyPhoto? = null

            try {
                getView()?.hideControls()
                myPhotoRepository.deleteAllWithState(PhotoState.PHOTO_TAKEN)

                myPhoto = myPhotoRepository.insert(MyPhotoEntity.create())
                if (myPhoto.isEmpty()) {
                    Timber.tag(tag).e("myPhotoRepository.insert returned empty photo")

                    myPhotoRepository.delete(myPhoto)
                    getView()?.showToast("Could not take photo (database error)", Toast.LENGTH_LONG)
                    getView()?.showControls()
                    return@async
                }

                val takePhotoStatus = getView()?.takePhoto(myPhoto.getFile())?.await() ?: false
                if (!takePhotoStatus) {
                    Timber.tag(tag).e("view.takePhoto returned false")

                    myPhotoRepository.delete(myPhoto)
                    getView()?.showToast("Could not take photo (database error)", Toast.LENGTH_LONG)
                    getView()?.showControls()
                    return@async
                }

                getView()?.onPhotoTaken(myPhoto)
            } catch (error: Throwable) {
                Timber.tag(tag).e(error)
                myPhotoRepository.delete(myPhoto)
                getView()?.showToast("Could not take photo (database error)", Toast.LENGTH_LONG)
                getView()?.showControls()
            }
        }
    }

    override fun onCleared() {
        Timber.tag(tag).d("TakePhotoActivityViewModel.onCleared()")

        super.onCleared()
    }
}
















