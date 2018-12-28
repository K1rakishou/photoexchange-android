package com.kirakishou.photoexchange.helper.util

import com.kirakishou.photoexchange.helper.LonLat
import com.kirakishou.photoexchange.helper.database.mapper.GalleryPhotosMapper
import com.kirakishou.photoexchange.helper.exception.ConnectionError
import com.kirakishou.photoexchange.mvp.model.photo.GalleryPhoto
import com.kirakishou.photoexchange.mvp.model.photo.PhotoAdditionalInfo
import com.nhaarman.mockitokotlin2.whenever
import junit.framework.Assert.*
import kotlinx.coroutines.runBlocking
import net.response.data.GalleryPhotoResponseData
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito
import java.lang.RuntimeException

class PagedApiUtilsImplTest {

  private lateinit var timeUtils: TimeUtils
  private lateinit var netUtils: NetUtils
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

  private val freshPhotos = listOf(
    GalleryPhotoResponseData(
      "222",
      11.1,
      22.2,
      400L
    ),
    GalleryPhotoResponseData(
      "333",
      12.1,
      23.2,
      401L
    )
  )

  @Before
  fun setUp() {
    timeUtils = Mockito.mock(TimeUtils::class.java)
    netUtils = Mockito.mock(NetUtils::class.java)

    pagedApiUtils = PagedApiUtilsImpl(
      timeUtils, netUtils
    )
  }
  
  private fun assertNotCalled(): Nothing = throw RuntimeException("Must not be called")

  @Test
  fun `should return photos from cache when there is no internet and network access level is less than canAccessNetwork`() {
    runBlocking {
      whenever(netUtils.canAccessNetwork()).thenReturn(false)

      val page = pagedApiUtils.getPageOfPhotos<GalleryPhoto, GalleryPhotoResponseData>(
        "test",
        -1L,
        currentTime,
        5,
        "234",
        getFreshPhotosCountFunc = { assertNotCalled() },
        getPhotosFromCacheFunc = { _, _ -> photosFromCache },
        getPageOfPhotosFunc = { _, _, _ -> assertNotCalled() },
        clearCacheFunc = { assertNotCalled() },
        deleteOldFunc = { assertNotCalled() },
        mapperFunc = { assertNotCalled() },
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
      var deleteOldFuncCalled = false
      Mockito.`when`(netUtils.canAccessNetwork()).thenReturn(true)

      val page = pagedApiUtils.getPageOfPhotos<GalleryPhoto, GalleryPhotoResponseData>(
        "test",
        -1L,
        currentTime,
        1,
        "234",
        getFreshPhotosCountFunc = { assertNotCalled() },
        getPhotosFromCacheFunc = { _, _ -> photosFromCache },
        getPageOfPhotosFunc = { _, _, _ -> assertNotCalled() },
        clearCacheFunc = { assertNotCalled() },
        deleteOldFunc = { deleteOldFuncCalled = true },
        mapperFunc = { assertNotCalled() },
        filterBannedPhotosFunc = { assertNotCalled() },
        cachePhotosFunc = { assertNotCalled() }
      )

      assertTrue(deleteOldFuncCalled)

      assertEquals(1, page.page.size)
      assertEquals("123", page.page.first().photoName)

      assertFalse(page.isEnd)
    }
  }

  @Test
  fun `should get fresh photos from server when it's first run and there are not enough photos in the cache`() {
    runBlocking {
      var deleteOldFuncCalled = false
      Mockito.`when`(netUtils.canAccessNetwork()).thenReturn(true)

      val page = pagedApiUtils.getPageOfPhotos(
        "test",
        -1L,
        currentTime,
        2,
        "234",
        getFreshPhotosCountFunc = { assertNotCalled() },
        getPhotosFromCacheFunc = { _, _ -> photosFromCache },
        getPageOfPhotosFunc = { _, _, _ -> freshPhotos },
        clearCacheFunc = { assertNotCalled() },
        deleteOldFunc = { deleteOldFuncCalled = true },
        mapperFunc = { GalleryPhotosMapper.FromResponse.ToObject.toGalleryPhotoList(it) },
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
      var deleteOldFuncCalled = false
      Mockito.`when`(netUtils.canAccessNetwork()).thenReturn(true)

      val page = pagedApiUtils.getPageOfPhotos<GalleryPhoto, GalleryPhotoResponseData>(
        "test",
        -1L,
        currentTime,
        2,
        "234",
        getFreshPhotosCountFunc = { assertNotCalled() },
        getPhotosFromCacheFunc = { _, _ -> photosFromCache },
        getPageOfPhotosFunc = { _, _, _ -> throw ConnectionError("BAM") },
        clearCacheFunc = { assertNotCalled() },
        deleteOldFunc = { deleteOldFuncCalled = true },
        mapperFunc = { GalleryPhotosMapper.FromResponse.ToObject.toGalleryPhotoList(it) },
        filterBannedPhotosFunc = { assertNotCalled() },
        cachePhotosFunc = { assertNotCalled() }
      )

      assertTrue(deleteOldFuncCalled)

      assertEquals(1, page.page.size)
      assertEquals("123", page.page.first().photoName)

      assertTrue(page.isEnd)
    }
  }

  @Test
  fun `should return photos from cache when attempt to get amount of fresh photos on the server resulted in connection exception`() {
    runBlocking {
      Mockito.`when`(netUtils.canAccessNetwork()).thenReturn(true)

      val page = pagedApiUtils.getPageOfPhotos<GalleryPhoto, GalleryPhotoResponseData>(
        "test",
        1L,
        currentTime,
        2,
        "234",
        getFreshPhotosCountFunc = { throw ConnectionError("BAM") },
        getPhotosFromCacheFunc = { _, _ -> photosFromCache },
        getPageOfPhotosFunc = { _, _, _ -> assertNotCalled() },
        clearCacheFunc = { assertNotCalled() },
        deleteOldFunc = { assertNotCalled() },
        mapperFunc = { GalleryPhotosMapper.FromResponse.ToObject.toGalleryPhotoList(it) },
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
      Mockito.`when`(netUtils.canAccessNetwork()).thenReturn(true)

      val page = pagedApiUtils.getPageOfPhotos<GalleryPhoto, GalleryPhotoResponseData>(
        "test",
        1L,
        currentTime,
        2,
        "234",
        getFreshPhotosCountFunc = { 0 },
        getPhotosFromCacheFunc = { _, _ -> photosFromCache },
        getPageOfPhotosFunc = { _, _, _ -> freshPhotos },
        clearCacheFunc = { assertNotCalled() },
        deleteOldFunc = { deleteOldFuncCalled = true },
        mapperFunc = { GalleryPhotosMapper.FromResponse.ToObject.toGalleryPhotoList(it) },
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
      var deleteOldFuncCalled = false
      Mockito.`when`(netUtils.canAccessNetwork()).thenReturn(true)

      val page = pagedApiUtils.getPageOfPhotos<GalleryPhoto, GalleryPhotoResponseData>(
        "test",
        1L,
        currentTime,
        2,
        "234",
        getFreshPhotosCountFunc = { 2 },
        getPhotosFromCacheFunc = { _, _ -> photosFromCache },
        getPageOfPhotosFunc = { _, _, _ -> throw ConnectionError("BAM") },
        clearCacheFunc = { assertNotCalled() },
        deleteOldFunc = { deleteOldFuncCalled = true },
        mapperFunc = { GalleryPhotosMapper.FromResponse.ToObject.toGalleryPhotoList(it) },
        filterBannedPhotosFunc = { assertNotCalled() },
        cachePhotosFunc = { assertNotCalled() }
      )

      assertTrue(deleteOldFuncCalled)

      assertEquals(1, page.page.size)
      assertEquals("123", page.page.first().photoName)

      assertTrue(page.isEnd)
    }
  }
}