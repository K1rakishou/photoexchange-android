package com.kirakishou.photoexchange.di.module

import android.content.Context
import com.kirakishou.photoexchange.service.UploadPhotoServicePresenter
import dagger.Module
import dagger.Provides
import javax.inject.Singleton

/**
 * Created by kirakishou on 3/17/2018.
 */

@Module
class UploadPhotoServiceModule(
    val context: Context
) {

    @Singleton
    @Provides
    fun provideContext(): Context {
        return context
    }
}