package com.kirakishou.photoexchange.di.module

import android.content.Context
import com.kirakishou.photoexchange.di.scope.PerActivity
import com.kirakishou.photoexchange.helper.CameraProvider
import dagger.Module
import dagger.Provides

/**
 * Created by kirakishou on 3/8/2018.
 */

@Module
class CameraProviderModule {

    @PerActivity
    @Provides
    fun provideCameraProvider(context: Context): CameraProvider {
        return CameraProvider(context)
    }
}