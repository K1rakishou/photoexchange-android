package com.kirakishou.photoexchange.mwvm.viewmodel.factory

import android.arch.lifecycle.ViewModel
import android.arch.lifecycle.ViewModelProvider
import com.kirakishou.photoexchange.helper.api.ApiClient
import com.kirakishou.photoexchange.helper.database.repository.PhotoAnswerRepository
import com.kirakishou.photoexchange.helper.database.repository.RecipientLocationRepository
import com.kirakishou.photoexchange.helper.database.repository.TakenPhotosRepository
import com.kirakishou.photoexchange.helper.rx.scheduler.SchedulerProvider
import com.kirakishou.photoexchange.mwvm.viewmodel.AllPhotosViewActivityViewModel
import javax.inject.Inject

/**
 * Created by kirakishou on 11/7/2017.
 */
class AllPhotosViewActivityViewModelFactory
@Inject constructor(
        val apiClient: ApiClient,
        val photoAnswerRepository: PhotoAnswerRepository,
        val takenPhotosRepository: TakenPhotosRepository,
        val recipientLocationRepository: RecipientLocationRepository,
        val schedulers: SchedulerProvider
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        return AllPhotosViewActivityViewModel(apiClient, photoAnswerRepository, takenPhotosRepository, recipientLocationRepository, schedulers) as T
    }
}