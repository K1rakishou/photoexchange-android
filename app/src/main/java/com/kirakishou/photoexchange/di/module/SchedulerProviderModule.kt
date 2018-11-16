package com.kirakishou.photoexchange.di.module

import com.kirakishou.photoexchange.helper.concurrency.rx.scheduler.NormalSchedulers
import com.kirakishou.photoexchange.helper.concurrency.rx.scheduler.SchedulerProvider
import dagger.Module
import dagger.Provides
import javax.inject.Singleton

/**
 * Created by kirakishou on 3/3/2018.
 */
@Module
open class SchedulerProviderModule {

  @Singleton
  @Provides
  open fun provideSchedulers(): SchedulerProvider {
    return NormalSchedulers()
  }
}