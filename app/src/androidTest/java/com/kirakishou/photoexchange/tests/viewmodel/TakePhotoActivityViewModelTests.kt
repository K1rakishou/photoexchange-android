package com.kirakishou.photoexchange.tests.viewmodel

import android.arch.persistence.room.Room
import android.content.Context
import android.support.test.InstrumentationRegistry
import android.support.test.runner.AndroidJUnit4
import com.kirakishou.photoexchange.di.module.MockCoroutineThreadPoolProviderModule
import com.kirakishou.photoexchange.helper.database.MyDatabase
import com.kirakishou.photoexchange.mvp.view.TakePhotoActivityView
import com.kirakishou.photoexchange.tests.AbstractTest
import org.junit.After
import org.junit.Before
import org.junit.runner.RunWith
import org.mockito.Mockito
import java.io.File

/**
 * Created by kirakishou on 3/8/2018.
 */

@RunWith(AndroidJUnit4::class)
class TakePhotoActivityViewModelTests : AbstractTest() {

    lateinit var mockedView: TakePhotoActivityView
    lateinit var appContext: Context
    lateinit var targetContext: Context
    lateinit var database: MyDatabase
    lateinit var coroutinesPool: MockCoroutineThreadPoolProviderModule.TestCoroutineThreadPoolProvider
    lateinit var tempFilesDir: String

    @Before
    fun setup() {
        appContext = InstrumentationRegistry.getContext()
        targetContext = InstrumentationRegistry.getTargetContext()

        mockedView = Mockito.mock(TakePhotoActivityView::class.java)
        database = Room.inMemoryDatabaseBuilder(appContext, MyDatabase::class.java).build()
        coroutinesPool = MockCoroutineThreadPoolProviderModule.TestCoroutineThreadPoolProvider()
        tempFilesDir = targetContext.getDir("test_temp_files", Context.MODE_PRIVATE).absolutePath
    }

    @After
    fun tearDown() {
        //hack
        if (::database.isInitialized) {
            database.close()
        }

        if (::tempFilesDir.isInitialized) {
            deleteDir(File(tempFilesDir))
        }
    }

//    @Test
//    fun should_take_photo_and_store_photo_info_in_the_database() {
//        runBlocking {
//            val realTempFilesRepository = TempFileRepository(tempFilesDir, database)
//            val realMyPhotosRepository = PhotosRepository(database, realTempFilesRepository)
//            val viewModel = TakePhotoActivityViewModel(mockedView, coroutinesPool, realMyPhotosRepository)
//
//            whenever(mockedView.takePhoto(any())).thenReturn(Single.just(true))
//
//            viewModel.attach()
//            viewModel.takePhoto()
//
//            verify(mockedView).hideControls()
//            verify(mockedView, never()).showControls()
//
//            argumentCaptor<MyPhoto>().apply {
//                verify(mockedView).onPhotoTaken(capture())
//
//                val myPhoto = realMyPhotosRepository.findAll().first()
//
//                assertEquals(1L, firstValue.id)
//                assertEquals(PhotoState.PHOTO_TAKEN, firstValue.photoState)
//                assertEquals(true, firstValue.photoTempFile!!.absolutePath.isNotEmpty())
//
//                assertEquals(firstValue.id, myPhoto.id)
//                assertEquals(firstValue.photoState, myPhoto.photoState)
//                assertEquals(firstValue.photoTempFile!!.absolutePath, myPhoto.photoTempFile!!.absolutePath)
//            }
//        }
//    }
//
//    @Test
//    fun should_cleanup_and_show_toast_if_repository_insert_fails() {
//        runBlocking {
//            val realTempFilesRepository = TempFileRepository(tempFilesDir, database)
//            val spyMyPhotosRepository = Mockito.spy(PhotosRepository(database, realTempFilesRepository))
//
//            val result = async(Unconfined) { MyPhoto.empty() }
//            doReturn(result).`when`(spyMyPhotosRepository).insert(any())
//
//            val viewModel = TakePhotoActivityViewModel(mockedView, coroutinesPool, spyMyPhotosRepository)
//
//            viewModel.attach()
//            viewModel.takePhoto()
//
//            verify(mockedView).showToast(anyString(), anyInt())
//            verify(mockedView).showControls()
//        }
//    }
}
























