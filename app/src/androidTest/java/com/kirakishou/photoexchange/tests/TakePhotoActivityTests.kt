package com.kirakishou.photoexchange.tests

import android.arch.persistence.room.Room
import android.content.Context
import android.support.test.InstrumentationRegistry
import android.support.test.runner.AndroidJUnit4
import com.kirakishou.photoexchange.di.module.MockCoroutineThreadPoolProviderModule
import com.kirakishou.photoexchange.helper.database.MyDatabase
import com.kirakishou.photoexchange.helper.database.repository.MyPhotoRepository
import com.kirakishou.photoexchange.helper.database.repository.TempFileRepository
import com.kirakishou.photoexchange.mvp.model.MyPhoto
import com.kirakishou.photoexchange.mvp.model.state.PhotoState
import com.kirakishou.photoexchange.mvp.view.TakePhotoActivityView
import com.kirakishou.photoexchange.mvp.viewmodel.TakePhotoActivityViewModel
import com.nhaarman.mockito_kotlin.any
import com.nhaarman.mockito_kotlin.argumentCaptor
import com.nhaarman.mockito_kotlin.verify
import com.nhaarman.mockito_kotlin.whenever
import io.reactivex.Single
import kotlinx.coroutines.experimental.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito

/**
 * Created by kirakishou on 3/8/2018.
 */

@RunWith(AndroidJUnit4::class)
class TakePhotoActivityTests {

    lateinit var mockedView: TakePhotoActivityView
    lateinit var appContext: Context
    lateinit var database: MyDatabase
    lateinit var coroutinesPool: MockCoroutineThreadPoolProviderModule.TestCoroutineThreadPoolProvider
    lateinit var tempFilesDir: String

    lateinit var realTempFilesRepository: TempFileRepository
    lateinit var mockTempFileRepository: TempFileRepository

    lateinit var realMyPhotosRepository: MyPhotoRepository
    lateinit var mockMyPhotoRepository: MyPhotoRepository

    @Before
    fun setup() {
        appContext = InstrumentationRegistry.getContext()
        mockedView = Mockito.mock(TakePhotoActivityView::class.java)

        database = Room.inMemoryDatabaseBuilder(appContext, MyDatabase::class.java).build()
        coroutinesPool = MockCoroutineThreadPoolProviderModule.TestCoroutineThreadPoolProvider()
        tempFilesDir = appContext.getDir("temp_files", Context.MODE_PRIVATE).absolutePath

        realTempFilesRepository = TempFileRepository(tempFilesDir, database, coroutinesPool)
        realMyPhotosRepository = MyPhotoRepository(database, realTempFilesRepository, coroutinesPool)

        mockTempFileRepository = Mockito.mock(TempFileRepository::class.java)
        mockMyPhotoRepository = Mockito.mock(MyPhotoRepository::class.java)
    }

//    @Test
//    fun testCreateFile() {
//        val tempFilesDir = appContext.getDir("temp_files", Context.MODE_PRIVATE)
//
//        if (!tempFilesDir.exists()) {
//            assertEquals(true, tempFilesDir.mkdirs())
//        }
//
//        val file = File.createTempFile("temp", ".file")
//        assertEquals(true, file.exists())
//    }

    @Test
    fun should_take_photo_and_store_photo_info_in_the_database() {
        runBlocking {
            val viewModel = TakePhotoActivityViewModel(mockedView, coroutinesPool, realMyPhotosRepository)

            whenever(mockedView.takePhoto(any())).thenReturn(Single.just(true))

            viewModel.init()
            viewModel.takePhoto()

            verify(mockedView).hideTakePhotoButton()
            verify(mockedView).showTakePhotoButton()

            argumentCaptor<MyPhoto>().apply {
                verify(mockedView).onPhotoTaken(capture())

                assertEquals(1L, firstValue.id)
                assertEquals(PhotoState.PHOTO_TAKEN, firstValue.photoState)
                assertEquals(true, firstValue.photoTempFile!!.absolutePath.isNotEmpty())
            }
        }
    }
}

























