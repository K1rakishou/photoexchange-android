package com.kirakishou.photoexchange.tests.repository

import android.arch.persistence.room.Room
import android.content.Context
import android.support.test.InstrumentationRegistry
import android.support.test.runner.AndroidJUnit4
import com.kirakishou.photoexchange.helper.database.MyDatabase
import com.kirakishou.photoexchange.helper.database.entity.MyPhotoEntity
import com.kirakishou.photoexchange.helper.database.repository.MyPhotoRepository
import com.kirakishou.photoexchange.helper.database.repository.TempFileRepository
import com.kirakishou.photoexchange.mvp.model.PhotoState
import kotlinx.coroutines.experimental.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.Assert.assertEquals
import org.junit.runner.RunWith

/**
 * Created by kirakishou on 3/10/2018.
 */


@RunWith(AndroidJUnit4::class)
class MyPhotoRepositoryTests {

    lateinit var appContext: Context
    lateinit var targetContext: Context
    lateinit var database: MyDatabase

    lateinit var myPhotosRepository: MyPhotoRepository

    @Before
    fun init() {
        appContext = InstrumentationRegistry.getContext()
        targetContext = InstrumentationRegistry.getTargetContext()
        database = Room.inMemoryDatabaseBuilder(appContext, MyDatabase::class.java).build()
        val tempFilesDir = targetContext.getDir("test_temp_files", Context.MODE_PRIVATE).absolutePath

        val tempFilesRepository = TempFileRepository(tempFilesDir, database)
        myPhotosRepository = MyPhotoRepository(database, tempFilesRepository)
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun should_delete_after_insert() {
        runBlocking {
            myPhotosRepository.init()

            val myPhoto = myPhotosRepository.insert(MyPhotoEntity.create())
            val myPhoto2 = myPhotosRepository.insert(MyPhotoEntity.create())
            assertEquals(1, myPhoto.id)
            assertEquals(2, myPhoto2.id)

            assertEquals(true, myPhotosRepository.delete(myPhoto))
            assertEquals(true, myPhotosRepository.delete(myPhoto2))
            assertEquals(true, myPhotosRepository.findAll().isEmpty())
        }
    }

    @Test
    fun should_delete_all_with_photo_state() {
        runBlocking {
            myPhotosRepository.init()

            myPhotosRepository.insert(MyPhotoEntity.create(PhotoState.PHOTO_TAKEN))
            myPhotosRepository.insert(MyPhotoEntity.create(PhotoState.PHOTO_UPLOADING))
            myPhotosRepository.insert(MyPhotoEntity.create(PhotoState.PHOTO_UPLOADING))
            myPhotosRepository.insert(MyPhotoEntity.create(PhotoState.PHOTO_UPLOADED))
            myPhotosRepository.insert(MyPhotoEntity.create(PhotoState.PHOTO_UPLOADED))
            myPhotosRepository.insert(MyPhotoEntity.create(PhotoState.PHOTO_UPLOADED))

            myPhotosRepository.deleteAllWithState(PhotoState.PHOTO_TAKEN)
            myPhotosRepository.findAll().let { allPhotos ->
                assertEquals(5, allPhotos.size)

                val noPhotoTakenPhotos = allPhotos.none { it.photoState == PhotoState.PHOTO_TAKEN }
                assertEquals(true, noPhotoTakenPhotos)
            }

            myPhotosRepository.deleteAllWithState(PhotoState.PHOTO_UPLOADING)
            myPhotosRepository.findAll().let { allPhotos ->
                assertEquals(3, allPhotos.size)

                val noPhotoTakenPhotos = allPhotos.none { it.photoState == PhotoState.PHOTO_UPLOADING }
                assertEquals(true, noPhotoTakenPhotos)
            }

            myPhotosRepository.deleteAllWithState(PhotoState.PHOTO_UPLOADED)
            myPhotosRepository.findAll().let { allPhotos ->
                assertEquals(0, allPhotos.size)
            }
        }
    }
}