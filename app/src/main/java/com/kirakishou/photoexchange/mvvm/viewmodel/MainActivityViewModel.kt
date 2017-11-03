package com.kirakishou.photoexchange.mvvm.viewmodel

import com.kirakishou.photoexchange.base.BaseViewModel
import com.kirakishou.photoexchange.helper.api.ApiClient
import com.kirakishou.photoexchange.helper.rx.scheduler.SchedulerProvider
import timber.log.Timber
import javax.inject.Inject

/**
 * Created by kirakishou on 11/3/2017.
 */
class MainActivityViewModel
@Inject constructor(private val apiClient: ApiClient,
                    private val mSchedulers: SchedulerProvider) : BaseViewModel() {

    override fun onCleared() {
        Timber.e("ClientMainActivityViewModel.onCleared()")

        super.onCleared()
    }
}