package com.kirakishou.photoexchange.mvp.viewmodel

import com.kirakishou.photoexchange.base.BaseViewModel
import com.kirakishou.photoexchange.helper.concurrency.coroutine.CoroutineThreadPoolProvider
import com.kirakishou.photoexchange.helper.database.entity.MyPhotoEntity
import com.kirakishou.photoexchange.helper.database.repository.MyPhotoRepository
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
    private val myPhotoRepository: MyPhotoRepository
) : BaseViewModel<TakePhotoActivityView>(view) {

    private val tag = "[${this::class.java.simpleName}] "

    override fun init() {
        async(coroutinesPool.provideCommon()) {
            myPhotoRepository.init()
        }
    }

    suspend fun takePhoto() {
        async(coroutinesPool.provideCommon()) {
            var myPhoto: MyPhoto? = null

            try {
                getView()?.hideTakePhotoButton()

                myPhoto = myPhotoRepository.insert(MyPhotoEntity.create()).await()
                if (myPhoto == null) {
                    myPhotoRepository.delete(myPhoto)
                    getView()?.showToast("Could not take photo (database error)")
                    return@async
                }

                val takePhotoStatus = getView()?.takePhoto(myPhoto.getFile())?.await() ?: false
                if (!takePhotoStatus) {
                    myPhotoRepository.delete(myPhoto)
                    getView()?.showToast("Could not take photo (database error)")
                    return@async
                }

                getView()?.onPhotoTaken(myPhoto)
            } catch (error: Throwable) {
                Timber.e(error)
                myPhotoRepository.delete(myPhoto)
                getView()?.showToast("Could not take photo (database error)")
            } finally {
                getView()?.showTakePhotoButton()
            }
        }
    }

    override fun onCleared() {
        Timber.d("TakePhotoActivityViewModel.onCleared()")

        super.onCleared()
    }
}
















