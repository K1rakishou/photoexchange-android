package com.kirakishou.photoexchange.di.module

import dagger.Module
import dagger.Provides
import org.greenrobot.eventbus.EventBus
import javax.inject.Singleton

/**
 * Created by kirakishou on 11/8/2017.
 */

@Module
class EventBusModule {

    @Singleton
    @Provides
    fun provideEventBus(): EventBus {
        return EventBus.getDefault()
    }
}