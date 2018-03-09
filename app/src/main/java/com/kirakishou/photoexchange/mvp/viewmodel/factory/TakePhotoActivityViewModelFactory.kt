package com.kirakishou.photoexchange.mvp.viewmodel.factory

import android.arch.lifecycle.ViewModel
import android.arch.lifecycle.ViewModelProvider
import com.kirakishou.photoexchange.helper.concurrency.coroutine.CoroutineThreadPoolProvider
import com.kirakishou.photoexchange.helper.concurrency.rx.scheduler.SchedulerProvider
import com.kirakishou.photoexchange.helper.database.repository.MyPhotoRepository
import com.kirakishou.photoexchange.helper.database.repository.TempFileRepository
import com.kirakishou.photoexchange.mvp.view.TakePhotoActivityView
import com.kirakishou.photoexchange.mvp.viewmodel.TakePhotoActivityViewModel
import java.lang.ref.WeakReference
import javax.inject.Inject

/**
 * Created by kirakishou on 11/7/2017.
 */
class TakePhotoActivityViewModelFactory
@Inject constructor(
    val view: TakePhotoActivityView,
    val myPhotoRepository: MyPhotoRepository,
    val coroutinesPool: CoroutineThreadPoolProvider
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        return TakePhotoActivityViewModel(view, coroutinesPool, myPhotoRepository) as T
    }
}