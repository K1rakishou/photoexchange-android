package com.kirakishou.photoexchange.interactors

import com.kirakishou.photoexchange.helper.api.ApiClient
import com.kirakishou.photoexchange.helper.database.MyDatabase
import com.kirakishou.photoexchange.helper.database.mapper.ReceivedPhotosMapper
import com.kirakishou.photoexchange.helper.database.repository.ReceivedPhotosRepository
import com.kirakishou.photoexchange.helper.database.repository.TakenPhotosRepository
import com.kirakishou.photoexchange.helper.database.repository.UploadedPhotosRepository
import com.kirakishou.photoexchange.mvp.model.FindPhotosData
import com.kirakishou.photoexchange.mvp.model.net.response.ReceivedPhotosResponse
import com.kirakishou.photoexchange.service.ReceivePhotosServicePresenter
import com.nhaarman.mockito_kotlin.any
import io.reactivex.Single
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito

class ReceivePhotosUseCaseTest {

  lateinit var database: MyDatabase
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

  @Test
  fun `should return LocalCouldNotGetUserId when userId is null`() {
    val userId = null
    val photoNames = ""

    receivePhotosUseCaseTest.receivePhotos(FindPhotosData(userId, photoNames))
      .test()
      .assertNoErrors()
      .assertValueAt(0) { value ->
        value is ReceivePhotosServicePresenter.ReceivePhotoEvent.OnKnownError
      }
      .assertValueAt(0) { value ->
        (value as ReceivePhotosServicePresenter.ReceivePhotoEvent.OnKnownError).errorCode is ErrorCode.ReceivePhotosErrors.LocalCouldNotGetUserId
      }
      .assertTerminated()
      .awaitTerminalEvent()
  }

  @Test
  fun `should return LocalPhotoNamesAreEmpty when photoNames are empty`() {
    val userId = "123"
    val photoNames = ""

    receivePhotosUseCaseTest.receivePhotos(FindPhotosData(userId, photoNames))
      .test()
      .assertNoErrors()
      .assertValueAt(0) { value ->
        value is ReceivePhotosServicePresenter.ReceivePhotoEvent.OnKnownError
      }
      .assertValueAt(0) { value ->
        (value as ReceivePhotosServicePresenter.ReceivePhotoEvent.OnKnownError).errorCode is ErrorCode.ReceivePhotosErrors.LocalPhotoNamesAreEmpty
      }
      .assertTerminated()
      .awaitTerminalEvent()
  }

  @Test
  fun `should return errorCode when server does not return ok`() {
    val userId = "123"
    val photoNames = "photo1,photo2"

    Mockito.`when`(apiClient.receivePhotos(userId, photoNames))
      .thenReturn(Single.just(ReceivedPhotosResponse.error(ErrorCode.ReceivePhotosErrors.BadRequest())))

    receivePhotosUseCaseTest.receivePhotos(FindPhotosData(userId, photoNames))
      .test()
      .assertNoErrors()
      .assertValueAt(0) { value ->
        value is ReceivePhotosServicePresenter.ReceivePhotoEvent.OnKnownError
      }
      .assertValueAt(0) { value ->
        (value as ReceivePhotosServicePresenter.ReceivePhotoEvent.OnKnownError).errorCode is ErrorCode.ReceivePhotosErrors.BadRequest
      }
      .assertTerminated()
      .awaitTerminalEvent()
  }

  @Test
  fun `should return receivedPhotos when server returns ok`() {
    val userId = "123"
    val photoNames = "photo1,photo2"
    val receivedPhotos = listOf(
      ReceivedPhotosResponse.ReceivedPhoto(1L, "123", "456", 55.4, 44.5),
      ReceivedPhotosResponse.ReceivedPhoto(2L, "789", "000", 44.5, 22.4)
    )

    Mockito.`when`(apiClient.receivePhotos(userId, photoNames))
      .thenReturn(Single.just(ReceivedPhotosResponse.success(receivedPhotos)))
    Mockito.`when`(database.transactional(any()))
      .thenReturn(true)

    receivePhotosUseCaseTest.receivePhotos(FindPhotosData(userId, photoNames))
      .test()
      .assertNoErrors()
      .assertValueAt(0) { value ->
        value is ReceivePhotosServicePresenter.ReceivePhotoEvent.OnReceivedPhoto
      }
      .assertValueAt(0) { value ->
        (value as ReceivePhotosServicePresenter.ReceivePhotoEvent.OnReceivedPhoto).takenPhotoName ==
          receivedPhotos[0].uploadedPhotoName
      }
      .assertValueAt(0) { value ->
        (value as ReceivePhotosServicePresenter.ReceivePhotoEvent.OnReceivedPhoto).receivedPhoto ==
          ReceivedPhotosMapper.FromResponse.ReceivedPhotos.toReceivedPhoto(receivedPhotos[0])
      }
      .assertValueAt(1) { value ->
        value is ReceivePhotosServicePresenter.ReceivePhotoEvent.OnReceivedPhoto
      }
      .assertValueAt(1) { value ->
        (value as ReceivePhotosServicePresenter.ReceivePhotoEvent.OnReceivedPhoto).takenPhotoName ==
          receivedPhotos[1].uploadedPhotoName
      }
      .assertValueAt(1) { value ->
        (value as ReceivePhotosServicePresenter.ReceivePhotoEvent.OnReceivedPhoto).receivedPhoto ==
          ReceivedPhotosMapper.FromResponse.ReceivedPhotos.toReceivedPhoto(receivedPhotos[1])
      }
      .assertTerminated()
      .awaitTerminalEvent()
  }
}





























