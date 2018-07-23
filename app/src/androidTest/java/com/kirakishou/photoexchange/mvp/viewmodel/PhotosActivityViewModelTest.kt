package com.kirakishou.photoexchange.mvp.viewmodel

import android.arch.persistence.room.Room
import android.content.Context
import android.support.test.InstrumentationRegistry
import android.support.test.runner.AndroidJUnit4
import com.kirakishou.photoexchange.helper.Either
import com.kirakishou.photoexchange.helper.api.ApiClient
import com.kirakishou.photoexchange.helper.concurrency.rx.scheduler.SchedulerProvider
import com.kirakishou.photoexchange.helper.concurrency.rx.scheduler.TestSchedulers
import com.kirakishou.photoexchange.helper.database.MyDatabase
import com.kirakishou.photoexchange.helper.database.entity.CachedPhotoIdEntity
import com.kirakishou.photoexchange.helper.database.repository.*
import com.kirakishou.photoexchange.helper.util.FileUtils
import com.kirakishou.photoexchange.helper.util.FileUtilsImpl
import com.kirakishou.photoexchange.helper.util.TimeUtils
import com.kirakishou.photoexchange.interactors.*
import com.kirakishou.photoexchange.mvp.model.UploadedPhoto
import io.reactivex.Observable
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito
import java.io.File

@RunWith(AndroidJUnit4::class)
class PhotosActivityViewModelTest {

    lateinit var database: MyDatabase
    lateinit var appContext: Context
    lateinit var targetContext: Context
    lateinit var tempFilesDir: String
    lateinit var timeUtils: TimeUtils
    lateinit var fileUtils: FileUtils
    lateinit var apiClient: ApiClient
    lateinit var tempFileRepository: TempFileRepository
    lateinit var takenPhotosRepository: TakenPhotosRepository
    lateinit var uploadedPhotosRepository: UploadedPhotosRepository
    lateinit var galleryPhotoRepository: GalleryPhotoRepository
    lateinit var settingsRepository: SettingsRepository
    lateinit var receivedPhotosRepository: ReceivedPhotosRepository
    lateinit var cachedPhotoIdRepository: CachedPhotoIdRepository
    lateinit var getGalleryPhotosUseCase: GetGalleryPhotosUseCase
    lateinit var getGalleryPhotosInfoUseCase: GetGalleryPhotosInfoUseCase
    lateinit var getUploadedPhotosUseCase: GetUploadedPhotosUseCase
    lateinit var getReceivedPhotosUseCase: GetReceivedPhotosUseCase
    lateinit var favouritePhotoUseCase: FavouritePhotoUseCase
    lateinit var reportPhotoUseCase: ReportPhotoUseCase
    lateinit var schedulerProvider: SchedulerProvider

    lateinit var viewModel: PhotosActivityViewModel

    @Before
    fun init() {
        apiClient = Mockito.mock(ApiClient::class.java)

        appContext = InstrumentationRegistry.getContext()
        targetContext = InstrumentationRegistry.getTargetContext()
        database = Room.inMemoryDatabaseBuilder(appContext, MyDatabase::class.java).build()
        timeUtils = Mockito.mock(TimeUtils::class.java)
        fileUtils = FileUtilsImpl()
        tempFilesDir = targetContext.getDir("test_temp_files", Context.MODE_PRIVATE).absolutePath

        tempFileRepository = TempFileRepository(
            tempFilesDir,
            database,
            timeUtils,
            fileUtils
        )

        takenPhotosRepository = TakenPhotosRepository(
            timeUtils,
            database,
            tempFileRepository
        )

        uploadedPhotosRepository = UploadedPhotosRepository(
            database,
            timeUtils
        )

        galleryPhotoRepository = GalleryPhotoRepository(
            database,
            timeUtils
        )

        settingsRepository = SettingsRepository(
            database
        )

        receivedPhotosRepository = ReceivedPhotosRepository(
            database
        )

        cachedPhotoIdRepository = CachedPhotoIdRepository(
            database
        )

        getGalleryPhotosUseCase = Mockito.mock(GetGalleryPhotosUseCase::class.java)
        getGalleryPhotosInfoUseCase = Mockito.mock(GetGalleryPhotosInfoUseCase::class.java)
        getUploadedPhotosUseCase = Mockito.mock(GetUploadedPhotosUseCase::class.java)
        getReceivedPhotosUseCase = Mockito.mock(GetReceivedPhotosUseCase::class.java)
        favouritePhotoUseCase = Mockito.mock(FavouritePhotoUseCase::class.java)
        reportPhotoUseCase = Mockito.mock(ReportPhotoUseCase::class.java)

        schedulerProvider = TestSchedulers()

        viewModel = PhotosActivityViewModel(
            takenPhotosRepository,
            uploadedPhotosRepository,
            galleryPhotoRepository,
            settingsRepository,
            receivedPhotosRepository,
            cachedPhotoIdRepository,
            getGalleryPhotosUseCase,
            getGalleryPhotosInfoUseCase,
            getUploadedPhotosUseCase,
            getReceivedPhotosUseCase,
            favouritePhotoUseCase,
            reportPhotoUseCase,
            schedulerProvider
        )
    }

    @After
    fun tearDown() {
        FileUtilsImpl().deleteAllFiles(File(tempFilesDir))
        database.close()
    }

    @Test
    fun test_freshly_created_fragment_loading_page_of_uploaded_photos() {
        val lastId = Long.MAX_VALUE
        val userId = "123"
        val photosCount = 10
        val uploadedPhotos = listOf(
            UploadedPhoto(1L, "1", 1.1, 1.1, false, 1L),
            UploadedPhoto(2L, "2", 1.1, 1.1, false, 1L),
            UploadedPhoto(3L, "3", 1.1, 1.1, false, 1L),
            UploadedPhoto(4L, "4", 1.1, 1.1, false, 1L),
            UploadedPhoto(5L, "5", 1.1, 1.1, false, 1L)
        )

        cachedPhotoIdRepository.insert(1L, CachedPhotoIdEntity.PhotoType.UploadedPhoto)
        cachedPhotoIdRepository.insert(1L, CachedPhotoIdEntity.PhotoType.ReceivedPhoto)
        cachedPhotoIdRepository.insert(1L, CachedPhotoIdEntity.PhotoType.GalleryPhoto)
        settingsRepository.saveUserId(userId)

        Mockito.`when`(getUploadedPhotosUseCase.loadPageOfPhotos(userId, lastId, photosCount))
            .thenReturn(Observable.just(Either.Value(uploadedPhotos)))

        val values = viewModel.loadNextPageOfUploadedPhotos(lastId, photosCount, true)
            .test()
            .await()
            .values()

        assertEquals(true, values[0] is Either.Value)
        val photos = (values[0] as Either.Value).value

        assertEquals(5, photos.size)
        assertEquals(1L, photos[0].photoId)
        assertEquals(2L, photos[1].photoId)
        assertEquals(3L, photos[2].photoId)
        assertEquals(4L, photos[3].photoId)
        assertEquals(5L, photos[4].photoId)

        val cachedPhotoIds = cachedPhotoIdRepository.findAll()
        assertEquals(7, cachedPhotoIds.size)
    }
}




























