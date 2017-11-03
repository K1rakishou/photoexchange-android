package com.kirakishou.photoexchange.mvvm.viewmodel.factory

import android.arch.lifecycle.ViewModel
import android.arch.lifecycle.ViewModelProvider
import com.kirakishou.photoexchange.helper.api.ApiClient
import com.kirakishou.photoexchange.helper.rx.scheduler.SchedulerProvider
import com.kirakishou.photoexchange.mvvm.viewmodel.MainActivityViewModel
import javax.inject.Inject

/**
 * Created by kirakishou on 11/3/2017.
 */
class MainActivityViewModelFactory
@Inject constructor(val apiClient: ApiClient,
                    val mSchedulers: SchedulerProvider): ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return MainActivityViewModel(apiClient, mSchedulers) as T
    }
}