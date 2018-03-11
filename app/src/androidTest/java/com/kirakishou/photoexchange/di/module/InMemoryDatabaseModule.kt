package com.kirakishou.photoexchange.di.module

import android.arch.persistence.room.Room
import android.content.Context
import com.kirakishou.photoexchange.helper.concurrency.coroutine.CoroutineThreadPoolProvider
import com.kirakishou.photoexchange.helper.database.MyDatabase
import com.kirakishou.photoexchange.helper.database.repository.MyPhotoRepository
import com.kirakishou.photoexchange.helper.database.repository.TempFileRepository
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
    fun provideMyPhotoRepository(database: MyDatabase,
                                 tempFileRepository: TempFileRepository): MyPhotoRepository {
        return MyPhotoRepository(database, tempFileRepository)
    }

    @Singleton
    @Provides
    fun provideTempFileRepository(context: Context, database: MyDatabase): TempFileRepository {
        val filesDir = context.filesDir.absolutePath
        return TempFileRepository(filesDir, database)
    }
}