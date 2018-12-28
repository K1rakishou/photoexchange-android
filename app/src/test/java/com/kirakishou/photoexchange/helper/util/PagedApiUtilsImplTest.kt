package com.kirakishou.photoexchange.helper.util

import com.kirakishou.photoexchange.helper.LonLat
import com.kirakishou.photoexchange.mvp.model.photo.GalleryPhoto
import com.kirakishou.photoexchange.mvp.model.photo.PhotoAdditionalInfo
import junit.framework.Assert.assertEquals
import junit.framework.Assert.assertTrue
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
  private val requestedCount = 5

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
      Mockito.`when`(netUtils.canAccessNetwork()).thenReturn(false)

      val page = pagedApiUtils.getPageOfPhotos<GalleryPhoto, GalleryPhotoResponseData>(
        "test",
        -1L,
        currentTime,
        requestedCount,
        "234",
        { 0 },
        { _, _ -> listOf(GalleryPhoto("123", LonLat.empty(), 21212L, PhotoAdditionalInfo.empty("123"))) },
        {_, _, _ -> listOf()},
        {},
        {},
        { listOf() },
        { listOf() },
        { true }
      )

      assertEquals(1, page.page.size)
      assertTrue(page.isEnd)
    }
  }
}