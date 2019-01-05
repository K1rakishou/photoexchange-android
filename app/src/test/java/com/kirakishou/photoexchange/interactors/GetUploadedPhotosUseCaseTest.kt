package com.kirakishou.photoexchange.interactors

class GetUploadedPhotosUseCaseTest {

  /*lateinit var database: MyDatabase
  lateinit var apiClient: ApiClient
  lateinit var timeUtils: TimeUtils
  lateinit var dispatchersProvider: DispatchersProvider

  lateinit var uploadedPhotosRepository: UploadedPhotosRepository
  lateinit var getUploadedPhotosUseCase: GetUploadedPhotosUseCase

  @Before
  fun setUp() {
    apiClient = Mockito.mock(ApiClient::class.java)
    uploadedPhotosRepository = Mockito.mock(UploadedPhotosRepository::class.java)
    timeUtils = Mockito.mock(TimeUtils::class.java)
    dispatchersProvider = MockDispatchers()

    getUploadedPhotosUseCase = GetUploadedPhotosUseCase(
      uploadedPhotosRepository,
      apiClient,
      timeUtils,
      dispatchersProvider
    )
  }

  @After
  fun tearDown() {

  }

  @Test
  fun `should return EitherError with errorCode when server did not return ok`() {
    val userId = "123"
    val lastId = 1000L
    val photosPerPage = 10

    Mockito.`when`(apiClient.getUploadedPhotoIds(userId, lastId, photosPerPage))
      .thenReturn(Single.just(GetUploadedPhotoIdsResponse.fail(ErrorCode.GetUploadedPhotosErrors.BadRequest())))

    getUploadedPhotosUseCase.loadPageOfPhotos(userId, lastId, photosPerPage)
      .test()
      .assertNoErrors()
      .assertValueAt(0) { value ->
        value is Either.Error
      }
      .assertValueAt(0) { value ->
        (value as Either.Error).error is ErrorCode.GetUploadedPhotosErrors.BadRequest
      }
      .assertTerminated()
      .awaitTerminalEvent()

    Mockito.verify(apiClient, Mockito.times(1)).getUploadedPhotoIds(userId, lastId, photosPerPage)

    Mockito.verifyNoMoreInteractions(uploadedPhotosRepository)
    Mockito.verifyNoMoreInteractions(apiClient)
  }

  @Test
  fun `should return EitherValue with empty list when server has no photos`() {
    val userId = "123"
    val lastId = 1000L
    val photosPerPage = 10

    Mockito.`when`(apiClient.getUploadedPhotoIds(userId, lastId, photosPerPage))
      .thenReturn(Single.just(GetUploadedPhotoIdsResponse.success(emptyList())))

    getUploadedPhotosUseCase.loadPageOfPhotos(userId, lastId, photosPerPage)
      .test()
      .assertNoErrors()
      .assertValueAt(0) { value ->
        value is Either.Value
      }
      .assertValueAt(0) { value ->
        (value as Either.Value).value.isEmpty()
      }

    Mockito.verify(apiClient, Mockito.times(1)).getUploadedPhotoIds(userId, lastId, photosPerPage)

    Mockito.verifyNoMoreInteractions(uploadedPhotosRepository)
    Mockito.verifyNoMoreInteractions(apiClient)
  }

  @Test
  fun `should not make a network request when all photos have been found in the database`() {
    val userId = "123"
    val lastId = 1000L
    val photosPerPage = 10
    val photoIds = listOf<Long>(1, 2, 3, 4, 5)
    val uploadedPhotos = listOf(
      UploadedPhoto(4L, "456", 44.4, 44.4, false, 444L),
      UploadedPhoto(1L, "123", 11.1, 11.1, false, 111L),
      UploadedPhoto(3L, "345", 33.3, 33.3, false, 333L),
      UploadedPhoto(2L, "234", 22.2, 22.2, false, 222L),
      UploadedPhoto(5L, "567", 55.5, 55.5, false, 555L)
    )

    Mockito.`when`(apiClient.getUploadedPhotoIds(userId, lastId, photosPerPage))
      .thenReturn(Single.just(GetUploadedPhotoIdsResponse.success(photoIds)))
    Mockito.`when`(uploadedPhotosRepository.findMany(photoIds))
      .thenReturn(uploadedPhotos)
    Mockito.doNothing().`when`(uploadedPhotosRepository).deleteOldPhotoInfos()

    val events = getUploadedPhotosUseCase.loadPageOfPhotos(userId, lastId, photosPerPage)
      .test()
      .assertNoErrors()
      .assertTerminated()
      .values()

    assertEquals(1, events.size)
    assertEquals(true, events.first() is Either.Value)

    val values = (events.first() as Either.Value).value
    assertEquals(5, values.size)

    assertEquals(5L, values[0].photoId)
    assertEquals(4L, values[1].photoId)
    assertEquals(3L, values[2].photoId)
    assertEquals(2L, values[3].photoId)
    assertEquals(1L, values[4].photoId)

    Mockito.verify(uploadedPhotosRepository, Mockito.times(1)).findMany(photoIds)
    Mockito.verify(uploadedPhotosRepository, Mockito.times(1)).deleteOldPhotoInfos()
    Mockito.verify(apiClient, Mockito.times(1)).getUploadedPhotoIds(userId, lastId, photosPerPage)

    Mockito.verifyNoMoreInteractions(uploadedPhotosRepository)
    Mockito.verifyNoMoreInteractions(apiClient)
  }

  @Test
  fun `should make a network request when there are not cached photos in the database`() {
    val userId = "123"
    val lastId = 1000L
    val photosPerPage = 10
    val photoIds = listOf<Long>(1, 2, 3, 4, 5, 6, 7, 8, 9, 10)
    val notCachedPhotoIds = listOf<Long>(6, 7, 8, 9, 10)
    val uploadedPhotos1 = listOf(
      UploadedPhoto(4L, "456", 44.4, 44.4, false, 444L),
      UploadedPhoto(1L, "123", 11.1, 11.1, false, 111L),
      UploadedPhoto(3L, "345", 33.3, 33.3, false, 333L),
      UploadedPhoto(2L, "234", 22.2, 22.2, false, 222L),
      UploadedPhoto(5L, "567", 55.5, 55.5, false, 555L)
    )
    val uploadedPhotos2 = listOf(
      GetUploadedPhotosResponse.UploadedPhotoData(10L, "888", 100.5, 100.5, false, 1000L),
      GetUploadedPhotosResponse.UploadedPhotoData(7L, "789", 77.2, 77.2, false, 777L),
      GetUploadedPhotosResponse.UploadedPhotoData(6L, "678", 66.1, 66.1, false, 666L),
      GetUploadedPhotosResponse.UploadedPhotoData(8L, "890", 88.3, 88.3, false, 888L),
      GetUploadedPhotosResponse.UploadedPhotoData(9L, "777", 99.4, 99.4, false, 999L)
    )

    val photoIdsJoined = notCachedPhotoIds.joinToString(Constants.PHOTOS_SEPARATOR)

    Mockito.`when`(apiClient.getUploadedPhotoIds(userId, lastId, photosPerPage))
      .thenReturn(Single.just(GetUploadedPhotoIdsResponse.success(photoIds)))
    Mockito.`when`(apiClient.getUploadedPhotos(userId, photoIdsJoined))
      .thenReturn(Single.just(GetUploadedPhotosResponse.success(uploadedPhotos2)))
    Mockito.`when`(uploadedPhotosRepository.findMany(photoIds))
      .thenReturn(uploadedPhotos1)
    Mockito.`when`(uploadedPhotosRepository.saveMany(uploadedPhotos2))
      .thenReturn(true)
    Mockito.doNothing().`when`(uploadedPhotosRepository).deleteOldPhotoInfos()

    val events = getUploadedPhotosUseCase.loadPageOfPhotos(userId, lastId, photosPerPage)
      .test()
      .assertNoErrors()
      .assertTerminated()
      .values()

    assertEquals(1, events.size)
    assertEquals(true, events.first() is Either.Value)

    val values = (events.first() as Either.Value).value
    assertEquals(10, values.size)

    assertEquals(10L, values[0].photoId)
    assertEquals(9L, values[1].photoId)
    assertEquals(8L, values[2].photoId)
    assertEquals(7L, values[3].photoId)
    assertEquals(6L, values[4].photoId)
    assertEquals(5L, values[5].photoId)
    assertEquals(4L, values[6].photoId)
    assertEquals(3L, values[7].photoId)
    assertEquals(2L, values[8].photoId)
    assertEquals(1L, values[9].photoId)

    Mockito.verify(apiClient, Mockito.times(1)).getUploadedPhotoIds(Mockito.anyString(), Mockito.anyLong(), Mockito.anyInt())
    Mockito.verify(apiClient, Mockito.times(1)).getUploadedPhotos(Mockito.anyString(), Mockito.anyString())
    Mockito.verify(uploadedPhotosRepository, Mockito.times(1)).findMany(Mockito.anyList())
    Mockito.verify(uploadedPhotosRepository, Mockito.times(1)).saveMany(Mockito.anyList())
    Mockito.verify(uploadedPhotosRepository, Mockito.times(1)).deleteOldPhotoInfos()

    Mockito.verifyNoMoreInteractions(apiClient)
    Mockito.verifyNoMoreInteractions(uploadedPhotosRepository)
  }*/
}

























