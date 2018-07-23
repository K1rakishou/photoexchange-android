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
import com.kirakishou.photoexchange.mvp.model.net.response.GetUploadedPhotoIdsResponse
import com.kirakishou.photoexchange.mvp.model.net.response.GetUploadedPhotosResponse
import com.kirakishou.photoexchange.mvp.model.other.Constants
import io.reactivex.Single
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito
import java.io.File

@RunWith(AndroidJUnit4::class)
class PhotosActivityViewModelTest {

    private val maxPhotoCacheLiveTime = 10L //10 milliseconds

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
            timeUtils,
            maxPhotoCacheLiveTime
        )

        galleryPhotoRepository = GalleryPhotoRepository(
            database,
            timeUtils
        )

        settingsRepository = SettingsRepository(
            database
        )

        receivedPhotosRepository = ReceivedPhotosRepository(
            database,
            timeUtils,
            maxPhotoCacheLiveTime
        )

        cachedPhotoIdRepository = CachedPhotoIdRepository(
            database
        )

        getUploadedPhotosUseCase = GetUploadedPhotosUseCase(
            uploadedPhotosRepository,
            apiClient
        )

        getGalleryPhotosUseCase = Mockito.mock(GetGalleryPhotosUseCase::class.java)
        getGalleryPhotosInfoUseCase = Mockito.mock(GetGalleryPhotosInfoUseCase::class.java)
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
            schedulerProvider,
            0L,
            0L
        )
    }

    @After
    fun tearDown() {
        FileUtilsImpl().deleteAllFiles(File(tempFilesDir))
        database.close()
    }

    @Test
    fun test_load_page_of_uploaded_photos_should_not_delete_cached_uploaded_photos_and_concat_them_with_fresh_photos() {
        val lastId = Long.MAX_VALUE
        val userId = "123"
        val photosCount = 10

        val photoIds = listOf(1L, 2L, 3L, 4L, 5L)
        val uploadedPhotos = listOf(
            GetUploadedPhotosResponse.UploadedPhotoData(4L, "4", 1.1, 1.1, false, 1L),
            GetUploadedPhotosResponse.UploadedPhotoData(5L, "5", 1.1, 1.1, false, 1L)
        )

        Mockito.`when`(apiClient.getUploadedPhotoIds(userId, lastId, photosCount))
            .thenReturn(Single.just(GetUploadedPhotoIdsResponse.success(photoIds)))
        Mockito.`when`(apiClient.getUploadedPhotos(userId, "4,5"))
            .thenReturn(Single.just(GetUploadedPhotosResponse.success(uploadedPhotos)))

        Mockito.`when`(timeUtils.getTimeFast())
            .thenReturn(1L)
            .thenReturn(2L)
            .thenReturn(3L)
            .thenReturn(19L)

        uploadedPhotosRepository.save(1L, "123", 1.1, 2.2, 10L)
        uploadedPhotosRepository.save(2L, "456", 2.2, 3.3, 10L)
        uploadedPhotosRepository.save(3L, "789", 4.4, 5.6, 10L)

        settingsRepository.saveUserId(userId)

        val values = viewModel.loadNextPageOfUploadedPhotos(lastId, photosCount, true)
            .test()
            .await()
            .values()

        assertEquals(true, values[0] is Either.Value)
        val photos = (values[0] as Either.Value).value

        assertEquals(5, photos.size)

        assertEquals(1L, photos[0].photoId)
        assertEquals("123", photos[0].photoName)
        assertEquals(2L, photos[1].photoId)
        assertEquals("456", photos[1].photoName)
        assertEquals(3L, photos[2].photoId)
        assertEquals("789", photos[2].photoName)
        assertEquals(4L, photos[3].photoId)
        assertEquals("4", photos[3].photoName)
        assertEquals(5L, photos[4].photoId)
        assertEquals("5", photos[4].photoName)
    }

    @Test
    fun test_load_page_of_uploaded_photos_should_delete_cached_uploaded_photos_and_concat_them_with_fresh_photos() {
        val lastId = Long.MAX_VALUE
        val userId = "123"
        val photosCount = 10

        val photoIds = listOf(1L, 2L, 3L, 4L, 5L)
        val uploadedPhotos = listOf(
            GetUploadedPhotosResponse.UploadedPhotoData(1L, "1", 1.1, 1.1, false, 1L),
            GetUploadedPhotosResponse.UploadedPhotoData(2L, "2", 1.1, 1.1, false, 1L),
            GetUploadedPhotosResponse.UploadedPhotoData(3L, "3", 1.1, 1.1, false, 1L),
            GetUploadedPhotosResponse.UploadedPhotoData(4L, "4", 1.1, 1.1, false, 1L),
            GetUploadedPhotosResponse.UploadedPhotoData(5L, "5", 1.1, 1.1, false, 1L)
        )

        Mockito.`when`(apiClient.getUploadedPhotoIds(userId, lastId, photosCount))
            .thenReturn(Single.just(GetUploadedPhotoIdsResponse.success(photoIds)))
        Mockito.`when`(apiClient.getUploadedPhotos(userId, "1,2,3,4,5"))
            .thenReturn(Single.just(GetUploadedPhotosResponse.success(uploadedPhotos)))

        Mockito.`when`(timeUtils.getTimeFast())
            .thenReturn(1L)
            .thenReturn(2L)
            .thenReturn(3L)
            .thenReturn(30L)

        uploadedPhotosRepository.save(1L, "123", 1.1, 2.2, 10L)
        uploadedPhotosRepository.save(2L, "456", 2.2, 3.3, 10L)
        uploadedPhotosRepository.save(3L, "789", 4.4, 5.6, 10L)

        settingsRepository.saveUserId(userId)

        val values = viewModel.loadNextPageOfUploadedPhotos(lastId, photosCount, true)
            .test()
            .await()
            .values()

        assertEquals(true, values[0] is Either.Value)
        val photos = (values[0] as Either.Value).value

        assertEquals(5, photos.size)

        assertEquals(1L, photos[0].photoId)
        assertEquals("1", photos[0].photoName)
        assertEquals(2L, photos[1].photoId)
        assertEquals("2", photos[1].photoName)
        assertEquals(3L, photos[2].photoId)
        assertEquals("3", photos[2].photoName)
        assertEquals(4L, photos[3].photoId)
        assertEquals("4", photos[3].photoName)
        assertEquals(5L, photos[4].photoId)
        assertEquals("5", photos[4].photoName)
    }

    @Test
    fun test_should_clear_old_uploaded_photo_id_cache_and_cache_new_ones() {
        val lastId = Long.MAX_VALUE
        val userId = "123"
        val photosCount = 10

        val photoIds = listOf(1L, 2L, 3L, 4L, 5L)
        val uploadedPhotos = listOf(
            GetUploadedPhotosResponse.UploadedPhotoData(1L, "1", 1.1, 1.1, false, 1L),
            GetUploadedPhotosResponse.UploadedPhotoData(2L, "2", 1.1, 1.1, false, 1L),
            GetUploadedPhotosResponse.UploadedPhotoData(3L, "3", 1.1, 1.1, false, 1L),
            GetUploadedPhotosResponse.UploadedPhotoData(4L, "4", 1.1, 1.1, false, 1L),
            GetUploadedPhotosResponse.UploadedPhotoData(5L, "5", 1.1, 1.1, false, 1L)
        )

        Mockito.`when`(apiClient.getUploadedPhotoIds(userId, lastId, photosCount))
            .thenReturn(Single.just(GetUploadedPhotoIdsResponse.success(photoIds)))
        Mockito.`when`(apiClient.getUploadedPhotos(userId, photoIds.joinToString(Constants.PHOTOS_DELIMITER)))
            .thenReturn(Single.just(GetUploadedPhotosResponse.success(uploadedPhotos)))

        Mockito.`when`(timeUtils.getTimeFast())
            .thenReturn(1L)
            .thenReturn(2L)
            .thenReturn(3L)
            .thenReturn(30L)

        cachedPhotoIdRepository.insert(1L, CachedPhotoIdEntity.PhotoType.UploadedPhoto)
        cachedPhotoIdRepository.insert(1L, CachedPhotoIdEntity.PhotoType.ReceivedPhoto)
        cachedPhotoIdRepository.insert(1L, CachedPhotoIdEntity.PhotoType.GalleryPhoto)

        settingsRepository.saveUserId(userId)

        val values = viewModel.loadNextPageOfUploadedPhotos(lastId, photosCount, true)
            .test()
            .await()
            .values()

        assertEquals(true, values[0] is Either.Value)
        val photos = (values[0] as Either.Value).value

        assertEquals(5, photos.size)
        assertEquals(1L, photos[0].photoId)
        assertEquals("1", photos[0].photoName)
        assertEquals(2L, photos[1].photoId)
        assertEquals("2", photos[1].photoName)
        assertEquals(3L, photos[2].photoId)
        assertEquals("3", photos[2].photoName)
        assertEquals(4L, photos[3].photoId)
        assertEquals("4", photos[3].photoName)
        assertEquals(5L, photos[4].photoId)
        assertEquals("5", photos[4].photoName)

        val cachedPhotoIds = cachedPhotoIdRepository.findAll()
        assertEquals(7, cachedPhotoIds.size)
    }

    @Test
    fun test_should_get_fresh_photos_from_server_when_fragment_was_just_created() {
        val lastId = Long.MAX_VALUE
        val userId = "123"
        val photosCount = 10

        val photoIds = listOf(1L, 2L, 3L, 4L, 5L)
        val uploadedPhotos = listOf(
            GetUploadedPhotosResponse.UploadedPhotoData(1L, "1", 1.1, 1.1, false, 1L),
            GetUploadedPhotosResponse.UploadedPhotoData(2L, "2", 1.1, 1.1, false, 1L),
            GetUploadedPhotosResponse.UploadedPhotoData(3L, "3", 1.1, 1.1, false, 1L),
            GetUploadedPhotosResponse.UploadedPhotoData(4L, "4", 1.1, 1.1, false, 1L),
            GetUploadedPhotosResponse.UploadedPhotoData(5L, "5", 1.1, 1.1, false, 1L)
        )

        Mockito.`when`(apiClient.getUploadedPhotoIds(userId, lastId, photosCount))
            .thenReturn(Single.just(GetUploadedPhotoIdsResponse.success(photoIds)))
        Mockito.`when`(apiClient.getUploadedPhotos(userId, photoIds.joinToString(Constants.PHOTOS_DELIMITER)))
            .thenReturn(Single.just(GetUploadedPhotosResponse.success(uploadedPhotos)))

        Mockito.`when`(timeUtils.getTimeFast())
            .thenReturn(1L)
            .thenReturn(2L)
            .thenReturn(3L)
            .thenReturn(30L)

        settingsRepository.saveUserId(userId)

        val values = viewModel.loadNextPageOfUploadedPhotos(lastId, photosCount, true)
            .test()
            .await()
            .values()

        assertEquals(true, values[0] is Either.Value)
        val photos = (values[0] as Either.Value).value

        assertEquals(5, photos.size)
        assertEquals(1L, photos[0].photoId)
        assertEquals("1", photos[0].photoName)
        assertEquals(2L, photos[1].photoId)
        assertEquals("2", photos[1].photoName)
        assertEquals(3L, photos[2].photoId)
        assertEquals("3", photos[2].photoName)
        assertEquals(4L, photos[3].photoId)
        assertEquals("4", photos[3].photoName)
        assertEquals(5L, photos[4].photoId)
        assertEquals("5", photos[4].photoName)
    }

//    @Test
//    fun test_load_page_of_received_photos_should_not_delete_cached_received_photos_and_concat_them_with_fresh_photos() {
//        val lastId = Long.MAX_VALUE
//        val userId = "123"
//        val photosCount = 10
//
//        val photoIds = listOf(1L, 2L, 3L, 4L, 5L)
//        val uploadedPhotos = listOf(
//            GetUploadedPhotosResponse.UploadedPhotoData(4L, "4", 1.1, 1.1, false, 1L),
//            GetUploadedPhotosResponse.UploadedPhotoData(5L, "5", 1.1, 1.1, false, 1L)
//        )
//
//        Mockito.`when`(apiClient.getUploadedPhotoIds(userId, lastId, photosCount))
//            .thenReturn(Single.just(GetUploadedPhotoIdsResponse.success(photoIds)))
//        Mockito.`when`(apiClient.getUploadedPhotos(userId, "4,5"))
//            .thenReturn(Single.just(GetUploadedPhotosResponse.success(uploadedPhotos)))
//
//        Mockito.`when`(timeUtils.getTimeFast())
//            .thenReturn(1L)
//            .thenReturn(2L)
//            .thenReturn(3L)
//            .thenReturn(19L)
//
//        uploadedPhotosRepository.save(1L, "123", 1.1, 2.2, 10L)
//        uploadedPhotosRepository.save(2L, "456", 2.2, 3.3, 10L)
//        uploadedPhotosRepository.save(3L, "789", 4.4, 5.6, 10L)
//
//        settingsRepository.saveUserId(userId)
//
//        val values = viewModel.loadNextPageOfReceivedPhotos(lastId, photosCount, true)
//            .test()
//            .await()
//            .values()
//
//        assertEquals(true, values[0] is Either.Value)
//        val photos = (values[0] as Either.Value).value
//
//        assertEquals(5, photos.size)
//        assertEquals(1L, photos[0].photoId)
//        assertEquals(2L, photos[1].photoId)
//        assertEquals(3L, photos[2].photoId)
//        assertEquals(4L, photos[3].photoId)
//        assertEquals(5L, photos[4].photoId)
//    }
}




























