package com.kirakishou.photoexchange.di.module

import com.kirakishou.photoexchange.helper.EventAccumulator
import dagger.Module
import dagger.Provides
import javax.inject.Singleton

/**
 * Created by kirakishou on 12/20/2017.
 */

@Module
class EventAccumulatorModule {

    @Singleton
    @Provides
    fun provideEventAccumulator(): EventAccumulator {
        return EventAccumulator()
    }
}