package com.kirakishou.photoexchange.di.component

import com.kirakishou.photoexchange.di.module.SettingsActivityModule
import com.kirakishou.photoexchange.di.scope.PerActivity
import com.kirakishou.photoexchange.ui.activity.SettingsActivity
import dagger.Subcomponent

@PerActivity
@Subcomponent(modules = [
  SettingsActivityModule::class
])
interface SettingsActivityComponent {
  fun inject(activity: SettingsActivity)
}