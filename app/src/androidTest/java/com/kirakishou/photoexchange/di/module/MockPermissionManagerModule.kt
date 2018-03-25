package com.kirakishou.photoexchange.di.module

import com.kirakishou.photoexchange.di.scope.PerActivity
import com.kirakishou.photoexchange.helper.permission.PermissionManager
import dagger.Module
import dagger.Provides
import org.mockito.Mockito

/**
 * Created by kirakishou on 3/8/2018.
 */

@Module
class MockPermissionManagerModule {

    @PerActivity
    @Provides
    fun providePermissionManager(): PermissionManager {
        return Mockito.mock(PermissionManager::class.java)
    }
}