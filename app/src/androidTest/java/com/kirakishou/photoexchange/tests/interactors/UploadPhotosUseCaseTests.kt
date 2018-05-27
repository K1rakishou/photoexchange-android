package com.kirakishou.photoexchange.tests.interactors

import android.arch.persistence.room.Room
import android.content.Context
import android.support.test.InstrumentationRegistry
import android.support.test.runner.AndroidJUnit4
import com.google.gson.GsonBuilder
import com.kirakishou.photoexchange.helper.api.ApiClient
import com.kirakishou.photoexchange.helper.api.ApiService
import com.kirakishou.photoexchange.helper.concurrency.rx.scheduler.TestSchedulers
import com.kirakishou.photoexchange.helper.database.MyDatabase
import com.kirakishou.photoexchange.helper.database.repository.TakenPhotosRepository
import com.kirakishou.photoexchange.helper.database.repository.UploadedPhotosRepository
import com.kirakishou.photoexchange.helper.intercom.event.UploadedPhotosFragmentEvent
import com.kirakishou.photoexchange.interactors.UploadPhotosUseCase
import com.kirakishou.photoexchange.mvp.model.net.response.UploadPhotoResponse
import com.kirakishou.photoexchange.mvp.model.other.LonLat
import com.kirakishou.photoexchange.service.UploadPhotoServiceCallbacks
import com.kirakishou.photoexchange.tests.AbstractTest
import io.reactivex.Single
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito
import retrofit2.Response
import java.io.File
import java.lang.ref.WeakReference

@RunWith(AndroidJUnit4::class)
class UploadPhotosUseCaseTests : AbstractTest() {

//    lateinit var database: MyDatabase
//    lateinit var tempFilesDir: String
//
//    lateinit var apiService: ApiService
//
//    lateinit var takenPhotosRepository: TakenPhotosRepository
//    lateinit var uploadedPhotosRepository: UploadedPhotosRepository
//    lateinit var uploadPhotosUseCase: UploadPhotosUseCase
//
//    @Before
//    fun setup() {
//        val appContext = InstrumentationRegistry.getContext()
//        val targetContext = InstrumentationRegistry.getTargetContext()
//
//        database = Room.inMemoryDatabaseBuilder(appContext, MyDatabase::class.java).build()
//        tempFilesDir = targetContext.getDir("test_temp_files", Context.MODE_PRIVATE).absolutePath
//
//        takenPhotosRepository = Mockito.spy(TakenPhotosRepository(tempFilesDir, database))
//        uploadedPhotosRepository = Mockito.spy(UploadedPhotosRepository(database))
//
//        apiService = Mockito.spy(ApiService::class.java)
//        val gson = GsonBuilder().create()
//        val schedulerProvider = TestSchedulers()
//        val apiClient = Mockito.spy(ApiClient(apiService, gson, schedulerProvider))
//
//        uploadPhotosUseCase = UploadPhotosUseCase(
//            database,
//            takenPhotosRepository,
//            uploadedPhotosRepository,
//            apiClient
//        )
//    }
//
//    @After
//    fun tearDown() {
//        if (::database.isInitialized) {
//            database.close()
//        }
//
//        if (::tempFilesDir.isInitialized) {
//            deleteDir(File(tempFilesDir))
//        }
//    }
//
//    @Test
//    fun `test`() {
//        val file1 = takenPhotosRepository.createFile()
//        val file2 = takenPhotosRepository.createFile()
//        val file3 = takenPhotosRepository.createFile()
//
//        val takenPhoto1 = takenPhotosRepository.saveTakenPhoto(file1)
//        val takenPhoto2 = takenPhotosRepository.saveTakenPhoto(file2)
//        val takenPhoto3 = takenPhotosRepository.saveTakenPhoto(file3)
//
//        Mockito.`when`(apiService.uploadPhoto(Mockito.anyObject(), Mockito.anyObject())).thenReturn(Single.just(Response.success(UploadPhotoResponse.success("123"))))
//
//        val callbacks = WeakReference(object : UploadPhotoServiceCallbacks {
//            override fun getCurrentLocation(): Single<LonLat> {
//                return Single.just(LonLat(11.4, 55.2))
//            }
//
//            override fun onUploadingEvent(event: UploadedPhotosFragmentEvent.PhotoUploadEvent) {
//            }
//
//            override fun onError(error: Throwable) {
//            }
//
//            override fun stopService() {
//            }
//        })
//
//        uploadPhotosUseCase.uploadPhoto(takenPhoto1, "sss", LonLat(11.4, 55.2), callbacks)
}
