package com.kirakishou.photoexchange.tests.repository

import android.content.Context
import android.support.test.runner.AndroidJUnit4
import com.kirakishou.photoexchange.helper.database.MyDatabase
import com.kirakishou.photoexchange.helper.database.repository.PhotosRepository
import org.junit.runner.RunWith

/**
 * Created by kirakishou on 3/10/2018.
 */


@RunWith(AndroidJUnit4::class)
class PhotosRepositoryTests {

    lateinit var appContext: Context
    lateinit var targetContext: Context
    lateinit var database: MyDatabase

    lateinit var myPhotosRepository: PhotosRepository

//    @Before
//    fun init() {
//        appContext = InstrumentationRegistry.getContext()
//        targetContext = InstrumentationRegistry.getTargetContext()
//        database = Room.inMemoryDatabaseBuilder(appContext, MyDatabase::class.java).execute()
//        val tempFilesDir = targetContext.getDir("test_temp_files", Context.MODE_PRIVATE).absolutePath
//
//        val tempFilesRepository = TempFileRepository(tempFilesDir, database)
//        myPhotosRepository = PhotosRepository(database, tempFilesRepository)
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
//            myPhotosRepository.init()
//
//            val myPhoto = myPhotosRepository.insert(MyPhotoEntity.create())
//            val myPhoto2 = myPhotosRepository.insert(MyPhotoEntity.create())
//
//            assertEquals(1, myPhoto.id)
//            assertEquals(2, myPhoto2.id)
//
//            assertEquals(true, myPhotosRepository.delete(myPhoto))
//            assertEquals(true, myPhotosRepository.delete(myPhoto2))
//            assertEquals(true, myPhotosRepository.findAll().isEmpty())
//        }
//    }
//
//    @Test
//    fun should_delete_all_with_photo_state() {
//        runBlocking {
//            myPhotosRepository.init()
//
//            myPhotosRepository.insert(MyPhotoEntity.create(PhotoState.PHOTO_TAKEN))
//            myPhotosRepository.insert(MyPhotoEntity.create(PhotoState.PHOTO_UPLOADING))
//            myPhotosRepository.insert(MyPhotoEntity.create(PhotoState.PHOTO_UPLOADING))
//            myPhotosRepository.insert(MyPhotoEntity.create(PhotoState.PHOTO_UPLOADED))
//            myPhotosRepository.insert(MyPhotoEntity.create(PhotoState.PHOTO_UPLOADED))
//            myPhotosRepository.insert(MyPhotoEntity.create(PhotoState.PHOTO_UPLOADED))
//
//            assertEquals(true, myPhotosRepository.deleteAllWithState(PhotoState.PHOTO_TAKEN))
//            myPhotosRepository.findAll().let { allPhotos ->
//                assertEquals(5, allPhotos.size)
//
//                val noPhotoTakenPhotos = allPhotos.none { it.photoState == PhotoState.PHOTO_TAKEN }
//                assertEquals(true, noPhotoTakenPhotos)
//            }
//
//            assertEquals(true, myPhotosRepository.deleteAllWithState(PhotoState.PHOTO_UPLOADING))
//            myPhotosRepository.findAll().let { allPhotos ->
//                assertEquals(3, allPhotos.size)
//
//                val noPhotoTakenPhotos = allPhotos.none { it.photoState == PhotoState.PHOTO_UPLOADING }
//                assertEquals(true, noPhotoTakenPhotos)
//            }
//
//            assertEquals(true, myPhotosRepository.deleteAllWithState(PhotoState.PHOTO_UPLOADED))
//            myPhotosRepository.findAll().let { allPhotos ->
//                assertEquals(0, allPhotos.size)
//            }
//        }
//    }
}