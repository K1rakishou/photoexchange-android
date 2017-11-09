package com.kirakishou.photoexchange.mvvm.viewmodel

import com.kirakishou.photoexchange.base.BaseViewModel
import com.kirakishou.photoexchange.helper.database.repository.TakenPhotosRepository
import com.kirakishou.photoexchange.helper.rx.scheduler.SchedulerProvider
import timber.log.Timber
import javax.inject.Inject

/**
 * Created by kirakishou on 11/9/2017.
 */
class ViewTakenPhotoActivityViewModel
@Inject constructor(
        val takenPhotosRepo: TakenPhotosRepository,
        val schedulers: SchedulerProvider
) : BaseViewModel() {

    override fun onCleared() {
        Timber.e("ViewTakenPhotoActivityViewModel.onCleared()")

        super.onCleared()
    }
}