package com.kirakishou.photoexchange.helper.database.source.local

import android.content.Context
import androidx.room.Room
import androidx.test.InstrumentationRegistry
import androidx.test.runner.AndroidJUnit4
import com.kirakishou.photoexchange.helper.LonLat
import com.kirakishou.photoexchange.helper.database.MyDatabase
import com.kirakishou.photoexchange.helper.util.TimeUtils
import com.kirakishou.photoexchange.mvrx.model.photo.GalleryPhoto
import com.kirakishou.photoexchange.mvrx.model.photo.PhotoAdditionalInfo
import com.nhaarman.mockitokotlin2.whenever
import junit.framework.Assert.assertEquals
import junit.framework.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito

@RunWith(AndroidJUnit4::class)
class GalleryPhotoLocalSourceTest {
  lateinit var appContext: Context
  lateinit var targetContext: Context
  lateinit var database: MyDatabase
  lateinit var timeUtils: TimeUtils

  lateinit var galleryPhotoLocalSource: GalleryPhotoLocalSource

  @Before
  fun init() {
    appContext = InstrumentationRegistry.getContext()
    targetContext = InstrumentationRegistry.getTargetContext()
    database = Room.inMemoryDatabaseBuilder(appContext, MyDatabase::class.java).build()
    timeUtils = Mockito.spy(TimeUtils::class.java)

    galleryPhotoLocalSource = GalleryPhotoLocalSource(
      database,
      timeUtils,
      100
    )
  }

  @Test
  fun test_should_return_all_photos_when_there_are_no_photos_with_insertedOn_greater_than_current_time_minus_insertedEarlierThanTimeDelta() {
    whenever(timeUtils.getTimeFast()).thenReturn(200, 201, 202, 203, 204, 255)
    whenever(timeUtils.getTimePlus26Hours()).thenReturn(300)

    galleryPhotoLocalSource.save(GalleryPhoto("123", LonLat(11.1, 22.2), 100, PhotoAdditionalInfo.empty("123")))
    galleryPhotoLocalSource.save(GalleryPhoto("124", LonLat(11.1, 22.2), 101, PhotoAdditionalInfo.empty("124")))
    galleryPhotoLocalSource.save(GalleryPhoto("125", LonLat(11.1, 22.2), 102, PhotoAdditionalInfo.empty("125")))
    galleryPhotoLocalSource.save(GalleryPhoto("126", LonLat(11.1, 22.2), 103, PhotoAdditionalInfo.empty("126")))
    galleryPhotoLocalSource.save(GalleryPhoto("127", LonLat(11.1, 22.2), 104, PhotoAdditionalInfo.empty("127")))

    val page = galleryPhotoLocalSource.getPage(null, 5)
    assertEquals(5, page.size)
  }

  @Test
  fun test_should_not_return_photos_with_insertedOn_greater_than_current_time_minus_insertedEarlierThanTimeDelta() {
    whenever(timeUtils.getTimeFast()).thenReturn(200, 201, 202, 203, 204, 303)
    whenever(timeUtils.getTimePlus26Hours()).thenReturn(300)

    galleryPhotoLocalSource.save(GalleryPhoto("123", LonLat(11.1, 22.2), 100, PhotoAdditionalInfo.empty("123")))
    galleryPhotoLocalSource.save(GalleryPhoto("124", LonLat(11.1, 22.2), 101, PhotoAdditionalInfo.empty("124")))
    galleryPhotoLocalSource.save(GalleryPhoto("125", LonLat(11.1, 22.2), 102, PhotoAdditionalInfo.empty("125")))
    galleryPhotoLocalSource.save(GalleryPhoto("126", LonLat(11.1, 22.2), 103, PhotoAdditionalInfo.empty("126")))
    galleryPhotoLocalSource.save(GalleryPhoto("127", LonLat(11.1, 22.2), 104, PhotoAdditionalInfo.empty("127")))

    val page = galleryPhotoLocalSource.getPage(null, 5)
    assertEquals(2, page.size)
  }

  @Test
  fun test_should_return_no_photos_when_all_photos_should_be_deleted() {
    whenever(timeUtils.getTimeFast()).thenReturn(200, 201, 202, 203, 204, 999)
    whenever(timeUtils.getTimePlus26Hours()).thenReturn(300)

    galleryPhotoLocalSource.save(GalleryPhoto("123", LonLat(11.1, 22.2), 100, PhotoAdditionalInfo.empty("123")))
    galleryPhotoLocalSource.save(GalleryPhoto("124", LonLat(11.1, 22.2), 101, PhotoAdditionalInfo.empty("124")))
    galleryPhotoLocalSource.save(GalleryPhoto("125", LonLat(11.1, 22.2), 102, PhotoAdditionalInfo.empty("125")))
    galleryPhotoLocalSource.save(GalleryPhoto("126", LonLat(11.1, 22.2), 103, PhotoAdditionalInfo.empty("126")))
    galleryPhotoLocalSource.save(GalleryPhoto("127", LonLat(11.1, 22.2), 104, PhotoAdditionalInfo.empty("127")))

    val page = galleryPhotoLocalSource.getPage(null, 5)
    assertTrue(page.isEmpty())
  }

  @Test
  fun test_should_return_no_photos_when_uploadedOn_is_less_than_current_time() {
    whenever(timeUtils.getTimeFast()).thenReturn(200, 201, 202, 203, 204, 999)
    whenever(timeUtils.getTimePlus26Hours()).thenReturn(100)

    galleryPhotoLocalSource.save(GalleryPhoto("123", LonLat(11.1, 22.2), 100, PhotoAdditionalInfo.empty("123")))
    galleryPhotoLocalSource.save(GalleryPhoto("124", LonLat(11.1, 22.2), 101, PhotoAdditionalInfo.empty("124")))
    galleryPhotoLocalSource.save(GalleryPhoto("125", LonLat(11.1, 22.2), 102, PhotoAdditionalInfo.empty("125")))
    galleryPhotoLocalSource.save(GalleryPhoto("126", LonLat(11.1, 22.2), 103, PhotoAdditionalInfo.empty("126")))
    galleryPhotoLocalSource.save(GalleryPhoto("127", LonLat(11.1, 22.2), 104, PhotoAdditionalInfo.empty("127")))

    val page = galleryPhotoLocalSource.getPage(null, 5)
    assertTrue(page.isEmpty())
  }
}