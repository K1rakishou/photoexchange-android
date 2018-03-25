package com.kirakishou.photoexchange.di.module

import com.kirakishou.photoexchange.di.scope.PerActivity
import com.kirakishou.photoexchange.helper.CameraProvider
import dagger.Module
import dagger.Provides
import org.mockito.Mockito

/**
 * Created by kirakishou on 3/8/2018.
 */

@Module
class MockCameraProviderModule {

    @PerActivity
    @Provides
    fun provideCameraProvider(): CameraProvider {
        return Mockito.mock(CameraProvider::class.java)
    }
}