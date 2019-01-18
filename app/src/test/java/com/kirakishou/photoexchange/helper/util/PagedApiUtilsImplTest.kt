package com.kirakishou.photoexchange.helper.util

import com.kirakishou.photoexchange.helper.LonLat
import com.kirakishou.photoexchange.helper.exception.AttemptToAccessInternetWithMeteredNetworkException
import com.kirakishou.photoexchange.helper.exception.ConnectionError
import com.kirakishou.photoexchange.mvrx.model.photo.GalleryPhoto
import com.kirakishou.photoexchange.mvrx.model.photo.PhotoAdditionalInfo
import junit.framework.Assert.*
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito

class PagedApiUtilsImplTest {

  private lateinit var timeUtils: TimeUtils
  private lateinit var pagedApiUtils: PagedApiUtils

  private val currentTime = 1000L

  private val photosFromCache = listOf(
    GalleryPhoto(
      "123",
      LonLat.empty(),
      21212L,
      PhotoAdditionalInfo.empty("123")
    )
  )
  private val photosFromServer = listOf(
    GalleryPhoto(
      "222",
      LonLat(11.1, 22.2),
      400L,
      PhotoAdditionalInfo.empty("222")
    ),
    GalleryPhoto(
      "333",
      LonLat(12.1, 23.2),
      401L,
      PhotoAdditionalInfo.empty("333")
    )
  )
  private val freshPhotosFromServer = listOf(
    GalleryPhoto(
      "444",
      LonLat(51.1, 22.2),
      396L,
      PhotoAdditionalInfo.empty("444")
    ),
    GalleryPhoto(
      "555",
      LonLat(42.1, 23.2),
      397L,
      PhotoAdditionalInfo.empty("555")
    ),
    GalleryPhoto(
      "666",
      LonLat(22.1, 63.2),
      398L,
      PhotoAdditionalInfo.empty("666")
    )
  )

  @Before
  fun setUp() {
    timeUtils = Mockito.mock(TimeUtils::class.java)

    pagedApiUtils = PagedApiUtilsImpl(
      timeUtils
    )
  }

  private fun assertNotCalled(): Nothing = throw RuntimeException("Must not be called")
  private fun throwMeteredNetworkException(): Nothing = throw AttemptToAccessInternetWithMeteredNetworkException("BAM")

  @Test
  fun `should return photos from cache when there is no internet and network access level is less than canAccessNetwork`() {
    runBlocking {
      val page = pagedApiUtils.getPageOfPhotos<GalleryPhoto>(
        "test",
        0,
        currentTime,
        5,
        "234",
        getPhotosFromCacheFunc = { _, _ -> photosFromCache },
        getFreshPhotosFunc = { throwMeteredNetworkException() },
        getPageOfPhotosFunc = { _, _, _ -> assertNotCalled() },
        clearCacheFunc = { assertNotCalled() },
        deleteOldFunc = { assertNotCalled() },
        filterBannedPhotosFunc = { assertNotCalled() },
        cachePhotosFunc = { assertNotCalled() }
      )

      assertEquals(1, page.page.size)
      assertEquals("123", page.page.first().photoName)

      assertTrue(page.isEnd)
    }
  }

  @Test
  fun `should return photos from cache when it's first run and there are enough photos in the cache`() {
    runBlocking {
      val page = pagedApiUtils.getPageOfPhotos<GalleryPhoto>(
        "test",
        -1L,
        currentTime,
        1,
        "234",
        getPhotosFromCacheFunc = { _, _ -> photosFromCache },
        getFreshPhotosFunc = { listOf<GalleryPhoto>() },
        getPageOfPhotosFunc = { _, _, _ -> assertNotCalled() },
        clearCacheFunc = { assertNotCalled() },
        deleteOldFunc = { assertNotCalled() },
        filterBannedPhotosFunc = { assertNotCalled() },
        cachePhotosFunc = { assertNotCalled() }
      )

      assertEquals(1, page.page.size)
      assertEquals("123", page.page.first().photoName)

      assertFalse(page.isEnd)
    }
  }

  @Test
  fun `should get fresh photos from server when it's first run and there are not enough photos in the cache`() {
    runBlocking {
      var deleteOldFuncCalled = false

      val page = pagedApiUtils.getPageOfPhotos(
        "test",
        -1L,
        currentTime,
        2,
        "234",
        getPhotosFromCacheFunc = { _, _ -> photosFromCache },
        getFreshPhotosFunc = { listOf<GalleryPhoto>() },
        getPageOfPhotosFunc = { _, _, _ -> photosFromServer },
        clearCacheFunc = { assertNotCalled() },
        deleteOldFunc = { deleteOldFuncCalled = true },
        filterBannedPhotosFunc = { photos -> photos },
        cachePhotosFunc = { true }
      )

      assertTrue(deleteOldFuncCalled)

      assertEquals(2, page.page.size)
      val photos = page.page

      assertEquals("222", photos[0].photoName)
      assertEquals("333", photos[1].photoName)

      assertFalse(page.isEnd)
    }
  }

  @Test
  fun `should return photos from cache when attempt to fetch photos from server resulted in connection exception`() {
    runBlocking {
      val page = pagedApiUtils.getPageOfPhotos<GalleryPhoto>(
        "test",
        -1L,
        currentTime,
        2,
        "234",
        getPhotosFromCacheFunc = { _, _ -> photosFromCache },
        getFreshPhotosFunc = { listOf<GalleryPhoto>() },
        getPageOfPhotosFunc = { _, _, _ -> throw ConnectionError("BAM") },
        clearCacheFunc = { assertNotCalled() },
        deleteOldFunc = { assertNotCalled() },
        filterBannedPhotosFunc = { assertNotCalled() },
        cachePhotosFunc = { assertNotCalled() }
      )

      assertEquals(1, page.page.size)
      val photos = page.page

      assertEquals("123", photos[0].photoName)
      assertTrue(page.isEnd)
    }
  }

  @Test
  fun `should return photos from cache when attempt to get amount of fresh photos on the server resulted in connection exception`() {
    runBlocking {
      val page = pagedApiUtils.getPageOfPhotos<GalleryPhoto>(
        "test",
        1L,
        currentTime,
        2,
        "234",
        getPhotosFromCacheFunc = { _, _ -> photosFromCache },
        getFreshPhotosFunc = { throw ConnectionError("BAM") },
        getPageOfPhotosFunc = { _, _, _ -> assertNotCalled() },
        clearCacheFunc = { assertNotCalled() },
        deleteOldFunc = { assertNotCalled() },
        filterBannedPhotosFunc = { assertNotCalled() },
        cachePhotosFunc = { assertNotCalled() }
      )

      assertEquals(1, page.page.size)
      assertEquals("123", page.page.first().photoName)

      assertTrue(page.isEnd)
    }
  }

  @Test
  fun `should return photos from server when there are no fresh photos on the server and not enough photos in the cache`() {
    runBlocking {
      var deleteOldFuncCalled = false
      val page = pagedApiUtils.getPageOfPhotos<GalleryPhoto>(
        "test",
        1L,
        currentTime,
        2,
        "234",
        getPhotosFromCacheFunc = { _, _ -> photosFromCache },
        getFreshPhotosFunc = { listOf<GalleryPhoto>() },
        getPageOfPhotosFunc = { _, _, _ -> photosFromServer },
        clearCacheFunc = { assertNotCalled() },
        deleteOldFunc = { deleteOldFuncCalled = true },
        filterBannedPhotosFunc = { it },
        cachePhotosFunc = { true }
      )

      assertTrue(deleteOldFuncCalled)

      assertEquals(2, page.page.size)
      val photos = page.page

      assertEquals("222", photos[0].photoName)
      assertEquals("333", photos[1].photoName)

      assertFalse(page.isEnd)
    }
  }

  @Test
  fun `should return photos from cache when there are fresh photos on the server but attempt to get them resulted in connection exception`() {
    runBlocking {
      val page = pagedApiUtils.getPageOfPhotos<GalleryPhoto>(
        "test",
        1L,
        currentTime,
        2,
        "234",
        getPhotosFromCacheFunc = { _, _ -> photosFromCache },
        getFreshPhotosFunc = { freshPhotosFromServer.take(2) },
        getPageOfPhotosFunc = { _, _, _ -> throw ConnectionError("BAM") },
        clearCacheFunc = { assertNotCalled() },
        deleteOldFunc = { assertNotCalled() },
        filterBannedPhotosFunc = { assertNotCalled() },
        cachePhotosFunc = { assertNotCalled() }
      )

      assertEquals(1, page.page.size)
      val photos = page.page

      assertEquals("123", photos[0].photoName)
      assertTrue(page.isEnd)
    }
  }

  @Test
  fun `should return page of photos concatenated with fresh photos when there are fresh photos on the server`() {
    runBlocking {
      var deleteOldFuncCalled = false
      val page = pagedApiUtils.getPageOfPhotos<GalleryPhoto>(
        "test",
        1L,
        currentTime,
        2,
        "234",
        getPhotosFromCacheFunc = { _, _ -> photosFromCache },
        getFreshPhotosFunc = { freshPhotosFromServer.take(2) },
        getPageOfPhotosFunc = { _, _, _ -> photosFromServer },
        clearCacheFunc = { assertNotCalled() },
        deleteOldFunc = { deleteOldFuncCalled = true },
        filterBannedPhotosFunc = { it },
        cachePhotosFunc = { true }
      )

      assertTrue(deleteOldFuncCalled)
      assertEquals(4, page.page.size)

      val photos = page.page

      assertEquals("222", photos[2].photoName)
      assertEquals("333", photos[3].photoName)
      assertEquals("444", photos[0].photoName)
      assertEquals("555", photos[1].photoName)

      assertFalse(page.isEnd)
    }
  }

  @Test
  fun `should clear cache completely and then get page of photos from the server when fresh photos amount is greater than requested photos count`() {
    runBlocking {
      var clearCacheFuncCalled = false

      val page = pagedApiUtils.getPageOfPhotos<GalleryPhoto>(
        "test",
        1L,
        currentTime,
        2,
        "234",
        getPhotosFromCacheFunc = { _, _ -> photosFromCache },
        getFreshPhotosFunc = { freshPhotosFromServer },
        getPageOfPhotosFunc = { _, _, _ -> photosFromServer },
        clearCacheFunc = { clearCacheFuncCalled = true },
        deleteOldFunc = { assertNotCalled() },
        filterBannedPhotosFunc = { it },
        cachePhotosFunc = { true }
      )

      assertTrue(clearCacheFuncCalled)

      assertEquals(2, page.page.size)
      val photos = page.page

      assertEquals("222", photos[0].photoName)
      assertEquals("333", photos[1].photoName)

      assertFalse(page.isEnd)
    }
  }
}