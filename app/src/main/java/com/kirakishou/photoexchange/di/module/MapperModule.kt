package com.kirakishou.photoexchange.di.module

import com.kirakishou.photoexchange.helper.mapper.PhotoAnswerMapper
import com.kirakishou.photoexchange.helper.mapper.RecipientLocationMapper
import com.kirakishou.photoexchange.helper.mapper.TakenPhotoMapper
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
    fun provideTakenPhotoMapper(): TakenPhotoMapper {
        return TakenPhotoMapper()
    }

    @Singleton
    @Provides
    fun providePhotoAnswerMapper(): PhotoAnswerMapper {
        return PhotoAnswerMapper()
    }

    @Singleton
    @Provides
    fun provideRecipientLocationMapper(): RecipientLocationMapper {
        return RecipientLocationMapper()
    }
}