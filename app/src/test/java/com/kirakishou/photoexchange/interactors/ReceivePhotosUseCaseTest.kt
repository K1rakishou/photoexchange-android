package com.kirakishou.photoexchange.interactors

class ReceivePhotosUseCaseTest {

  /*lateinit var database: MyDatabase
  lateinit var takenPhotosRepository: TakenPhotosRepository
  lateinit var receivedPhotosRepository: ReceivedPhotosRepository
  lateinit var uploadedPhotosRepository: UploadedPhotosRepository
  lateinit var apiClient: ApiClient

  lateinit var receivePhotosUseCaseTest: ReceivePhotosUseCase

  @Before
  fun setUp() {
    database = Mockito.mock(MyDatabase::class.java)
    takenPhotosRepository = Mockito.mock(TakenPhotosRepository::class.java)
    receivedPhotosRepository = Mockito.mock(ReceivedPhotosRepository::class.java)
    uploadedPhotosRepository = Mockito.mock(UploadedPhotosRepository::class.java)
    apiClient = Mockito.mock(ApiClient::class.java)

    receivePhotosUseCaseTest = ReceivePhotosUseCase(
      database,
      takenPhotosRepository,
      receivedPhotosRepository,
      uploadedPhotosRepository,
      apiClient
    )
  }

  @After
  fun tearDown() {

  }

  @Test(expected = ReceivePhotosUseCase.ReceivePhotosServiceException.UserIdIsEmptyException::class)
  fun `should throw UserIdIsEmptyException when userId is null`() {
    val userId = null
    val photoNames = ""

    runBlocking {
      receivePhotosUseCaseTest.receivePhotos(FindPhotosData(userId, photoNames))
    }
  }

  @Test(expected = ReceivePhotosUseCase.ReceivePhotosServiceException.PhotoNamesAreEmpty::class)
  fun `should throw PhotoNamesAreEmpty when photoNames are empty`() {
    val userId = "123"
    val photoNames = ""

    runBlocking {
      receivePhotosUseCaseTest.receivePhotos(FindPhotosData(userId, photoNames))
    }
  }

  @Test(expected = ReceivePhotosUseCase.ReceivePhotosServiceException.ApiException::class)
  fun `should throw ApiException when server does not return ok`() {
    val userId = "123"
    val photoNames = "photo1,photo2"

    runBlocking {
      Mockito.doThrow(ApiErrorException(ErrorCode.DatabaseError))
        .`when`(apiClient).receivePhotos(userId, photoNames)

      receivePhotosUseCaseTest.receivePhotos(FindPhotosData(userId, photoNames))
    }
  }

  @Test
  fun `should return receivedPhotos when server returns ok`() {
    val userId = "123"
    val photoNames = "photo1,photo2"
    val expectedPhotos = listOf(
      ReceivePhotosResponse.ReceivedPhotoResponseData(1L, "123", "456", 55.4, 44.5),
      ReceivePhotosResponse.ReceivedPhotoResponseData(2L, "789", "000", 44.5, 22.4)
    )

    runBlocking {
      Mockito.`when`(apiClient.receivePhotos(userId, photoNames))
        .thenReturn(expectedPhotos)
      Mockito.`when`(database.transactional(any()))
        .thenReturn(true)

      val receivedPhotos = receivePhotosUseCaseTest.receivePhotos(FindPhotosData(userId, photoNames))

      assertEquals(expectedPhotos.size, receivedPhotos.size)

      assertTrue(receivedPhotos[0].first.uploadedPhotoName == expectedPhotos[0].uploadedPhotoName)
      assertTrue(receivedPhotos[0].first.receivedPhotoName == expectedPhotos[0].receivedPhotoName)

      assertTrue(receivedPhotos[1].first.uploadedPhotoName == expectedPhotos[1].uploadedPhotoName)
      assertTrue(receivedPhotos[1].first.receivedPhotoName == expectedPhotos[1].receivedPhotoName)
    }
  }*/
}





























