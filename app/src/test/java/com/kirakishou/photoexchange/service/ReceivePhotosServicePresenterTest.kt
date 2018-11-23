package com.kirakishou.photoexchange.service

class ReceivePhotosServicePresenterTest {

  /*lateinit var uploadedPhotosRepository: UploadedPhotosRepository
  lateinit var settingsRepository: SettingsRepository
  lateinit var schedulerProvider: SchedulerProvider
  lateinit var receivePhotosUseCase: ReceivePhotosUseCase

  lateinit var presenter: ReceivePhotosServicePresenter

  @Before
  fun setUp() {
    uploadedPhotosRepository = Mockito.mock(UploadedPhotosRepository::class.java)
    settingsRepository = Mockito.mock(SettingsRepository::class.java)
    receivePhotosUseCase = Mockito.mock(ReceivePhotosUseCase::class.java)
    schedulerProvider = TestSchedulers()

    presenter = Mockito.spy(
      ReceivePhotosServicePresenter(
        uploadedPhotosRepository,
        settingsRepository,
        schedulerProvider,
        receivePhotosUseCase,
        0L)
    )
  }

  @After
  fun tearDown() {

  }

  @Test
  fun `should do nothing when there are no uploaded photos without receiver info`() {
    Mockito.`when`(uploadedPhotosRepository.findAllWithoutReceiverInfo())
      .thenReturn(emptyList())

    val values = presenter.resultEventsSubject
      .test()
      .assertNoErrors()
      .assertNotComplete()
      .values()

    presenter.startPhotosReceiving()

    assertEquals(4, values.size)

    assertEquals(true, values[0] is ReceivePhotosServicePresenter.ReceivePhotoEvent.OnNewNotification)
    assertEquals(true, (values[0] as ReceivePhotosServicePresenter.ReceivePhotoEvent.OnNewNotification).type
      is ReceivePhotosServicePresenter.NotificationType.Progress)

    assertEquals(true, values[1] is ReceivePhotosServicePresenter.ReceivePhotoEvent.OnKnownError)
    assertEquals(true, (values[1] as ReceivePhotosServicePresenter.ReceivePhotoEvent.OnKnownError).errorCode == null)

    assertEquals(true, values[2] is ReceivePhotosServicePresenter.ReceivePhotoEvent.RemoveNotification)
    assertEquals(true, values[3] is ReceivePhotosServicePresenter.ReceivePhotoEvent.StopService)

    Mockito.verify(uploadedPhotosRepository, Mockito.times(1)).findAllWithoutReceiverInfo()

    Mockito.verifyNoMoreInteractions(uploadedPhotosRepository)
    Mockito.verifyNoMoreInteractions(settingsRepository)
    Mockito.verifyNoMoreInteractions(receivePhotosUseCase)
  }

  @Test
  fun `should do nothing when userId is empty`() {
    val uploadedPhotos = listOf(UploadedPhoto(1L, "123", 55.4, 44.3, false, 112233L))

    Mockito.`when`(uploadedPhotosRepository.findAllWithoutReceiverInfo())
      .thenReturn(uploadedPhotos)
    Mockito.`when`(settingsRepository.getUserId())
      .thenReturn("")

    val values = presenter.resultEventsSubject
      .test()
      .assertNoErrors()
      .assertNotComplete()
      .values()

    presenter.startPhotosReceiving()

    assertEquals(4, values.size)

    assertEquals(true, values[0] is ReceivePhotosServicePresenter.ReceivePhotoEvent.OnNewNotification)
    assertEquals(true, (values[0] as ReceivePhotosServicePresenter.ReceivePhotoEvent.OnNewNotification).type
      is ReceivePhotosServicePresenter.NotificationType.Progress)

    assertEquals(true, values[1] is ReceivePhotosServicePresenter.ReceivePhotoEvent.OnKnownError)
    assertEquals(true, (values[1] as ReceivePhotosServicePresenter.ReceivePhotoEvent.OnKnownError).errorCode
      is ErrorCode.ReceivePhotosErrors.LocalCouldNotGetUserId)

    assertEquals(true, values[2] is ReceivePhotosServicePresenter.ReceivePhotoEvent.OnNewNotification)
    assertEquals(true, (values[2] as ReceivePhotosServicePresenter.ReceivePhotoEvent.OnNewNotification).type
      is ReceivePhotosServicePresenter.NotificationType.Error)

    assertEquals(true, values[3] is ReceivePhotosServicePresenter.ReceivePhotoEvent.StopService)

    Mockito.verify(uploadedPhotosRepository, Mockito.times(1)).findAllWithoutReceiverInfo()
    Mockito.verify(settingsRepository, Mockito.times(1)).getUserId()

    Mockito.verifyNoMoreInteractions(uploadedPhotosRepository)
    Mockito.verifyNoMoreInteractions(settingsRepository)
    Mockito.verifyNoMoreInteractions(receivePhotosUseCase)
  }

  @Test
  fun `should send NotificationTypeSuccess when receivePhotosUseCase returns OnReceivedPhoto`() {
    val userId = "346346"
    val uploadedPhotos = listOf(
      UploadedPhoto(1L, "123", 55.4, 44.3, false, 112233L),
      UploadedPhoto(2L, "234", 44.2, 55.5, false, 554433L)
    )
    val receivedPhoto1 = ReceivedPhoto(1L, "123", "876", 22.2, 33.3)
    val receivedPhoto2 = ReceivedPhoto(2L, "234", "765", 33.3, 55.2)

    Mockito.`when`(uploadedPhotosRepository.findAllWithoutReceiverInfo())
      .thenReturn(uploadedPhotos)
    Mockito.`when`(settingsRepository.getUserId())
      .thenReturn(userId)
    Mockito.doAnswer {
      return@doAnswer Observable.create<ReceivePhotosServicePresenter.ReceivePhotoEvent> { emitter ->
        emitter.onNext(ReceivePhotosServicePresenter.ReceivePhotoEvent.OnReceivedPhoto(receivedPhoto1, "123"))
        emitter.onNext(ReceivePhotosServicePresenter.ReceivePhotoEvent.OnReceivedPhoto(receivedPhoto2, "234"))
        emitter.onComplete()
      }
    }.`when`(receivePhotosUseCase).receivePhotos(any())

    val values = presenter.resultEventsSubject
      .test()
      .assertNoErrors()
      .assertNotComplete()
      .values()

    presenter.startPhotosReceiving()

    assertEquals(5, values.size)

    assertEquals(true, values[0] is ReceivePhotosServicePresenter.ReceivePhotoEvent.OnNewNotification)
    assertEquals(true, (values[0] as ReceivePhotosServicePresenter.ReceivePhotoEvent.OnNewNotification).type
      is ReceivePhotosServicePresenter.NotificationType.Progress)

    assertEquals(true, values[1] is ReceivePhotosServicePresenter.ReceivePhotoEvent.OnReceivedPhoto)
    (values[1] as ReceivePhotosServicePresenter.ReceivePhotoEvent.OnReceivedPhoto).also { photoEvent ->
      assertEquals("123", photoEvent.takenPhotoName)
      assertEquals(1L, photoEvent.receivedPhoto.photoId)
      assertEquals("123", photoEvent.receivedPhoto.uploadedPhotoName)
      assertEquals("876", photoEvent.receivedPhoto.receivedPhotoName)
    }

    assertEquals(true, values[2] is ReceivePhotosServicePresenter.ReceivePhotoEvent.OnReceivedPhoto)
    (values[2] as ReceivePhotosServicePresenter.ReceivePhotoEvent.OnReceivedPhoto).also { photoEvent ->
      assertEquals("234", photoEvent.takenPhotoName)
      assertEquals(2L, photoEvent.receivedPhoto.photoId)
      assertEquals("234", photoEvent.receivedPhoto.uploadedPhotoName)
      assertEquals("765", photoEvent.receivedPhoto.receivedPhotoName)
    }

    assertEquals(true, values[3] is ReceivePhotosServicePresenter.ReceivePhotoEvent.OnNewNotification)
    assertEquals(true, (values[3] as ReceivePhotosServicePresenter.ReceivePhotoEvent.OnNewNotification).type
      is ReceivePhotosServicePresenter.NotificationType.Success)

    assertEquals(true, values[4] is ReceivePhotosServicePresenter.ReceivePhotoEvent.StopService)

    Mockito.verify(uploadedPhotosRepository, Mockito.times(1)).findAllWithoutReceiverInfo()
    Mockito.verify(settingsRepository, Mockito.times(1)).getUserId()
    Mockito.verify(receivePhotosUseCase, Mockito.times(1)).receivePhotos(any())

    Mockito.verifyNoMoreInteractions(uploadedPhotosRepository)
    Mockito.verifyNoMoreInteractions(settingsRepository)
    Mockito.verifyNoMoreInteractions(receivePhotosUseCase)
  }

  @Test
  fun `should send NotificationTypeError when receivePhotosUseCase returns OnKnownError`() {
    val userId = "346346"
    val uploadedPhotos = listOf(
      UploadedPhoto(1L, "123", 55.4, 44.3, false, 112233L),
      UploadedPhoto(2L, "234", 44.2, 55.5, false, 554433L)
    )

    Mockito.`when`(uploadedPhotosRepository.findAllWithoutReceiverInfo())
      .thenReturn(uploadedPhotos)
    Mockito.`when`(settingsRepository.getUserId())
      .thenReturn(userId)
    Mockito.doAnswer {
      return@doAnswer Observable.create<ReceivePhotosServicePresenter.ReceivePhotoEvent> { emitter ->
        emitter.onNext(ReceivePhotosServicePresenter.ReceivePhotoEvent.OnKnownError(ErrorCode.ReceivePhotosErrors.BadRequest()))
        emitter.onComplete()
      }
    }.`when`(receivePhotosUseCase).receivePhotos(any())

    val values = presenter.resultEventsSubject
      .test()
      .assertNoErrors()
      .assertNotComplete()
      .values()

    presenter.startPhotosReceiving()

    assertEquals(4, values.size)

    assertEquals(true, values[0] is ReceivePhotosServicePresenter.ReceivePhotoEvent.OnNewNotification)
    assertEquals(true, (values[0] as ReceivePhotosServicePresenter.ReceivePhotoEvent.OnNewNotification).type
      is ReceivePhotosServicePresenter.NotificationType.Progress)

    assertEquals(true, values[1] is ReceivePhotosServicePresenter.ReceivePhotoEvent.OnKnownError)
    assertEquals(true, (values[1] as ReceivePhotosServicePresenter.ReceivePhotoEvent.OnKnownError).errorCode
      is ErrorCode.ReceivePhotosErrors.BadRequest)

    assertEquals(true, values[2] is ReceivePhotosServicePresenter.ReceivePhotoEvent.OnNewNotification)
    assertEquals(true, (values[2] as ReceivePhotosServicePresenter.ReceivePhotoEvent.OnNewNotification).type
      is ReceivePhotosServicePresenter.NotificationType.Error)

    assertEquals(true, values[3] is ReceivePhotosServicePresenter.ReceivePhotoEvent.StopService)

    Mockito.verify(uploadedPhotosRepository, Mockito.times(1)).findAllWithoutReceiverInfo()
    Mockito.verify(settingsRepository, Mockito.times(1)).getUserId()
    Mockito.verify(receivePhotosUseCase, Mockito.times(1)).receivePhotos(any())

    Mockito.verifyNoMoreInteractions(uploadedPhotosRepository)
    Mockito.verifyNoMoreInteractions(settingsRepository)
    Mockito.verifyNoMoreInteractions(receivePhotosUseCase)
  }

  @Test
  fun `should send NotificationTypeError when receivePhotosUseCase returns OnUnknownError`() {
    val userId = "346346"
    val uploadedPhotos = listOf(
      UploadedPhoto(1L, "123", 55.4, 44.3, false, 112233L),
      UploadedPhoto(2L, "234", 44.2, 55.5, false, 554433L)
    )

    Mockito.`when`(uploadedPhotosRepository.findAllWithoutReceiverInfo())
      .thenReturn(uploadedPhotos)
    Mockito.`when`(settingsRepository.getUserId())
      .thenReturn(userId)
    Mockito.doAnswer {
      return@doAnswer Observable.create<ReceivePhotosServicePresenter.ReceivePhotoEvent> { emitter ->
        emitter.onNext(ReceivePhotosServicePresenter.ReceivePhotoEvent.OnUnknownError(IOException()))
        emitter.onComplete()
      }
    }.`when`(receivePhotosUseCase).receivePhotos(any())

    val values = presenter.resultEventsSubject
      .test()
      .assertNoErrors()
      .assertNotComplete()
      .values()

    presenter.startPhotosReceiving()

    assertEquals(true, values[0] is ReceivePhotosServicePresenter.ReceivePhotoEvent.OnNewNotification)
    assertEquals(true, (values[0] as ReceivePhotosServicePresenter.ReceivePhotoEvent.OnNewNotification).type
      is ReceivePhotosServicePresenter.NotificationType.Progress)

    assertEquals(true, values[1] is ReceivePhotosServicePresenter.ReceivePhotoEvent.OnUnknownError)
    assertEquals(true, (values[1] as ReceivePhotosServicePresenter.ReceivePhotoEvent.OnUnknownError).error
      is IOException)

    assertEquals(true, values[2] is ReceivePhotosServicePresenter.ReceivePhotoEvent.OnNewNotification)
    assertEquals(true, (values[2] as ReceivePhotosServicePresenter.ReceivePhotoEvent.OnNewNotification).type
      is ReceivePhotosServicePresenter.NotificationType.Error)

    assertEquals(true, values[3] is ReceivePhotosServicePresenter.ReceivePhotoEvent.StopService)

    Mockito.verify(uploadedPhotosRepository, Mockito.times(1)).findAllWithoutReceiverInfo()
    Mockito.verify(settingsRepository, Mockito.times(1)).getUserId()
    Mockito.verify(receivePhotosUseCase, Mockito.times(1)).receivePhotos(any())

    Mockito.verifyNoMoreInteractions(uploadedPhotosRepository)
    Mockito.verifyNoMoreInteractions(settingsRepository)
    Mockito.verifyNoMoreInteractions(receivePhotosUseCase)
  }*/
}































