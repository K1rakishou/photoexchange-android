package com.kirakishou.photoexchange.di.module

import android.content.Context
import com.bumptech.glide.Glide
import com.bumptech.glide.GlideBuilder
import com.bumptech.glide.RequestManager
import com.kirakishou.photoexchange.helper.ImageLoader
import dagger.Module
import dagger.Provides
import javax.inject.Singleton

/**
 * Created by kirakishou on 12/20/2017.
 */

@Module
class ImageLoaderModule {

    @Singleton
    @Provides
    fun provideImageLoader(context: Context): ImageLoader {
        return ImageLoader(context)
    }
}