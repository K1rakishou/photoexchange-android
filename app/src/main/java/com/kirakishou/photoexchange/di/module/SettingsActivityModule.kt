package com.kirakishou.photoexchange.di.module

import android.arch.lifecycle.ViewModelProviders
import com.kirakishou.photoexchange.di.scope.PerActivity
import com.kirakishou.photoexchange.helper.concurrency.rx.scheduler.SchedulerProvider
import com.kirakishou.photoexchange.helper.database.repository.SettingsRepository
import com.kirakishou.photoexchange.mvp.viewmodel.SettingsActivityViewModel
import com.kirakishou.photoexchange.mvp.viewmodel.factory.SettingsActivityViewModelFactory
import com.kirakishou.photoexchange.ui.activity.SettingsActivity
import dagger.Module
import dagger.Provides

@Module
open class SettingsActivityModule(
    val activity: SettingsActivity
) {

    @PerActivity
    @Provides
    fun provideViewModelFactory(schedulerProvider: SchedulerProvider,
                                settingsRepository: SettingsRepository): SettingsActivityViewModelFactory {
        return SettingsActivityViewModelFactory(settingsRepository, schedulerProvider)
    }

    @PerActivity
    @Provides
    fun provideViewModel(viewModelFactory: SettingsActivityViewModelFactory): SettingsActivityViewModel {
        return ViewModelProviders.of(activity, viewModelFactory).get(SettingsActivityViewModel::class.java)
    }
}