package com.kirakishou.photoexchange.di.module

import com.kirakishou.photoexchange.di.scope.PerActivity
import com.kirakishou.photoexchange.helper.Vibrator
import dagger.Module
import dagger.Provides

@Module
class VibratorModule {

    @PerActivity
    @Provides
    fun provideVibrator(): Vibrator {
        return Vibrator()
    }
}