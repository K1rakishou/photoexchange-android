package com.kirakishou.photoexchange.mvp.viewmodel

import com.kirakishou.photoexchange.base.BaseViewModel
import com.kirakishou.photoexchange.helper.concurrency.coroutine.CoroutineThreadPoolProvider
import com.kirakishou.photoexchange.helper.database.repository.PhotosRepository
import com.kirakishou.photoexchange.mvp.view.AllPhotosActivityView
import timber.log.Timber

/**
 * Created by kirakishou on 3/11/2018.
 */
class AllPhotosActivityViewModel(
    view: AllPhotosActivityView,
    val photosRepository: PhotosRepository,
    val coroutinesPool: CoroutineThreadPoolProvider
): BaseViewModel<AllPhotosActivityView>(view) {

    private val tag = "[${this::class.java.simpleName}] "

    override fun attach() {
    }

    override fun detach() {
        clearView()
        Timber.tag(tag).d("View cleared")
    }

    override fun onCleared() {
        Timber.tag(tag).d("onCleared()")

        super.onCleared()
    }
}