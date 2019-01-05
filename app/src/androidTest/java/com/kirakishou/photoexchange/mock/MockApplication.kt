package com.kirakishou.photoexchange.mock

import android.app.Application
import com.kirakishou.photoexchange.dagger.component.DaggerMockApplicationComponent
import com.kirakishou.photoexchange.dagger.component.MockApplicationComponent
import com.kirakishou.photoexchange.dagger.module.MockApplicationModule
import com.kirakishou.photoexchange.dagger.module.MockDatabaseModule
import com.kirakishou.photoexchange.dagger.module.MockNetworkModule

class MockApplication : Application() {
  lateinit var applicationComponent: MockApplicationComponent

  override fun onCreate() {
    super.onCreate()

    applicationComponent = initializeApplicationComponent()
    applicationComponent.inject(this)
  }

  private fun initializeApplicationComponent(): MockApplicationComponent {
    return DaggerMockApplicationComponent.builder()
      .mockApplicationModule(MockApplicationModule(this))
      .mockDatabaseModule(MockDatabaseModule())
      .mockNetworkModule(MockNetworkModule())
      .build()
  }

}