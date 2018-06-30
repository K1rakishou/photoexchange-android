package com.kirakishou.photoexchange.service

import com.kirakishou.photoexchange.helper.Either
import com.kirakishou.photoexchange.helper.concurrency.rx.scheduler.SchedulerProvider
import com.kirakishou.photoexchange.helper.concurrency.rx.scheduler.TestSchedulers
import com.kirakishou.photoexchange.helper.database.repository.SettingsRepository
import com.kirakishou.photoexchange.helper.database.repository.TakenPhotosRepository
import com.kirakishou.photoexchange.helper.intercom.event.UploadedPhotosFragmentEvent
import com.kirakishou.photoexchange.interactors.GetUserIdUseCase
import com.kirakishou.photoexchange.interactors.UploadPhotosUseCase
import com.kirakishou.photoexchange.mvp.model.PhotoState
import com.kirakishou.photoexchange.mvp.model.TakenPhoto
import com.kirakishou.photoexchange.mvp.model.other.LonLat
import io.reactivex.Observable
import io.reactivex.Single
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito
import java.io.File
import java.io.IOException

class UploadPhotoServicePresenterTests {

    lateinit var takenPhotosRepository: TakenPhotosRepository
    lateinit var settingsRepository: SettingsRepository
    lateinit var schedulerProvider: SchedulerProvider
    lateinit var uploadPhotosUseCase: UploadPhotosUseCase
    lateinit var getUserIdUseCase: GetUserIdUseCase

    lateinit var presenter: UploadPhotoServicePresenter

    @Before
    fun setUp() {
        takenPhotosRepository = Mockito.mock(TakenPhotosRepository::class.java)
        settingsRepository = Mockito.mock(SettingsRepository::class.java)
        uploadPhotosUseCase = Mockito.mock(UploadPhotosUseCase::class.java)
        getUserIdUseCase = Mockito.mock(GetUserIdUseCase::class.java)

        schedulerProvider = TestSchedulers()

        presenter = Mockito.spy(
            UploadPhotoServicePresenter(
                takenPhotosRepository,
                settingsRepository,
                schedulerProvider,
                uploadPhotosUseCase,
                getUserIdUseCase)
        )
    }

    @After
    fun tearDown() {

    }

    @Test
    fun `should mark all photos with FAILED_TO_UPLOAD state, set error notification and stop the service when getUserId threw an error`() {
        Mockito.doAnswer { throw IOException("DB Error") }
            .`when`(getUserIdUseCase).getUserId()

        val values = presenter.resultEventsSubject
            .test()
            .assertNotComplete()
            .values()

        presenter.uploadPhotos(LonLat.empty())

        assertEquals(4, values.size)

        kotlin.run {
            assertEquals(true, values[0] is UploadPhotoServicePresenter.UploadPhotoEvent.OnNewNotification)

            val notificationType = (values[0] as UploadPhotoServicePresenter.UploadPhotoEvent.OnNewNotification).type
            assertEquals(true, notificationType is UploadPhotoServicePresenter.NotificationType.Uploading)
        }

        kotlin.run {
            assertEquals(true, values[1] is UploadPhotoServicePresenter.UploadPhotoEvent.OnNewNotification)

            val notificationType = (values[1] as UploadPhotoServicePresenter.UploadPhotoEvent.OnNewNotification).type
            assertEquals(true, notificationType is UploadPhotoServicePresenter.NotificationType.Error)
        }

        kotlin.run {
            assertEquals(true, values[2] is UploadPhotoServicePresenter.UploadPhotoEvent.UploadingEvent)

            val nestedEvent = (values[2] as UploadPhotoServicePresenter.UploadPhotoEvent.UploadingEvent).nestedEvent
            assertEquals(true, (nestedEvent is UploadedPhotosFragmentEvent.PhotoUploadEvent.OnUnknownError))

            val exception = (nestedEvent as UploadedPhotosFragmentEvent.PhotoUploadEvent.OnUnknownError).error
            assertEquals(true, exception is IOException)
        }

        assertEquals(true, values[3] is UploadPhotoServicePresenter.UploadPhotoEvent.StopService)

        Mockito.verify(getUserIdUseCase, Mockito.times(1)).getUserId()
        Mockito.verify(takenPhotosRepository, Mockito.times(1)).updateStates(PhotoState.PHOTO_QUEUED_UP, PhotoState.FAILED_TO_UPLOAD)
        Mockito.verify(takenPhotosRepository, Mockito.times(1)).updateStates(PhotoState.PHOTO_UPLOADING, PhotoState.FAILED_TO_UPLOAD)

        Mockito.verifyNoMoreInteractions(takenPhotosRepository)
        Mockito.verifyNoMoreInteractions(settingsRepository)
        Mockito.verifyNoMoreInteractions(uploadPhotosUseCase)
        Mockito.verifyNoMoreInteractions(getUserIdUseCase)
    }

    @Test
    fun `should do nothing when there are no queued up photos`() {
        Mockito.`when`(getUserIdUseCase.getUserId())
            .thenReturn(Single.just(Either.Value("123")))
        Mockito.`when`(takenPhotosRepository.findAllByState(PhotoState.PHOTO_QUEUED_UP))
            .thenReturn(listOf())

        val values = presenter.resultEventsSubject.test().values()
        presenter.uploadPhotos(LonLat.empty())

        assertEquals(3, values.size)

        kotlin.run {
            assertEquals(true, values[0] is UploadPhotoServicePresenter.UploadPhotoEvent.OnNewNotification)

            val notificationType = (values[0] as UploadPhotoServicePresenter.UploadPhotoEvent.OnNewNotification).type
            assertEquals(true, notificationType is UploadPhotoServicePresenter.NotificationType.Uploading)
        }

        kotlin.run {
            assertEquals(true, values[1] is UploadPhotoServicePresenter.UploadPhotoEvent.OnNewNotification)

            val notificationType = (values[1] as UploadPhotoServicePresenter.UploadPhotoEvent.OnNewNotification).type
            assertEquals(true, notificationType is UploadPhotoServicePresenter.NotificationType.Success)
        }

        assertEquals(true, values[2] is UploadPhotoServicePresenter.UploadPhotoEvent.StopService)

        Mockito.verify(getUserIdUseCase, Mockito.times(1)).getUserId()
        Mockito.verify(takenPhotosRepository, Mockito.times(1)).findAllByState(PhotoState.PHOTO_QUEUED_UP)

        Mockito.verifyNoMoreInteractions(takenPhotosRepository)
        Mockito.verifyNoMoreInteractions(settingsRepository)
        Mockito.verifyNoMoreInteractions(uploadPhotosUseCase)
        Mockito.verifyNoMoreInteractions(getUserIdUseCase)
    }

    @Test
    fun `test three photos uploading`() {
        val mockedFile = Mockito.mock(File::class.java)
        val mockedFile2 = Mockito.mock(File::class.java)
        val mockedFile3 = Mockito.mock(File::class.java)
        val userId = "1234"
        val location = LonLat(11.1, 33.5)
        val queuedUpPhoto1 = TakenPhoto(1L, PhotoState.PHOTO_QUEUED_UP, true, "111222", mockedFile)
        val queuedUpPhoto2 = TakenPhoto(2L, PhotoState.PHOTO_QUEUED_UP, true, "555666", mockedFile2)
        val queuedUpPhoto3 = TakenPhoto(3L, PhotoState.PHOTO_QUEUED_UP, true, "765756", mockedFile3)

        Mockito.`when`(getUserIdUseCase.getUserId())
            .thenReturn(Single.just(Either.Value(userId)))
        Mockito.`when`(takenPhotosRepository.findAllByState(PhotoState.PHOTO_QUEUED_UP))
            .thenReturn(listOf(queuedUpPhoto1, queuedUpPhoto2, queuedUpPhoto3))

        Mockito.doAnswer {
            return@doAnswer Observable.create<UploadedPhotosFragmentEvent.PhotoUploadEvent> { emitter ->
                emitter.onNext(UploadedPhotosFragmentEvent.PhotoUploadEvent.OnProgress(queuedUpPhoto1, 6))
                emitter.onNext(UploadedPhotosFragmentEvent.PhotoUploadEvent.OnProgress(queuedUpPhoto1, 55))
                emitter.onNext(UploadedPhotosFragmentEvent.PhotoUploadEvent.OnProgress(queuedUpPhoto1, 100))
                emitter.onComplete()
            }
        }.`when`(uploadPhotosUseCase).uploadPhoto(queuedUpPhoto1, userId, location)
        Mockito.doAnswer {
            return@doAnswer Observable.create<UploadedPhotosFragmentEvent.PhotoUploadEvent> { emitter ->
                emitter.onNext(UploadedPhotosFragmentEvent.PhotoUploadEvent.OnProgress(queuedUpPhoto2, 14))
                emitter.onNext(UploadedPhotosFragmentEvent.PhotoUploadEvent.OnProgress(queuedUpPhoto2, 44))
                emitter.onNext(UploadedPhotosFragmentEvent.PhotoUploadEvent.OnProgress(queuedUpPhoto2, 100))
                emitter.onComplete()
            }
        }.`when`(uploadPhotosUseCase).uploadPhoto(queuedUpPhoto2, userId, location)
        Mockito.doAnswer {
            return@doAnswer Observable.create<UploadedPhotosFragmentEvent.PhotoUploadEvent> { emitter ->
                emitter.onNext(UploadedPhotosFragmentEvent.PhotoUploadEvent.OnProgress(queuedUpPhoto3, 11))
                emitter.onNext(UploadedPhotosFragmentEvent.PhotoUploadEvent.OnProgress(queuedUpPhoto3, 53))
                emitter.onNext(UploadedPhotosFragmentEvent.PhotoUploadEvent.OnProgress(queuedUpPhoto3, 100))
                emitter.onComplete()
            }
        }.`when`(uploadPhotosUseCase).uploadPhoto(queuedUpPhoto3, userId, location)

        val values = presenter.resultEventsSubject.test()
            .assertNoErrors()
            .assertNotComplete()
            .values()

        presenter.uploadPhotos(location)

        assertEquals(16, values.size)

        kotlin.run {
            assertEquals(true, values[0] is UploadPhotoServicePresenter.UploadPhotoEvent.OnNewNotification)

            val notificationType = (values[0] as UploadPhotoServicePresenter.UploadPhotoEvent.OnNewNotification).type
            assertEquals(true, notificationType is UploadPhotoServicePresenter.NotificationType.Uploading)
        }

        kotlin.run {
            assertEquals(true, values[1] is UploadPhotoServicePresenter.UploadPhotoEvent.UploadingEvent)
            assertEquals(true, values[2] is UploadPhotoServicePresenter.UploadPhotoEvent.UploadingEvent)
            assertEquals(true, values[3] is UploadPhotoServicePresenter.UploadPhotoEvent.UploadingEvent)
            assertEquals(true, values[4] is UploadPhotoServicePresenter.UploadPhotoEvent.UploadingEvent)

            val nestedEvent = (values[1] as UploadPhotoServicePresenter.UploadPhotoEvent.UploadingEvent).nestedEvent
            assertEquals(true, nestedEvent is UploadedPhotosFragmentEvent.PhotoUploadEvent.OnPhotoUploadStart)
            assertEquals(queuedUpPhoto1, (nestedEvent as UploadedPhotosFragmentEvent.PhotoUploadEvent.OnPhotoUploadStart).photo)

            val nestedEvent1 = (values[2] as UploadPhotoServicePresenter.UploadPhotoEvent.UploadingEvent).nestedEvent
            assertEquals(true, nestedEvent1 is UploadedPhotosFragmentEvent.PhotoUploadEvent.OnProgress)
            assertEquals(6, (nestedEvent1 as UploadedPhotosFragmentEvent.PhotoUploadEvent.OnProgress).progress)
            assertEquals(queuedUpPhoto1, nestedEvent1.photo)

            val nestedEvent3 = (values[3] as UploadPhotoServicePresenter.UploadPhotoEvent.UploadingEvent).nestedEvent
            assertEquals(true, nestedEvent3 is UploadedPhotosFragmentEvent.PhotoUploadEvent.OnProgress)
            assertEquals(55, (nestedEvent3 as UploadedPhotosFragmentEvent.PhotoUploadEvent.OnProgress).progress)
            assertEquals(queuedUpPhoto1, nestedEvent3.photo)

            val nestedEvent6 = (values[4] as UploadPhotoServicePresenter.UploadPhotoEvent.UploadingEvent).nestedEvent
            assertEquals(true, nestedEvent6 is UploadedPhotosFragmentEvent.PhotoUploadEvent.OnProgress)
            assertEquals(100, (nestedEvent6 as UploadedPhotosFragmentEvent.PhotoUploadEvent.OnProgress).progress)
            assertEquals(queuedUpPhoto1, nestedEvent6.photo)
        }

        kotlin.run {
            assertEquals(true, values[5] is UploadPhotoServicePresenter.UploadPhotoEvent.UploadingEvent)
            assertEquals(true, values[6] is UploadPhotoServicePresenter.UploadPhotoEvent.UploadingEvent)
            assertEquals(true, values[7] is UploadPhotoServicePresenter.UploadPhotoEvent.UploadingEvent)
            assertEquals(true, values[8] is UploadPhotoServicePresenter.UploadPhotoEvent.UploadingEvent)

            val nestedEvent = (values[5] as UploadPhotoServicePresenter.UploadPhotoEvent.UploadingEvent).nestedEvent
            assertEquals(true, nestedEvent is UploadedPhotosFragmentEvent.PhotoUploadEvent.OnPhotoUploadStart)
            assertEquals(queuedUpPhoto2, (nestedEvent as UploadedPhotosFragmentEvent.PhotoUploadEvent.OnPhotoUploadStart).photo)

            val nestedEvent1 = (values[6] as UploadPhotoServicePresenter.UploadPhotoEvent.UploadingEvent).nestedEvent
            assertEquals(true, nestedEvent1 is UploadedPhotosFragmentEvent.PhotoUploadEvent.OnProgress)
            assertEquals(14, (nestedEvent1 as UploadedPhotosFragmentEvent.PhotoUploadEvent.OnProgress).progress)
            assertEquals(queuedUpPhoto2, nestedEvent1.photo)

            val nestedEvent3 = (values[7] as UploadPhotoServicePresenter.UploadPhotoEvent.UploadingEvent).nestedEvent
            assertEquals(true, nestedEvent3 is UploadedPhotosFragmentEvent.PhotoUploadEvent.OnProgress)
            assertEquals(44, (nestedEvent3 as UploadedPhotosFragmentEvent.PhotoUploadEvent.OnProgress).progress)
            assertEquals(queuedUpPhoto2, nestedEvent3.photo)

            val nestedEvent6 = (values[8] as UploadPhotoServicePresenter.UploadPhotoEvent.UploadingEvent).nestedEvent
            assertEquals(true, nestedEvent6 is UploadedPhotosFragmentEvent.PhotoUploadEvent.OnProgress)
            assertEquals(100, (nestedEvent6 as UploadedPhotosFragmentEvent.PhotoUploadEvent.OnProgress).progress)
            assertEquals(queuedUpPhoto2, nestedEvent6.photo)
        }

        kotlin.run {
            assertEquals(true, values[9] is UploadPhotoServicePresenter.UploadPhotoEvent.UploadingEvent)
            assertEquals(true, values[10] is UploadPhotoServicePresenter.UploadPhotoEvent.UploadingEvent)
            assertEquals(true, values[11] is UploadPhotoServicePresenter.UploadPhotoEvent.UploadingEvent)
            assertEquals(true, values[12] is UploadPhotoServicePresenter.UploadPhotoEvent.UploadingEvent)

            val nestedEvent = (values[9] as UploadPhotoServicePresenter.UploadPhotoEvent.UploadingEvent).nestedEvent
            assertEquals(true, nestedEvent is UploadedPhotosFragmentEvent.PhotoUploadEvent.OnPhotoUploadStart)
            assertEquals(queuedUpPhoto3, (nestedEvent as UploadedPhotosFragmentEvent.PhotoUploadEvent.OnPhotoUploadStart).photo)

            val nestedEvent1 = (values[10] as UploadPhotoServicePresenter.UploadPhotoEvent.UploadingEvent).nestedEvent
            assertEquals(true, nestedEvent1 is UploadedPhotosFragmentEvent.PhotoUploadEvent.OnProgress)
            assertEquals(11, (nestedEvent1 as UploadedPhotosFragmentEvent.PhotoUploadEvent.OnProgress).progress)
            assertEquals(queuedUpPhoto3, nestedEvent1.photo)

            val nestedEvent3 = (values[11] as UploadPhotoServicePresenter.UploadPhotoEvent.UploadingEvent).nestedEvent
            assertEquals(true, nestedEvent3 is UploadedPhotosFragmentEvent.PhotoUploadEvent.OnProgress)
            assertEquals(53, (nestedEvent3 as UploadedPhotosFragmentEvent.PhotoUploadEvent.OnProgress).progress)
            assertEquals(queuedUpPhoto3, nestedEvent3.photo)

            val nestedEvent6 = (values[12] as UploadPhotoServicePresenter.UploadPhotoEvent.UploadingEvent).nestedEvent
            assertEquals(true, nestedEvent6 is UploadedPhotosFragmentEvent.PhotoUploadEvent.OnProgress)
            assertEquals(100, (nestedEvent6 as UploadedPhotosFragmentEvent.PhotoUploadEvent.OnProgress).progress)
            assertEquals(queuedUpPhoto3, nestedEvent6.photo)
        }

        kotlin.run {
            assertEquals(true, values[13] is UploadPhotoServicePresenter.UploadPhotoEvent.UploadingEvent)

            val nestedEvent = (values[13] as UploadPhotoServicePresenter.UploadPhotoEvent.UploadingEvent).nestedEvent
            assertEquals(true, nestedEvent is UploadedPhotosFragmentEvent.PhotoUploadEvent.OnEnd)
        }

        kotlin.run {
            assertEquals(true, values[14] is UploadPhotoServicePresenter.UploadPhotoEvent.OnNewNotification)

            val notificationType = (values[14] as UploadPhotoServicePresenter.UploadPhotoEvent.OnNewNotification).type
            assertEquals(true, notificationType is UploadPhotoServicePresenter.NotificationType.Success)
        }

        assertEquals(true, values[15] is UploadPhotoServicePresenter.UploadPhotoEvent.StopService)

        Mockito.verify(takenPhotosRepository, Mockito.times(1)).findAllByState(PhotoState.PHOTO_QUEUED_UP)
        Mockito.verify(uploadPhotosUseCase, Mockito.times(1)).uploadPhoto(queuedUpPhoto1, userId, location)
        Mockito.verify(uploadPhotosUseCase, Mockito.times(1)).uploadPhoto(queuedUpPhoto2, userId, location)
        Mockito.verify(uploadPhotosUseCase, Mockito.times(1)).uploadPhoto(queuedUpPhoto3, userId, location)
        Mockito.verify(getUserIdUseCase, Mockito.times(1)).getUserId()

        Mockito.verifyNoMoreInteractions(takenPhotosRepository)
        Mockito.verifyNoMoreInteractions(settingsRepository)
        Mockito.verifyNoMoreInteractions(uploadPhotosUseCase)
        Mockito.verifyNoMoreInteractions(getUserIdUseCase)
    }
}





























