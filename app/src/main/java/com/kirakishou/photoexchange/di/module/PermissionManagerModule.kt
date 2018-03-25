package com.kirakishou.photoexchange.di.module

import com.kirakishou.photoexchange.di.scope.PerActivity
import com.kirakishou.photoexchange.helper.permission.PermissionManager
import dagger.Module
import dagger.Provides

/**
 * Created by kirakishou on 3/4/2018.
 */

@Module
class PermissionManagerModule {

    @PerActivity
    @Provides
    fun providePermissionManager(): PermissionManager {
        return PermissionManager()
    }
}