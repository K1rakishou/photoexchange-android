package com.kirakishou.photoexchange.helper.database.repository

import android.arch.persistence.room.Room
import android.content.Context
import android.support.test.InstrumentationRegistry
import android.support.test.runner.AndroidJUnit4
import com.kirakishou.photoexchange.helper.database.MyDatabase
import com.kirakishou.photoexchange.helper.util.TimeUtils
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito

@RunWith(AndroidJUnit4::class)
class GalleryPhotoRepositoryTest {
    lateinit var appContext: Context
    lateinit var targetContext: Context
    lateinit var database: MyDatabase
    lateinit var timeUtils: TimeUtils
    lateinit var galleryPhotoRepository: GalleryPhotoRepository

    @Before
    fun init() {
        appContext = InstrumentationRegistry.getContext()
        targetContext = InstrumentationRegistry.getTargetContext()
        database = Room.inMemoryDatabaseBuilder(appContext, MyDatabase::class.java).build()
        timeUtils = Mockito.mock(TimeUtils::class.java)

        galleryPhotoRepository = GalleryPhotoRepository(database, timeUtils, 10L, 10L)
    }

    @After
    fun tearDown() {
        database.close()
    }
}