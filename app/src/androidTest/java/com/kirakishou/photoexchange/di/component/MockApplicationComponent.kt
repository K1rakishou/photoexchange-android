package com.kirakishou.photoexchange.di.component

import android.app.Application
import com.kirakishou.photoexchange.di.module.*
import com.kirakishou.photoexchange.helper.concurrency.rx.scheduler.SchedulerProvider
import com.kirakishou.photoexchange.helper.database.repository.PhotosRepository
import com.kirakishou.photoexchange.tests.viewmodel.TakePhotoActivityViewModelTests
import dagger.Component
import javax.inject.Singleton

/**
 * Created by kirakishou on 3/8/2018.
 */
@Singleton
@Component(modules = [
    ApplicationModule::class,
    MockSchedulerProviderModule::class,
    InMemoryDatabaseModule::class])
interface MockApplicationComponent : ApplicationComponent {
    fun inject(test: TakePhotoActivityViewModelTests)

    fun exposeApplication(): Application
    fun exposeSchedulerProvider(): SchedulerProvider
    fun exposeMyPhotoRepository(): PhotosRepository

    fun testPlus(module: MockTakePhotoActivityModule): MockTakePhotoActivityComponent
}