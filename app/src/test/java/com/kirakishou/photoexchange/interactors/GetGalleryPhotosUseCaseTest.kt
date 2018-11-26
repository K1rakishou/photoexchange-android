package com.kirakishou.photoexchange.interactors

import com.kirakishou.photoexchange.helper.Either
import com.kirakishou.photoexchange.helper.api.ApiClient
import com.kirakishou.photoexchange.helper.concurrency.coroutines.DispatchersProvider
import com.kirakishou.photoexchange.helper.concurrency.coroutines.TestDispatchers
import com.kirakishou.photoexchange.helper.database.MyDatabase
import com.kirakishou.photoexchange.helper.database.repository.GetGalleryPhotosRepository
import com.kirakishou.photoexchange.helper.util.TimeUtils
import com.kirakishou.photoexchange.mvp.model.photo.GalleryPhoto
import com.kirakishou.photoexchange.helper.exception.ApiErrorException
import com.kirakishou.photoexchange.helper.exception.DatabaseException
import core.ErrorCode
import kotlinx.coroutines.runBlocking
import net.response.GalleryPhotosResponse
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito
import java.lang.Exception

class GetGalleryPhotosUseCaseTest {

  lateinit var database: MyDatabase
  lateinit var apiClient: ApiClient
  lateinit var timeUtils: TimeUtils
  lateinit var dispatchersProvider: DispatchersProvider
  lateinit var getGalleryPhotosRepository: GetGalleryPhotosRepository
  lateinit var getGalleryPhotosUseCase: GetGalleryPhotosUseCase

  @Before
  fun setUp() {
    database = Mockito.mock(MyDatabase::class.java)
    apiClient = Mockito.mock(ApiClient::class.java)
    getGalleryPhotosRepository = Mockito.mock(GetGalleryPhotosRepository::class.java)
    timeUtils = Mockito.mock(TimeUtils::class.java)
    dispatchersProvider = TestDispatchers()

    getGalleryPhotosUseCase = GetGalleryPhotosUseCase(
      apiClient,
      getGalleryPhotosRepository,
      timeUtils,
      dispatchersProvider
    )
  }

  @After
  fun tearDown() {

  }

  @Test
  fun `should return photos cached photos when there are enough in the database`() {
    val time = 10L
    val count = 5
    val expectedPhotos = listOf(
      GalleryPhoto(1, "1", 11.1, 11.1, 1, 0, null),
      GalleryPhoto(2, "1", 11.1, 11.1, 1, 0, null),
      GalleryPhoto(3, "1", 11.1, 11.1, 1, 0, null),
      GalleryPhoto(4, "1", 11.1, 11.1, 1, 0, null),
      GalleryPhoto(5, "1", 11.1, 11.1, 1, 0, null)
    )

    runBlocking {
      Mockito.`when`(getGalleryPhotosRepository.getPageOfGalleryPhotos(time, count))
        .thenReturn(expectedPhotos)

      val result = getGalleryPhotosUseCase.loadPageOfPhotos(time, count)
      assertTrue(result is Either.Value<List<GalleryPhoto>>)

      val actualPhotos = (result as Either.Value<List<GalleryPhoto>>).value
      assertEquals(5, actualPhotos.size)

      for (i in 0 until expectedPhotos.size) {
        assertEquals(expectedPhotos[expectedPhotos.lastIndex - i].galleryPhotoId, actualPhotos[i].galleryPhotoId)
      }

      Mockito.verify(getGalleryPhotosRepository, Mockito.times(1)).deleteOldPhotos()
      Mockito.verify(getGalleryPhotosRepository, Mockito.times(1)).getPageOfGalleryPhotos(time, count)

      Mockito.verifyNoMoreInteractions(getGalleryPhotosRepository)
    }
  }

  @Test
  fun `should return whatever there is in the database when server returned empty list`() {
    val time = 10L
    val count = 5
    val expectedPhotos = listOf(
      GalleryPhoto(1, "1", 11.1, 11.1, 1, 0, null),
      GalleryPhoto(2, "1", 11.1, 11.1, 1, 0, null),
      GalleryPhoto(3, "1", 11.1, 11.1, 1, 0, null)
    )

    runBlocking {
      Mockito.`when`(getGalleryPhotosRepository.getPageOfGalleryPhotos(time, count))
        .thenReturn(expectedPhotos)
      Mockito.`when`(apiClient.getPageOfGalleryPhotos(time, count))
        .thenReturn(emptyList())

      val result = getGalleryPhotosUseCase.loadPageOfPhotos(time, count)
      assertTrue(result is Either.Value<List<GalleryPhoto>>)

      val actualPhotos = (result as Either.Value<List<GalleryPhoto>>).value
      assertEquals(3, actualPhotos.size)

      for (i in 0 until expectedPhotos.size) {
        assertEquals(expectedPhotos[expectedPhotos.lastIndex - i].galleryPhotoId, actualPhotos[i].galleryPhotoId)
      }

      Mockito.verify(getGalleryPhotosRepository, Mockito.times(1)).deleteOldPhotos()
      Mockito.verify(getGalleryPhotosRepository, Mockito.times(1)).getPageOfGalleryPhotos(time, count)
      Mockito.verify(apiClient, Mockito.times(1)).getPageOfGalleryPhotos(time, count)

      Mockito.verifyNoMoreInteractions(getGalleryPhotosRepository)
      Mockito.verifyNoMoreInteractions(apiClient)
    }
  }

  @Test
  fun `should return EitherError with ApiErrorException when apiClient threw ApiErrorException`() {
    val time = 10L
    val count = 5
    val expectedPhotos = listOf(
      GalleryPhoto(1, "1", 11.1, 11.1, 1, 0, null),
      GalleryPhoto(2, "1", 11.1, 11.1, 1, 0, null),
      GalleryPhoto(3, "1", 11.1, 11.1, 1, 0, null)
    )

    runBlocking {
      Mockito.`when`(getGalleryPhotosRepository.getPageOfGalleryPhotos(time, count))
        .thenReturn(expectedPhotos)
      Mockito.doThrow(ApiErrorException(ErrorCode.DatabaseError))
        .`when`(apiClient).getPageOfGalleryPhotos(time, count)

      val result = getGalleryPhotosUseCase.loadPageOfPhotos(time, count)
      assertTrue(result is Either.Error<Exception>)

      val error = (result as Either.Error<Exception>).error
      assertTrue(error is ApiErrorException)

      val errorCode = (error as ApiErrorException).errorCode
      assertEquals(errorCode, ErrorCode.DatabaseError)

      Mockito.verify(getGalleryPhotosRepository, Mockito.times(1)).deleteOldPhotos()
      Mockito.verify(getGalleryPhotosRepository, Mockito.times(1)).getPageOfGalleryPhotos(time, count)
      Mockito.verify(apiClient, Mockito.times(1)).getPageOfGalleryPhotos(time, count)

      Mockito.verifyNoMoreInteractions(getGalleryPhotosRepository)
      Mockito.verifyNoMoreInteractions(apiClient)
    }
  }

  @Test
  fun `should return EitherError with DatabaseException when could not store fresh photos`() {
    val time = 10L
    val count = 5
    val expectedPhotos1 = listOf(
      GalleryPhoto(1, "1", 11.1, 11.1, 1, 0, null),
      GalleryPhoto(2, "1", 11.1, 11.1, 1, 0, null),
      GalleryPhoto(3, "1", 11.1, 11.1, 1, 0, null)
    )
    val expectedPhotos2 = listOf(
      GalleryPhotosResponse.GalleryPhotoResponseData(4, "1", 11.1, 11.1, 1, 0),
      GalleryPhotosResponse.GalleryPhotoResponseData(5, "1", 11.1, 11.1, 1, 0)
    )

    runBlocking {
      Mockito.`when`(getGalleryPhotosRepository.getPageOfGalleryPhotos(time, count))
        .thenReturn(expectedPhotos1)
      Mockito.`when`(apiClient.getPageOfGalleryPhotos(time, count))
        .thenReturn(expectedPhotos2)
      Mockito.`when`(getGalleryPhotosRepository.saveMany(expectedPhotos2))
        .thenReturn(false)

      val result = getGalleryPhotosUseCase.loadPageOfPhotos(time, count)
      assertTrue(result is Either.Error<Exception>)

      val error = (result as Either.Error<Exception>).error
      assertTrue(error is DatabaseException)

      val message = (error as DatabaseException).message
      assertEquals("Could not cache gallery photos in the database", message)

      Mockito.verify(getGalleryPhotosRepository, Mockito.times(1)).deleteOldPhotos()
      Mockito.verify(getGalleryPhotosRepository, Mockito.times(1)).getPageOfGalleryPhotos(time, count)
      Mockito.verify(getGalleryPhotosRepository, Mockito.times(1)).saveMany(expectedPhotos2)
      Mockito.verify(apiClient, Mockito.times(1)).getPageOfGalleryPhotos(time, count)

      Mockito.verifyNoMoreInteractions(getGalleryPhotosRepository)
      Mockito.verifyNoMoreInteractions(apiClient)
    }
  }

  @Test
  fun `should return cached photos and fresh photos combined and sorted by photoId descending when everything is ok`() {
    val time = 10L
    val count = 5
    val expectedPhotos1 = listOf(
      GalleryPhoto(1, "1", 11.1, 11.1, 1, 0, null),
      GalleryPhoto(2, "1", 11.1, 11.1, 1, 0, null),
      GalleryPhoto(3, "1", 11.1, 11.1, 1, 0, null)
    )
    val expectedPhotos2 = listOf(
      GalleryPhotosResponse.GalleryPhotoResponseData(1, "1", 11.1, 11.1, 1, 0),
      GalleryPhotosResponse.GalleryPhotoResponseData(2, "1", 11.1, 11.1, 1, 0),
      GalleryPhotosResponse.GalleryPhotoResponseData(3, "1", 11.1, 11.1, 1, 0),
      GalleryPhotosResponse.GalleryPhotoResponseData(4, "1", 11.1, 11.1, 1, 0),
      GalleryPhotosResponse.GalleryPhotoResponseData(5, "1", 11.1, 11.1, 1, 0)
    )
    val expectedPhotos3 = listOf(
      GalleryPhoto(1, "1", 11.1, 11.1, 1, 0, null),
      GalleryPhoto(2, "1", 11.1, 11.1, 1, 0, null),
      GalleryPhoto(3, "1", 11.1, 11.1, 1, 0, null),
      GalleryPhoto(4, "1", 11.1, 11.1, 1, 0, null),
      GalleryPhoto(5, "1", 11.1, 11.1, 1, 0, null)
    )

    runBlocking {
      Mockito.`when`(getGalleryPhotosRepository.getPageOfGalleryPhotos(time, count))
        .thenReturn(expectedPhotos1)
      Mockito.`when`(apiClient.getPageOfGalleryPhotos(time, count))
        .thenReturn(expectedPhotos2)
      Mockito.`when`(getGalleryPhotosRepository.saveMany(expectedPhotos2))
        .thenReturn(true)

      val result = getGalleryPhotosUseCase.loadPageOfPhotos(time, count)
      assertTrue(result is Either.Value<List<GalleryPhoto>>)

      val actualPhotos = (result as Either.Value<List<GalleryPhoto>>).value
      assertEquals(expectedPhotos3.size, actualPhotos.size)

      for (i in 0 until expectedPhotos3.size) {
        assertEquals(expectedPhotos3[expectedPhotos3.lastIndex - i].galleryPhotoId, actualPhotos[i].galleryPhotoId)
      }

      Mockito.verify(getGalleryPhotosRepository, Mockito.times(1)).deleteOldPhotos()
      Mockito.verify(getGalleryPhotosRepository, Mockito.times(1)).getPageOfGalleryPhotos(time, count)
      Mockito.verify(getGalleryPhotosRepository, Mockito.times(1)).saveMany(expectedPhotos2)
      Mockito.verify(apiClient, Mockito.times(1)).getPageOfGalleryPhotos(time, count)

      Mockito.verifyNoMoreInteractions(getGalleryPhotosRepository)
      Mockito.verifyNoMoreInteractions(apiClient)
    }
  }
}




























