package com.kirakishou.photoexchange.mvp.viewmodel

import com.kirakishou.photoexchange.helper.concurrency.rx.scheduler.SchedulerProvider
import com.kirakishou.photoexchange.helper.database.repository.SettingsRepository
import com.kirakishou.photoexchange.mvp.view.SettingsActivityView
import io.reactivex.Completable

class SettingsActivityViewModel(
    private val settingsRepository: SettingsRepository,
    private val schedulerProvider: SchedulerProvider
) : BaseViewModel<SettingsActivityView>() {

    fun resetMakePublicPhotoOption(): Completable {
        return Completable
            .fromAction { settingsRepository.saveMakePublicFlag(null) }
            .subscribeOn(schedulerProvider.IO())
    }

}