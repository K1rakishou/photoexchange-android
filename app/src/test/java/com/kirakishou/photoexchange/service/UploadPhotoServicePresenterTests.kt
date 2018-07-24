package com.kirakishou.photoexchange.service

import com.kirakishou.photoexchange.helper.Either
import com.kirakishou.photoexchange.helper.concurrency.rx.scheduler.SchedulerProvider
import com.kirakishou.photoexchange.helper.concurrency.rx.scheduler.TestSchedulers
import com.kirakishou.photoexchange.helper.database.repository.TakenPhotosRepository
import com.kirakishou.photoexchange.helper.intercom.event.UploadedPhotosFragmentEvent
import com.kirakishou.photoexchange.interactors.GetUserIdUseCase
import com.kirakishou.photoexchange.interactors.UploadPhotosUseCase
import com.kirakishou.photoexchange.mvp.model.PhotoState
import com.kirakishou.photoexchange.mvp.model.TakenPhoto
import com.kirakishou.photoexchange.mvp.model.other.ErrorCode
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
    lateinit var schedulerProvider: SchedulerProvider
    lateinit var uploadPhotosUseCase: UploadPhotosUseCase
    lateinit var getUserIdUseCase: GetUserIdUseCase

    lateinit var presenter: UploadPhotoServicePresenter

    @Before
    fun setUp() {
        takenPhotosRepository = Mockito.mock(TakenPhotosRepository::class.java)
        uploadPhotosUseCase = Mockito.mock(UploadPhotosUseCase::class.java)
        getUserIdUseCase = Mockito.mock(GetUserIdUseCase::class.java)
        schedulerProvider = TestSchedulers()

        presenter = Mockito.spy(
            UploadPhotoServicePresenter(
                takenPhotosRepository,
                schedulerProvider,
                uploadPhotosUseCase,
                getUserIdUseCase,
                0L)
        )
    }

    @After
    fun tearDown() {

    }

    private inline fun <reified T> checkOnNewNotificationEvent(event: UploadPhotoServicePresenter.UploadPhotoEvent) {
        assertEquals(true, event is UploadPhotoServicePresenter.UploadPhotoEvent.OnNewNotification)

        val notificationType = (event as UploadPhotoServicePresenter.UploadPhotoEvent.OnNewNotification).type
        assertEquals(true, notificationType is T)
    }

    private fun checkStopServiceEvent(event: UploadPhotoServicePresenter.UploadPhotoEvent) {
        assertEquals(true, event is UploadPhotoServicePresenter.UploadPhotoEvent.StopService)
    }

    private fun checkFailedToUploadEvent(photo: TakenPhoto, errorCode: ErrorCode.UploadPhotoErrors, event: UploadPhotoServicePresenter.UploadPhotoEvent) {
        assertEquals(true, event is UploadPhotoServicePresenter.UploadPhotoEvent.UploadingEvent)

        val nestedEvent = (event as UploadPhotoServicePresenter.UploadPhotoEvent.UploadingEvent).nestedEvent
        assertEquals(true, nestedEvent is UploadedPhotosFragmentEvent.PhotoUploadEvent.OnFailedToUpload)
        assertEquals(photo, (nestedEvent as UploadedPhotosFragmentEvent.PhotoUploadEvent.OnFailedToUpload).photo)
        assertEquals(errorCode.getValue(), nestedEvent.errorCode.getValue())
    }

    private fun checkOnEndEvent(event: UploadPhotoServicePresenter.UploadPhotoEvent) {
        assertEquals(true, event is UploadPhotoServicePresenter.UploadPhotoEvent.UploadingEvent)

        val nestedEvent = (event as UploadPhotoServicePresenter.UploadPhotoEvent.UploadingEvent).nestedEvent
        assertEquals(true, nestedEvent is UploadedPhotosFragmentEvent.PhotoUploadEvent.OnEnd)
    }

    private fun checkOnPhotoUploadStartEvent(photo: TakenPhoto, event: UploadPhotoServicePresenter.UploadPhotoEvent) {
        assertEquals(true, event is UploadPhotoServicePresenter.UploadPhotoEvent.UploadingEvent)

        val nestedEvent = (event as UploadPhotoServicePresenter.UploadPhotoEvent.UploadingEvent).nestedEvent
        assertEquals(true, nestedEvent is UploadedPhotosFragmentEvent.PhotoUploadEvent.OnPhotoUploadStart)
        assertEquals(photo, (nestedEvent as UploadedPhotosFragmentEvent.PhotoUploadEvent.OnPhotoUploadStart).photo)
    }

    private fun checkOnProgressEvent(progress: Int, photo: TakenPhoto, event: UploadPhotoServicePresenter.UploadPhotoEvent) {
        assertEquals(true, event is UploadPhotoServicePresenter.UploadPhotoEvent.UploadingEvent)

        val nestedEvent = (event as UploadPhotoServicePresenter.UploadPhotoEvent.UploadingEvent).nestedEvent
        assertEquals(true, nestedEvent is UploadedPhotosFragmentEvent.PhotoUploadEvent.OnProgress)
        assertEquals(progress, (nestedEvent as UploadedPhotosFragmentEvent.PhotoUploadEvent.OnProgress).progress)
        assertEquals(photo, nestedEvent.photo)
    }

    private fun checkOnUploadedEvent(photo: TakenPhoto, event: UploadPhotoServicePresenter.UploadPhotoEvent) {
        assertEquals(true, event is UploadPhotoServicePresenter.UploadPhotoEvent.UploadingEvent)

        val nestedEvent = (event as UploadPhotoServicePresenter.UploadPhotoEvent.UploadingEvent).nestedEvent
        assertEquals(true, nestedEvent is UploadedPhotosFragmentEvent.PhotoUploadEvent.OnUploaded)
        assertEquals(photo, (nestedEvent as UploadedPhotosFragmentEvent.PhotoUploadEvent.OnUploaded).takenPhoto)
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

        assertEquals(19, values.size)

        checkOnNewNotificationEvent<UploadPhotoServicePresenter.NotificationType.Uploading>(values[0])

        checkOnPhotoUploadStartEvent(queuedUpPhoto1, values[1])
        checkOnProgressEvent(6, queuedUpPhoto1, values[2])
        checkOnProgressEvent(55, queuedUpPhoto1, values[3])
        checkOnProgressEvent(100, queuedUpPhoto1, values[4])
        checkOnUploadedEvent(queuedUpPhoto1, values[5])

        checkOnPhotoUploadStartEvent(queuedUpPhoto2, values[6])
        checkOnProgressEvent(14, queuedUpPhoto2, values[7])
        checkOnProgressEvent(44, queuedUpPhoto2, values[8])
        checkOnProgressEvent(100, queuedUpPhoto2, values[9])
        checkOnUploadedEvent(queuedUpPhoto2, values[10])

        checkOnPhotoUploadStartEvent(queuedUpPhoto3, values[11])
        checkOnProgressEvent(11, queuedUpPhoto3, values[12])
        checkOnProgressEvent(53, queuedUpPhoto3, values[13])
        checkOnProgressEvent(100, queuedUpPhoto3, values[14])
        checkOnUploadedEvent(queuedUpPhoto3, values[15])

        checkOnEndEvent(values[16])
        checkOnNewNotificationEvent<UploadPhotoServicePresenter.NotificationType.Success>(values[17])
        checkStopServiceEvent(values[18])

        Mockito.verify(takenPhotosRepository, Mockito.times(1)).findAllByState(PhotoState.PHOTO_QUEUED_UP)
        Mockito.verify(uploadPhotosUseCase, Mockito.times(1)).uploadPhoto(queuedUpPhoto1, userId, location)
        Mockito.verify(uploadPhotosUseCase, Mockito.times(1)).uploadPhoto(queuedUpPhoto2, userId, location)
        Mockito.verify(uploadPhotosUseCase, Mockito.times(1)).uploadPhoto(queuedUpPhoto3, userId, location)
        Mockito.verify(getUserIdUseCase, Mockito.times(1)).getUserId()

        Mockito.verifyNoMoreInteractions(takenPhotosRepository)
        Mockito.verifyNoMoreInteractions(uploadPhotosUseCase)
        Mockito.verifyNoMoreInteractions(getUserIdUseCase)
    }

    @Test
    fun `test three photos uploading with the second one throwing an exception`() {
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
                emitter.onError(IOException("Error while trying to write a file to socket"))
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

        assertEquals(17, values.size)

        checkOnNewNotificationEvent<UploadPhotoServicePresenter.NotificationType.Uploading>(values[0])

        checkOnPhotoUploadStartEvent(queuedUpPhoto1, values[1])
        checkOnProgressEvent(6, queuedUpPhoto1, values[2])
        checkOnProgressEvent(55, queuedUpPhoto1, values[3])
        checkOnProgressEvent(100, queuedUpPhoto1, values[4])
        checkOnUploadedEvent(queuedUpPhoto1, values[5])

        checkOnPhotoUploadStartEvent(queuedUpPhoto2, values[6])
        checkOnProgressEvent(14, queuedUpPhoto2, values[7])
        checkFailedToUploadEvent(queuedUpPhoto2, ErrorCode.UploadPhotoErrors.UnknownError(), values[8])

        checkOnPhotoUploadStartEvent(queuedUpPhoto3, values[9])
        checkOnProgressEvent(11, queuedUpPhoto3, values[10])
        checkOnProgressEvent(53, queuedUpPhoto3, values[11])
        checkOnProgressEvent(100, queuedUpPhoto3, values[12])
        checkOnUploadedEvent(queuedUpPhoto3, values[13])

        checkOnEndEvent(values[14])
        checkOnNewNotificationEvent<UploadPhotoServicePresenter.NotificationType.Success>(values[15])
        checkStopServiceEvent(values[16])

        Mockito.verify(takenPhotosRepository, Mockito.times(1)).findAllByState(PhotoState.PHOTO_QUEUED_UP)
        Mockito.verify(takenPhotosRepository, Mockito.times(1)).updatePhotoState(queuedUpPhoto2.id, PhotoState.FAILED_TO_UPLOAD)
        Mockito.verify(uploadPhotosUseCase, Mockito.times(1)).uploadPhoto(queuedUpPhoto1, userId, location)
        Mockito.verify(uploadPhotosUseCase, Mockito.times(1)).uploadPhoto(queuedUpPhoto2, userId, location)
        Mockito.verify(uploadPhotosUseCase, Mockito.times(1)).uploadPhoto(queuedUpPhoto3, userId, location)
        Mockito.verify(getUserIdUseCase, Mockito.times(1)).getUserId()

        Mockito.verifyNoMoreInteractions(takenPhotosRepository)
        Mockito.verifyNoMoreInteractions(uploadPhotosUseCase)
        Mockito.verifyNoMoreInteractions(getUserIdUseCase)
    }
}





























