package com.kirakishou.photoexchange.di.module

import android.content.SharedPreferences
import com.kirakishou.photoexchange.helper.preference.AppSharedPreference
import dagger.Module
import dagger.Provides
import javax.inject.Singleton

/**
 * Created by kirakishou on 11/4/2017.
 */

@Module
class AppSharedPreferenceModule {

    @Singleton
    @Provides
    fun provideAppSharedPreferences(sharedPreferences: SharedPreferences): AppSharedPreference {
        return AppSharedPreference(sharedPreferences)
    }
}