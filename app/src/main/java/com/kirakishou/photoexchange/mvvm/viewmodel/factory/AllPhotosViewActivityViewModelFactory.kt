package com.kirakishou.photoexchange.mvvm.viewmodel.factory

import android.arch.lifecycle.ViewModel
import android.arch.lifecycle.ViewModelProvider
import com.kirakishou.photoexchange.helper.database.repository.TakenPhotosRepository
import com.kirakishou.photoexchange.helper.rx.scheduler.SchedulerProvider
import com.kirakishou.photoexchange.mvvm.viewmodel.AllPhotosViewActivityViewModel
import javax.inject.Inject

/**
 * Created by kirakishou on 11/7/2017.
 */
class AllPhotosViewActivityViewModelFactory
@Inject constructor(
        val takenPhotosRepository: TakenPhotosRepository,
        val schedulers: SchedulerProvider
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        return AllPhotosViewActivityViewModel(takenPhotosRepository, schedulers) as T
    }
}