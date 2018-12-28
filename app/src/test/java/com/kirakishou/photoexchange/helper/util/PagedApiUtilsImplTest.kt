package com.kirakishou.photoexchange.helper.util

import com.kirakishou.photoexchange.helper.LonLat
import com.kirakishou.photoexchange.mvp.model.photo.GalleryPhoto
import com.kirakishou.photoexchange.mvp.model.photo.PhotoAdditionalInfo
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.whenever
import junit.framework.Assert.*
import kotlinx.coroutines.runBlocking
import net.response.data.GalleryPhotoResponseData
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito

class PagedApiUtilsImplTest {

  private lateinit var timeUtils: TimeUtils
  private lateinit var netUtils: NetUtils
  private lateinit var pagedApiUtils: PagedApiUtils

  private val currentTime = 1000L

  @Before
  fun setUp() {
    timeUtils = Mockito.mock(TimeUtils::class.java)
    netUtils = Mockito.mock(NetUtils::class.java)

    pagedApiUtils = PagedApiUtilsImpl(
      timeUtils, netUtils
    )
  }

  @Test
  fun `should return photos from cache when there is no internet and network access level is less than canAccessNetwork`() {
    runBlocking {
      val photos = listOf(
        GalleryPhoto(
          "123",
          LonLat.empty(),
          21212L,
          PhotoAdditionalInfo.empty("123")
        )
      )

      val getFreshPhotosFunc = mock<suspend (Long) -> Int>()
      val getPageOfPhotosFunc = mock<suspend (String?, Long, Int) -> List<GalleryPhotoResponseData>>()
      val clearCacheFunc = mock<suspend () -> Unit>()
      val deleteOldFunc = mock<suspend () -> Unit>()
      val mapperFunc = mock<suspend (List<GalleryPhotoResponseData>) -> List<GalleryPhoto>>()
      val filterBannedPhotosFunc = mock<suspend (List<GalleryPhoto>) -> List<GalleryPhoto>>()
      val cachePhotosFunc = mock<suspend (List<GalleryPhoto>) -> Boolean>()

      whenever(netUtils.canAccessNetwork()).thenReturn(false)

      val page = pagedApiUtils.getPageOfPhotos(
        "test",
        -1L,
        currentTime,
        5,
        "234",
        getFreshPhotosFunc,
        { _, _ -> photos },
        getPageOfPhotosFunc,
        clearCacheFunc,
        deleteOldFunc,
        mapperFunc,
        filterBannedPhotosFunc,
        cachePhotosFunc
      )

      assertEquals(1, page.page.size)
      assertEquals("123", page.page.first().photoName)

      assertTrue(page.isEnd)
    }
  }

  @Test
  fun `should return photos from cache when it's first run and there are enough photos in the cache`() {
    runBlocking {
      val photos = listOf(
        GalleryPhoto(
          "123",
          LonLat.empty(),
          21212L,
          PhotoAdditionalInfo.empty("123")
        )
      )

      val getFreshPhotosFunc = mock<suspend (Long) -> Int>()
      val getPageOfPhotosFunc = mock<suspend (String?, Long, Int) -> List<GalleryPhotoResponseData>>()
      val clearCacheFunc = mock<suspend () -> Unit>()
      val deleteOldFunc = mock<suspend () -> Unit>()
      val mapperFunc = mock<suspend (List<GalleryPhotoResponseData>) -> List<GalleryPhoto>>()
      val filterBannedPhotosFunc = mock<suspend (List<GalleryPhoto>) -> List<GalleryPhoto>>()
      val cachePhotosFunc = mock<suspend (List<GalleryPhoto>) -> Boolean>()

      Mockito.`when`(netUtils.canAccessNetwork()).thenReturn(true)

      val page = pagedApiUtils.getPageOfPhotos(
        "test",
        -1L,
        currentTime,
        1,
        "234",
        getFreshPhotosFunc,
        { _, _ -> photos },
        getPageOfPhotosFunc,
        clearCacheFunc,
        deleteOldFunc,
        mapperFunc,
        filterBannedPhotosFunc,
        cachePhotosFunc
      )

      assertEquals(1, page.page.size)
      assertEquals("123", page.page.first().photoName)

      assertFalse(page.isEnd)
    }
  }
}