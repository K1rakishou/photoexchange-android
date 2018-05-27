package com.kirakishou.photoexchange.tests.repository

import android.content.Context
import android.support.test.runner.AndroidJUnit4
import com.kirakishou.photoexchange.helper.database.MyDatabase
import com.kirakishou.photoexchange.helper.database.repository.TakenPhotosRepository
import org.junit.runner.RunWith

/**
 * Created by kirakishou on 3/10/2018.
 */


@RunWith(AndroidJUnit4::class)
class TakenPhotosRepositoryTests {

    lateinit var appContext: Context
    lateinit var targetContext: Context
    lateinit var database: MyDatabase

    lateinit var myTakenPhotosRepository: TakenPhotosRepository

//    @Before
//    fun init() {
//        appContext = InstrumentationRegistry.getContext()
//        targetContext = InstrumentationRegistry.getTargetContext()
//        database = Room.inMemoryDatabaseBuilder(appContext, MyDatabase::class.java).execute()
//        val tempFilesDir = targetContext.getDir("test_temp_files", Context.MODE_PRIVATE).absolutePath
//
//        val tempFilesRepository = TempFileRepository(tempFilesDir, database)
//        myTakenPhotosRepository = TakenPhotosRepository(database, tempFilesRepository)
//    }
//
//    @After
//    fun tearDown() {
//        database.close()
//    }
//
//    @Test
//    fun should_delete_after_insert() {
//        runBlocking {
//            myTakenPhotosRepository.init()
//
//            val myPhoto = myTakenPhotosRepository.save(TakenPhotoEntity.create())
//            val myPhoto2 = myTakenPhotosRepository.save(TakenPhotoEntity.create())
//
//            assertEquals(1, myPhoto.id)
//            assertEquals(2, myPhoto2.id)
//
//            assertEquals(true, myTakenPhotosRepository.delete(myPhoto))
//            assertEquals(true, myTakenPhotosRepository.delete(myPhoto2))
//            assertEquals(true, myTakenPhotosRepository.findAll().isEmpty())
//        }
//    }
//
//    @Test
//    fun should_delete_all_with_photo_state() {
//        runBlocking {
//            myTakenPhotosRepository.init()
//
//            myTakenPhotosRepository.save(TakenPhotoEntity.create(PhotoState.PHOTO_TAKEN))
//            myTakenPhotosRepository.save(TakenPhotoEntity.create(PhotoState.PHOTO_UPLOADING))
//            myTakenPhotosRepository.save(TakenPhotoEntity.create(PhotoState.PHOTO_UPLOADING))
//            myTakenPhotosRepository.save(TakenPhotoEntity.create(PhotoState.PHOTO_UPLOADED))
//            myTakenPhotosRepository.save(TakenPhotoEntity.create(PhotoState.PHOTO_UPLOADED))
//            myTakenPhotosRepository.save(TakenPhotoEntity.create(PhotoState.PHOTO_UPLOADED))
//
//            assertEquals(true, myTakenPhotosRepository.deleteAllWithState(PhotoState.PHOTO_TAKEN))
//            myTakenPhotosRepository.findAll().let { allPhotos ->
//                assertEquals(5, allPhotos.size)
//
//                val noPhotoTakenPhotos = allPhotos.none { it.photoState == PhotoState.PHOTO_TAKEN }
//                assertEquals(true, noPhotoTakenPhotos)
//            }
//
//            assertEquals(true, myTakenPhotosRepository.deleteAllWithState(PhotoState.PHOTO_UPLOADING))
//            myTakenPhotosRepository.findAll().let { allPhotos ->
//                assertEquals(3, allPhotos.size)
//
//                val noPhotoTakenPhotos = allPhotos.none { it.photoState == PhotoState.PHOTO_UPLOADING }
//                assertEquals(true, noPhotoTakenPhotos)
//            }
//
//            assertEquals(true, myTakenPhotosRepository.deleteAllWithState(PhotoState.PHOTO_UPLOADED))
//            myTakenPhotosRepository.findAll().let { allPhotos ->
//                assertEquals(0, allPhotos.size)
//            }
//        }
//    }
}