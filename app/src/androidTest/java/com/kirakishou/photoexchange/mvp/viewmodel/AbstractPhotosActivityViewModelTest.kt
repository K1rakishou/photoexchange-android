package com.kirakishou.photoexchange.mvp.viewmodel

import android.arch.persistence.room.Room
import android.content.Context
import android.support.annotation.CallSuper
import android.support.test.InstrumentationRegistry
import com.kirakishou.photoexchange.helper.api.ApiClient
import com.kirakishou.photoexchange.helper.concurrency.rx.scheduler.SchedulerProvider
import com.kirakishou.photoexchange.helper.concurrency.rx.scheduler.TestSchedulers
import com.kirakishou.photoexchange.helper.database.MyDatabase
import com.kirakishou.photoexchange.helper.database.repository.*
import com.kirakishou.photoexchange.helper.util.FileUtils
import com.kirakishou.photoexchange.helper.util.FileUtilsImpl
import com.kirakishou.photoexchange.helper.util.TimeUtils
import com.kirakishou.photoexchange.interactors.*
import org.mockito.Mockito
import java.io.File

abstract class AbstractPhotosActivityViewModelTest {
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
    lateinit var getGalleryPhotosUseCase: GetGalleryPhotosUseCase
    lateinit var getGalleryPhotosInfoUseCase: GetGalleryPhotosInfoUseCase
    lateinit var getUploadedPhotosUseCase: GetUploadedPhotosUseCase
    lateinit var getReceivedPhotosUseCase: GetReceivedPhotosUseCase
    lateinit var favouritePhotoUseCase: FavouritePhotoUseCase
    lateinit var reportPhotoUseCase: ReportPhotoUseCase
    lateinit var schedulerProvider: SchedulerProvider

    lateinit var viewModel: PhotosActivityViewModel

    @CallSuper
    open fun init() {
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
            timeUtils,
            maxPhotoCacheLiveTime,
            maxPhotoCacheLiveTime
        )

        settingsRepository = SettingsRepository(
            database
        )

        receivedPhotosRepository = ReceivedPhotosRepository(
            database,
            timeUtils,
            maxPhotoCacheLiveTime
        )

        getUploadedPhotosUseCase = GetUploadedPhotosUseCase(
            uploadedPhotosRepository,
            apiClient
        )

        getReceivedPhotosUseCase = GetReceivedPhotosUseCase(
            database,
            receivedPhotosRepository,
            uploadedPhotosRepository,
            apiClient
        )

        getGalleryPhotosUseCase = GetGalleryPhotosUseCase(
            apiClient,
            galleryPhotoRepository
        )

        getGalleryPhotosInfoUseCase = GetGalleryPhotosInfoUseCase(
            apiClient,
            galleryPhotoRepository
        )

        favouritePhotoUseCase = Mockito.mock(FavouritePhotoUseCase::class.java)
        reportPhotoUseCase = Mockito.mock(ReportPhotoUseCase::class.java)

        schedulerProvider = TestSchedulers()

        viewModel = PhotosActivityViewModel(
            takenPhotosRepository,
            uploadedPhotosRepository,
            galleryPhotoRepository,
            settingsRepository,
            receivedPhotosRepository,
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

    @CallSuper
    open fun tearDown() {
        FileUtilsImpl().deleteAllFiles(File(tempFilesDir))
        database.close()
    }
}