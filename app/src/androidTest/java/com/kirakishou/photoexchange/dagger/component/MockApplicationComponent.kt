package com.kirakishou.photoexchange.dagger.component

import com.kirakishou.photoexchange.dagger.component.activity.MockPhotosActivityComponent
import com.kirakishou.photoexchange.dagger.module.*
import com.kirakishou.photoexchange.mock.MockApplication
import com.kirakishou.photoexchange.dagger.module.activity.MockPhotosActivityModule
import com.kirakishou.photoexchange.di.module.ApiClientModule
import com.kirakishou.photoexchange.di.module.GsonModule
import com.kirakishou.photoexchange.di.module.ImageLoaderModule
import com.kirakishou.photoexchange.di.module.UseCaseProviderModule
import dagger.Component
import javax.inject.Singleton

@Singleton
@Component(modules = [
  MockApplicationModule::class,
  MockNetworkModule::class,
  MockDatabaseModule::class,
  MockDispatchersProviderModule::class,
  MockUtilsModule::class,
  MockApiClientModule::class,
  ImageLoaderModule::class,
  UseCaseProviderModule::class,
  GsonModule::class
])
interface MockApplicationComponent {
  fun inject(application: MockApplication)

  fun plus(photosActivityModule: MockPhotosActivityModule): MockPhotosActivityComponent
}