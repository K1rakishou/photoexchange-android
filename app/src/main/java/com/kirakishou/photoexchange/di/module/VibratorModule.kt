package com.kirakishou.photoexchange.di.module

import android.content.Context
import com.kirakishou.photoexchange.di.scope.PerActivity
import com.kirakishou.photoexchange.helper.Vibrator
import dagger.Module
import dagger.Provides
import javax.inject.Named

@Module
class VibratorModule {

  @PerActivity
  @Provides
  fun provideVibrator(@Named("app_context") context: Context): Vibrator {
    return Vibrator(context)
  }
}