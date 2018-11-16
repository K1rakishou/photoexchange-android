package com.kirakishou.photoexchange.interactors

import com.kirakishou.photoexchange.helper.Either
import com.kirakishou.photoexchange.helper.api.ApiClient
import com.kirakishou.photoexchange.helper.database.MyDatabase
import com.kirakishou.photoexchange.helper.database.repository.GalleryPhotoRepository
import com.kirakishou.photoexchange.mvp.model.GalleryPhoto
import com.kirakishou.photoexchange.mvp.model.GalleryPhotoInfo
import com.kirakishou.photoexchange.mvp.model.net.response.GalleryPhotoIdsResponse
import com.kirakishou.photoexchange.mvp.model.net.response.GalleryPhotosResponse
import com.kirakishou.photoexchange.mvp.model.other.Constants
import com.kirakishou.photoexchange.mvp.model.other.ErrorCode
import io.reactivex.Single
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito

class GetGalleryPhotosUseCaseTest {

  lateinit var database: MyDatabase
  lateinit var apiClient: ApiClient
  lateinit var galleryPhotoRepository: GalleryPhotoRepository
  lateinit var getGalleryPhotosUseCase: GetGalleryPhotosUseCase

  @Before
  fun setUp() {
    database = Mockito.mock(MyDatabase::class.java)
    apiClient = Mockito.mock(ApiClient::class.java)
    galleryPhotoRepository = Mockito.mock(GalleryPhotoRepository::class.java)

    getGalleryPhotosUseCase = GetGalleryPhotosUseCase(
      apiClient,
      galleryPhotoRepository
    )
  }

  @After
  fun tearDown() {

  }

  @Test
  fun `should return EitherError with errorCode when server does not return ok`() {
    val lastId = 10L
    val photosPerPage = 10

    Mockito.`when`(apiClient.getGalleryPhotoIds(lastId, photosPerPage))
      .thenReturn(Single.just(GalleryPhotoIdsResponse.error(ErrorCode.GetGalleryPhotosErrors.BadRequest())))

    getGalleryPhotosUseCase.loadPageOfPhotos(lastId, photosPerPage)
      .test()
      .assertNoErrors()
      .assertValueAt(0) { value ->
        value is Either.Error
      }
      .assertValueAt(0) { value ->
        (value as Either.Error).error is ErrorCode.GetGalleryPhotosErrors.BadRequest
      }
      .assertTerminated()
      .awaitTerminalEvent()

    Mockito.verify(apiClient, Mockito.times(1)).getGalleryPhotoIds(lastId, photosPerPage)

    Mockito.verifyNoMoreInteractions(galleryPhotoRepository)
    Mockito.verifyNoMoreInteractions(apiClient)
  }

  @Test
  fun `should return nothing when there are no photos on server`() {
    val lastId = 10L
    val photosPerPage = 10
    val galleryPhotoIds = listOf<Long>()

    Mockito.`when`(apiClient.getGalleryPhotoIds(lastId, photosPerPage))
      .thenReturn(Single.just(GalleryPhotoIdsResponse.success(galleryPhotoIds)))

    getGalleryPhotosUseCase.loadPageOfPhotos(lastId, photosPerPage)
      .test()
      .assertNoErrors()
      .assertValueAt(0) { value ->
        value is Either.Value
      }
      .assertValueAt(0) { value ->
        (value as Either.Value).value.isEmpty()
      }
      .assertTerminated()
      .awaitTerminalEvent()

    Mockito.verify(apiClient, Mockito.times(1)).getGalleryPhotoIds(lastId, photosPerPage)

    Mockito.verifyNoMoreInteractions(galleryPhotoRepository)
    Mockito.verifyNoMoreInteractions(apiClient)
  }

  @Test
  fun `should return the photos from the database when it contains all of the photoIds`() {
    val lastId = 10L
    val photosPerPage = 10
    val galleryPhotoIds = listOf<Long>(1, 2, 3, 4, 5)
    val galleryPhotos = listOf(
      GalleryPhoto(1L, "1", 11.1, 11.1, 5555L, 0L, null),
      GalleryPhoto(2L, "2", 22.1, 22.1, 6555L, 2L, GalleryPhotoInfo(2L, false, false)),
      GalleryPhoto(3L, "3", 33.1, 33.1, 7555L, 4L, null),
      GalleryPhoto(4L, "4", 44.1, 44.1, 8555L, 1L, GalleryPhotoInfo(4L, true, true)),
      GalleryPhoto(5L, "5", 55.1, 55.1, 9555L, 3L, null)
    )

    Mockito.`when`(apiClient.getGalleryPhotoIds(lastId, photosPerPage))
      .thenReturn(Single.just(GalleryPhotoIdsResponse.success(galleryPhotoIds)))
    Mockito.`when`(galleryPhotoRepository.findMany(galleryPhotoIds))
      .thenReturn(galleryPhotos)
    Mockito.doNothing().`when`(galleryPhotoRepository).deleteOldPhotos()

    val events = getGalleryPhotosUseCase.loadPageOfPhotos(lastId, photosPerPage)
      .test()
      .assertNoErrors()
      .assertTerminated()
      .values()

    assertEquals(1, events.size)
    assertEquals(true, events.first() is Either.Value)

    val values = (events.first() as Either.Value).value
    assertEquals(5, values.size)

    Mockito.verify(galleryPhotoRepository, Mockito.times(1)).findMany(galleryPhotoIds)
    Mockito.verify(galleryPhotoRepository, Mockito.times(1)).deleteOldPhotos()
    Mockito.verify(apiClient, Mockito.times(1)).getGalleryPhotoIds(lastId, photosPerPage)

    Mockito.verifyNoMoreInteractions(galleryPhotoRepository)
    Mockito.verifyNoMoreInteractions(apiClient)
  }

  @Test
  fun `should grab fresh photos from server when photo was not found in the database and concatenate them together`() {
    val lastId = 10L
    val photosPerPage = 10

    val galleryPhotoIds = listOf<Long>(1, 2, 3, 4, 5, 6, 7, 8, 9, 10)
    val galleryPhotosFromDb = listOf(
      GalleryPhoto(1L, "1", 11.1, 11.1, 5555L, 0L, null),
      GalleryPhoto(2L, "2", 22.1, 22.1, 6555L, 2L, GalleryPhotoInfo(2L, false, false)),
      GalleryPhoto(3L, "3", 33.1, 33.1, 7555L, 4L, null),
      GalleryPhoto(4L, "4", 44.1, 44.1, 8555L, 1L, GalleryPhotoInfo(4L, true, true)),
      GalleryPhoto(5L, "5", 55.1, 55.1, 9555L, 3L, null)
    )

    val filtered = listOf<Long>(6, 7, 8, 9, 10)
    val galleryPhotosFromServer = listOf(
      GalleryPhotosResponse.GalleryPhotoResponseData(6L, "6", 11.1, 11.1, 5555L, 0L),
      GalleryPhotosResponse.GalleryPhotoResponseData(7L, "7", 22.1, 22.1, 6555L, 2L),
      GalleryPhotosResponse.GalleryPhotoResponseData(8L, "8", 33.1, 33.1, 7555L, 4L),
      GalleryPhotosResponse.GalleryPhotoResponseData(9L, "9", 44.1, 44.1, 8555L, 1L),
      GalleryPhotosResponse.GalleryPhotoResponseData(10L, "10", 55.1, 55.1, 9555L, 3L)
    )

    val photoIdsJoined = filtered.joinToString(Constants.PHOTOS_DELIMITER)

    Mockito.`when`(apiClient.getGalleryPhotoIds(lastId, photosPerPage))
      .thenReturn(Single.just(GalleryPhotoIdsResponse.success(galleryPhotoIds)))
    Mockito.`when`(apiClient.getGalleryPhotos(photoIdsJoined))
      .thenReturn(Single.just(GalleryPhotosResponse.success(galleryPhotosFromServer)))
    Mockito.`when`(galleryPhotoRepository.findMany(galleryPhotoIds))
      .thenReturn(galleryPhotosFromDb)
    Mockito.`when`(galleryPhotoRepository.saveMany(galleryPhotosFromServer))
      .thenReturn(true)
    Mockito.doNothing().`when`(galleryPhotoRepository).deleteOldPhotos()

    val events = getGalleryPhotosUseCase.loadPageOfPhotos(lastId, photosPerPage)
      .test()
      .assertNoErrors()
      .assertTerminated()
      .values()

    assertEquals(1, events.size)
    assertEquals(true, events.first() is Either.Value)

    val values = (events.first() as Either.Value).value
    assertEquals(10, values.size)

    Mockito.verify(galleryPhotoRepository, Mockito.times(1)).findMany(galleryPhotoIds)
    Mockito.verify(galleryPhotoRepository, Mockito.times(1)).saveMany(galleryPhotosFromServer)
    Mockito.verify(galleryPhotoRepository, Mockito.times(1)).deleteOldPhotos()
    Mockito.verify(apiClient, Mockito.times(1)).getGalleryPhotoIds(lastId, photosPerPage)
    Mockito.verify(apiClient, Mockito.times(1)).getGalleryPhotos(photoIdsJoined)

    Mockito.verifyNoMoreInteractions(galleryPhotoRepository)
    Mockito.verifyNoMoreInteractions(apiClient)
  }

  @Test
  fun `should only return photos from database when could not get fresh photos from server`() {
    val lastId = 10L
    val photosPerPage = 10

    val galleryPhotoIds = listOf<Long>(1, 2, 3, 4, 5, 6, 7, 8, 9, 10)
    val galleryPhotosFromDb = listOf(
      GalleryPhoto(1L, "1", 11.1, 11.1, 5555L, 0L, null),
      GalleryPhoto(2L, "2", 22.1, 22.1, 6555L, 2L, GalleryPhotoInfo(2L, false, false)),
      GalleryPhoto(3L, "3", 33.1, 33.1, 7555L, 4L, null),
      GalleryPhoto(4L, "4", 44.1, 44.1, 8555L, 1L, GalleryPhotoInfo(4L, true, true)),
      GalleryPhoto(5L, "5", 55.1, 55.1, 9555L, 3L, null)
    )

    val filtered = listOf<Long>(6, 7, 8, 9, 10)
    val galleryPhotosFromServer = listOf(
      GalleryPhotosResponse.GalleryPhotoResponseData(6L, "6", 11.1, 11.1, 5555L, 0L),
      GalleryPhotosResponse.GalleryPhotoResponseData(7L, "7", 22.1, 22.1, 6555L, 2L),
      GalleryPhotosResponse.GalleryPhotoResponseData(8L, "8", 33.1, 33.1, 7555L, 4L),
      GalleryPhotosResponse.GalleryPhotoResponseData(9L, "9", 44.1, 44.1, 8555L, 1L),
      GalleryPhotosResponse.GalleryPhotoResponseData(10L, "10", 55.1, 55.1, 9555L, 3L)
    )

    val photoIdsJoined = filtered.joinToString(Constants.PHOTOS_DELIMITER)

    Mockito.`when`(apiClient.getGalleryPhotoIds(lastId, photosPerPage))
      .thenReturn(Single.just(GalleryPhotoIdsResponse.success(galleryPhotoIds)))
    Mockito.`when`(apiClient.getGalleryPhotos(photoIdsJoined))
      .thenReturn(Single.just(GalleryPhotosResponse.fail(ErrorCode.GetGalleryPhotosErrors.UnknownError())))
    Mockito.`when`(galleryPhotoRepository.findMany(galleryPhotoIds))
      .thenReturn(galleryPhotosFromDb)
    Mockito.`when`(galleryPhotoRepository.saveMany(galleryPhotosFromServer))
      .thenReturn(true)
    Mockito.doNothing().`when`(galleryPhotoRepository).deleteOldPhotos()

    val events = getGalleryPhotosUseCase.loadPageOfPhotos(lastId, photosPerPage)
      .test()
      .assertNoErrors()
      .assertTerminated()
      .values()

    assertEquals(1, events.size)
    assertEquals(true, events.first() is Either.Value)

    val values = (events.first() as Either.Value).value
    assertEquals(5, values.size)

    Mockito.verify(galleryPhotoRepository, Mockito.times(1)).findMany(galleryPhotoIds)
    Mockito.verify(galleryPhotoRepository, Mockito.times(1)).deleteOldPhotos()
    Mockito.verify(apiClient, Mockito.times(1)).getGalleryPhotoIds(lastId, photosPerPage)
    Mockito.verify(apiClient, Mockito.times(1)).getGalleryPhotos(photoIdsJoined)

    Mockito.verifyNoMoreInteractions(galleryPhotoRepository)
    Mockito.verifyNoMoreInteractions(apiClient)
  }
}




























