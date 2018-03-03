package com.kirakishou.photoexchange.mvp.viewmodel

import com.kirakishou.photoexchange.base.BaseViewModel
import com.kirakishou.photoexchange.helper.rx.scheduler.SchedulerProvider
import com.kirakishou.photoexchange.mvp.view.TakePhotoActivityView
import io.reactivex.rxkotlin.plusAssign
import kotlinx.coroutines.experimental.CommonPool
import kotlinx.coroutines.experimental.launch
import kotlinx.coroutines.experimental.rx2.asCompletable
import kotlinx.coroutines.experimental.rx2.await
import timber.log.Timber
import java.io.File
import java.lang.ref.WeakReference

/**
 * Created by kirakishou on 11/7/2017.
 */
class TakePhotoActivityViewModel(
    view: WeakReference<TakePhotoActivityView>,
    private val schedulers: SchedulerProvider
) : BaseViewModel<TakePhotoActivityView>(view) {

    fun takePhoto() {
        compositeDisposable += launch {
            getView()?.hideTakePhotoButton()

            val file = File.createTempFile("file", "tmp")
            try {
                val status = getView()?.takePhoto(file)?.await() ?: false
                if (!status) {
                    return@launch
                }
            } finally {
                file.delete()
            }

            getView()?.showTakePhotoButton()
        }.asCompletable(CommonPool)
            .subscribe()
    }

    override fun onCleared() {
        Timber.d("TakePhotoActivityViewModel.onCleared()")

        super.onCleared()
    }
}
















