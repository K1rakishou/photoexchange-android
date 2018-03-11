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
 * Created by kirakishou on 3/4/2018.
 */

@Module
open class DatabaseModule(
    val dbName: String
) {

    @Singleton
    @Provides
    open fun provideDatabase(context: Context): MyDatabase {
        return Room.databaseBuilder(context, MyDatabase::class.java, dbName).build()
    }

    @Singleton
    @Provides
    open fun provideTempFileRepository(context: Context, database: MyDatabase): TempFileRepository {

        val filesDir = context.filesDir.absolutePath
        return TempFileRepository(filesDir, database)
    }

    @Singleton
    @Provides
    open fun provideMyPhotoRepository(database: MyDatabase,
                                      tempFileRepository: TempFileRepository): MyPhotoRepository {
        return MyPhotoRepository(database, tempFileRepository)
    }
}