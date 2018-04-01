package com.kirakishou.photoexchange

import com.kirakishou.photoexchange.di.component.ApplicationComponent
import com.kirakishou.photoexchange.di.component.DaggerMockApplicationComponent
import com.kirakishou.photoexchange.di.component.MockApplicationComponent
import com.kirakishou.photoexchange.di.module.ApplicationModule
import com.kirakishou.photoexchange.di.module.InMemoryDatabaseModule
import timber.log.Timber

/**
 * Created by kirakishou on 3/8/2018.
 */
class MockApplication : PhotoExchangeApplication() {

    override lateinit var applicationComponent: ApplicationComponent

    override fun onCreate() {
        Timber.plant(Timber.DebugTree())

        applicationComponent = initializeApplicationComponent()
        applicationComponent.inject(this)
    }

    override fun init() {

    }

    override fun initializeApplicationComponent(): MockApplicationComponent {
//        return DaggerMockApplicationComponent.builder()
//            .applicationModule(ApplicationModule(this))
//            .inMemoryDatabaseModule(InMemoryDatabaseModule())
//            .build()
    }
}