package com.kirakishou.photoexchange.di.module

import android.arch.persistence.room.Room
import android.content.Context
import com.kirakishou.photoexchange.helper.api.ApiClient
import com.kirakishou.photoexchange.helper.database.MyDatabase
import com.kirakishou.photoexchange.helper.database.repository.TakenPhotosRepository
import dagger.Module
import dagger.Provides
import javax.inject.Singleton

/**
 * Created by kirakishou on 3/8/2018.
 */

@Module
class InMemoryDatabaseModule {

    @Singleton
    @Provides
    fun provideDatabase(context: Context): MyDatabase {
        return Room.inMemoryDatabaseBuilder(context, MyDatabase::class.java).build()
    }

    @Singleton
    @Provides
    fun provideMyPhotoRepository(context: Context,
                                 database: MyDatabase): TakenPhotosRepository {
        val filesDir = context.filesDir.absolutePath
        return TakenPhotosRepository(filesDir, database)
    }
}