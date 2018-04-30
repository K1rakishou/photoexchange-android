package com.kirakishou.photoexchange.mvp.viewmodel.factory

import android.arch.lifecycle.ViewModel
import android.arch.lifecycle.ViewModelProvider
import com.kirakishou.photoexchange.helper.concurrency.rx.scheduler.SchedulerProvider
import com.kirakishou.photoexchange.helper.database.repository.SettingsRepository
import com.kirakishou.photoexchange.mvp.viewmodel.SettingsActivityViewModel
import javax.inject.Inject

class SettingsActivityViewModelFactory
@Inject constructor(
    val settingsRepository: SettingsRepository,
    val schedulerProvider: SchedulerProvider
) : ViewModelProvider.Factory {

    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        return SettingsActivityViewModel(settingsRepository, schedulerProvider) as T
    }

}