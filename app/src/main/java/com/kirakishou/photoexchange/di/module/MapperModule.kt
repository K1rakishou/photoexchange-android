package com.kirakishou.photoexchange.di.module

import com.kirakishou.photoexchange.helper.mapper.TakenPhotoMapper
import com.kirakishou.photoexchange.helper.mapper.UploadedPhotoMapper
import dagger.Module
import dagger.Provides
import javax.inject.Singleton

/**
 * Created by kirakishou on 11/8/2017.
 */

@Module
class MapperModule {

    @Singleton
    @Provides
    fun provideUploadedPhotoMapper(): UploadedPhotoMapper {
        return UploadedPhotoMapper()
    }

    @Singleton
    @Provides
    fun provideTakenPhotoMapper(): TakenPhotoMapper {
        return TakenPhotoMapper()
    }
}