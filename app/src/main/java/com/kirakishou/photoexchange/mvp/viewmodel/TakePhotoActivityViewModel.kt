package com.kirakishou.photoexchange.mvp.viewmodel

import com.kirakishou.photoexchange.base.BaseViewModel
import com.kirakishou.photoexchange.helper.concurrency.coroutine.CoroutineThreadPoolProvider
import com.kirakishou.photoexchange.helper.concurrency.scheduler.SchedulerProvider
import com.kirakishou.photoexchange.helper.database.entity.MyPhotoEntity
import com.kirakishou.photoexchange.helper.database.repository.MyPhotoRepository
import com.kirakishou.photoexchange.helper.database.repository.TempFileRepository
import com.kirakishou.photoexchange.mvp.view.TakePhotoActivityView
import io.reactivex.rxkotlin.plusAssign
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
    private val coroutinesPool: CoroutineThreadPoolProvider,
    private val schedulers: SchedulerProvider,
    private val myPhotoRepository: MyPhotoRepository,
    private val tempFileRepository: TempFileRepository
) : BaseViewModel<TakePhotoActivityView>(view) {

    fun takePhoto() {
        compositeDisposable += launch {
            getView()?.hideTakePhotoButton()

            val tempFileEntity = tempFileRepository.createTempFile().await()
                ?: return@launch

            val tempFile = File(tempFileEntity.filePath)

            try {
                val status = getView()?.takePhoto(tempFile)?.await() ?: false
                if (!status) {
                    return@launch
                }

                if (!myPhotoRepository.insert(MyPhotoEntity.create(tempFileEntity.photoOwnerId)).await()) {
                    return@launch
                }

            } finally {
                getView()?.showTakePhotoButton()
            }

        }.asCompletable(coroutinesPool.provideCommon())
            .subscribe()
    }

    override fun onCleared() {
        Timber.d("TakePhotoActivityViewModel.onCleared()")

        super.onCleared()
    }
}
















