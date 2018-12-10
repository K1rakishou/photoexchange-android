package com.kirakishou.photoexchange.di.module

import android.app.Application
import android.content.Context
import dagger.Module
import dagger.Provides
import javax.inject.Named
import javax.inject.Singleton

/**
 * Created by kirakishou on 3/3/2018.
 */
@Module
open class ApplicationModule(
  private val application: Application
) {

  @Singleton
  @Provides
  open fun provideApplication(): Application {
    return application
  }

  @Singleton
  @Provides
  @Named("app_context")
  open fun provideContext(): Context {
    return application.applicationContext
  }
}