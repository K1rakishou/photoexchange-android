package com.kirakishou.photoexchange.dagger.module

import android.content.Context
import com.kirakishou.photoexchange.mock.MockApplication
import dagger.Module
import dagger.Provides
import javax.inject.Named
import javax.inject.Singleton

/**
 * Created by kirakishou on 3/3/2018.
 */
@Module
open class MockApplicationModule(
  private val application: MockApplication
) {

  @Singleton
  @Provides
  open fun provideApplication(): MockApplication {
    return application
  }

  @Singleton
  @Provides
  @Named("app_context")
  open fun provideContext(): Context {
    return application.applicationContext
  }
}