package com.kirakishou.photoexchange.mvp.viewmodel

import android.support.test.runner.AndroidJUnit4
import com.kirakishou.photoexchange.helper.Either
import com.kirakishou.photoexchange.mvp.model.net.response.GalleryPhotoIdsResponse
import com.kirakishou.photoexchange.mvp.model.net.response.GalleryPhotoInfoResponse
import com.kirakishou.photoexchange.mvp.model.net.response.GalleryPhotosResponse
import io.reactivex.Single
import org.junit.After
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito

@RunWith(AndroidJUnit4::class)
class PhotosActivityViewModelTest_GalleryPhotosFragment : AbstractPhotosActivityViewModelTest() {

    @Before
    override fun init() {
        super.init()
    }

    @After
    override fun tearDown() {
        super.tearDown()
    }

    /**
     * Gallery photos
     * */
    @Test
    fun test_load_page_of_gallery_photos_should_not_delete_cached_gallery_photos_and_concat_them_with_fresh_photos() {
        val lastId = Long.MAX_VALUE
        val userId = "123"
        val photosCount = 10

        val photoIds = listOf(1L, 2L, 3L, 4L, 5L)
        val receivedPhotos = listOf(
            GalleryPhotosResponse.GalleryPhotoResponseData(4L, "4", 1.1, 1.1, 11L, 0),
            GalleryPhotosResponse.GalleryPhotoResponseData(5L, "5", 1.1, 1.1, 11L, 0)
        )
        val galleryPhotos = listOf(
            GalleryPhotoInfoResponse.GalleryPhotosInfoData(1L, true, false),
            GalleryPhotoInfoResponse.GalleryPhotosInfoData(2L, false, true),
            GalleryPhotoInfoResponse.GalleryPhotosInfoData(3L, true, false),
            GalleryPhotoInfoResponse.GalleryPhotosInfoData(4L, false, true),
            GalleryPhotoInfoResponse.GalleryPhotosInfoData(5L, true, false)
        )

        Mockito.`when`(apiClient.getGalleryPhotoIds(lastId, photosCount))
            .thenReturn(Single.just(GalleryPhotoIdsResponse.success(photoIds)))
        Mockito.`when`(apiClient.getGalleryPhotos("4,5"))
            .thenReturn(Single.just(GalleryPhotosResponse.success(receivedPhotos)))
        Mockito.`when`(apiClient.getGalleryPhotoInfo(userId, "1,2,3,4,5"))
            .thenReturn(Single.just(GalleryPhotoInfoResponse.success(galleryPhotos)))

        Mockito.`when`(timeUtils.getTimeFast())
            .thenReturn(10L)
            .thenReturn(19L)

        galleryPhotoRepository.saveMany(listOf(
            GalleryPhotosResponse.GalleryPhotoResponseData(1, "111", 1.1, 1.1, 11L, 0),
            GalleryPhotosResponse.GalleryPhotoResponseData(2, "222", 1.1, 1.1, 11L, 0),
            GalleryPhotosResponse.GalleryPhotoResponseData(3, "333", 1.1, 1.1, 11L, 0)
        ))

        settingsRepository.saveUserId(userId)

        val values = viewModel.loadNextPageOfGalleryPhotos(lastId, photosCount, true)
            .test()
            .await()
            .values()

        Assert.assertEquals(true, values[0] is Either.Value)
        val photos = (values[0] as Either.Value).value

        Assert.assertEquals(5, photos.size)

        Assert.assertEquals(1L, photos[0].galleryPhotoId)
        Assert.assertEquals("111", photos[0].photoName)
        Assert.assertEquals(1, photos[0].galleryPhotoInfo!!.galleryPhotoId)

        Assert.assertEquals(2L, photos[1].galleryPhotoId)
        Assert.assertEquals("222", photos[1].photoName)
        Assert.assertEquals(2, photos[1].galleryPhotoInfo!!.galleryPhotoId)

        Assert.assertEquals(3L, photos[2].galleryPhotoId)
        Assert.assertEquals("333", photos[2].photoName)
        Assert.assertEquals(3, photos[2].galleryPhotoInfo!!.galleryPhotoId)

        Assert.assertEquals(4L, photos[3].galleryPhotoId)
        Assert.assertEquals("4", photos[3].photoName)
        Assert.assertEquals(4, photos[3].galleryPhotoInfo!!.galleryPhotoId)

        Assert.assertEquals(5L, photos[4].galleryPhotoId)
        Assert.assertEquals("5", photos[4].photoName)
        Assert.assertEquals(5, photos[4].galleryPhotoInfo!!.galleryPhotoId)
    }

    @Test
    fun test_load_page_of_gallery_photos_should_delete_cached_gallery_photos_and_concat_them_with_fresh_photos() {
        val lastId = Long.MAX_VALUE
        val userId = "123"
        val photosCount = 10

        val photoIds = listOf(1L, 2L, 3L, 4L, 5L)
        val galleryPhotos = listOf(
            GalleryPhotosResponse.GalleryPhotoResponseData(1L, "1", 1.1, 1.1, 11L, 0),
            GalleryPhotosResponse.GalleryPhotoResponseData(2L, "2", 1.1, 1.1, 11L, 0),
            GalleryPhotosResponse.GalleryPhotoResponseData(3L, "3", 1.1, 1.1, 11L, 0),
            GalleryPhotosResponse.GalleryPhotoResponseData(4L, "4", 1.1, 1.1, 11L, 0),
            GalleryPhotosResponse.GalleryPhotoResponseData(5L, "5", 1.1, 1.1, 11L, 0)
        )
        val galleryPhotosInfo = listOf(
            GalleryPhotoInfoResponse.GalleryPhotosInfoData(1L, true, false),
            GalleryPhotoInfoResponse.GalleryPhotosInfoData(2L, false, true),
            GalleryPhotoInfoResponse.GalleryPhotosInfoData(3L, true, false),
            GalleryPhotoInfoResponse.GalleryPhotosInfoData(4L, false, true),
            GalleryPhotoInfoResponse.GalleryPhotosInfoData(5L, true, false)
        )

        Mockito.`when`(apiClient.getGalleryPhotoIds(lastId, photosCount))
            .thenReturn(Single.just(GalleryPhotoIdsResponse.success(photoIds)))
        Mockito.`when`(apiClient.getGalleryPhotos("1,2,3,4,5"))
            .thenReturn(Single.just(GalleryPhotosResponse.success(galleryPhotos)))
        Mockito.`when`(apiClient.getGalleryPhotoInfo(userId, "1,2,3,4,5"))
            .thenReturn(Single.just(GalleryPhotoInfoResponse.success(galleryPhotosInfo)))

        Mockito.`when`(timeUtils.getTimeFast())
            .thenReturn(10L)
            .thenReturn(30L)

        galleryPhotoRepository.saveMany(listOf(
            GalleryPhotosResponse.GalleryPhotoResponseData(1, "111", 1.1, 1.1, 11L, 0),
            GalleryPhotosResponse.GalleryPhotoResponseData(2, "222", 1.1, 1.1, 11L, 0),
            GalleryPhotosResponse.GalleryPhotoResponseData(3, "333", 1.1, 1.1, 11L, 0)
        ))

        settingsRepository.saveUserId(userId)

        val values = viewModel.loadNextPageOfGalleryPhotos(lastId, photosCount, true)
            .test()
            .await()
            .values()

        Assert.assertEquals(true, values[0] is Either.Value)
        val photos = (values[0] as Either.Value).value

        Assert.assertEquals(5, photos.size)

        Assert.assertEquals(1L, photos[0].galleryPhotoId)
        Assert.assertEquals("1", photos[0].photoName)
        Assert.assertEquals(1, photos[0].galleryPhotoInfo!!.galleryPhotoId)

        Assert.assertEquals(2L, photos[1].galleryPhotoId)
        Assert.assertEquals("2", photos[1].photoName)
        Assert.assertEquals(2, photos[1].galleryPhotoInfo!!.galleryPhotoId)

        Assert.assertEquals(3L, photos[2].galleryPhotoId)
        Assert.assertEquals("3", photos[2].photoName)
        Assert.assertEquals(3, photos[2].galleryPhotoInfo!!.galleryPhotoId)

        Assert.assertEquals(4L, photos[3].galleryPhotoId)
        Assert.assertEquals("4", photos[3].photoName)
        Assert.assertEquals(4, photos[3].galleryPhotoInfo!!.galleryPhotoId)

        Assert.assertEquals(5L, photos[4].galleryPhotoId)
        Assert.assertEquals("5", photos[4].photoName)
        Assert.assertEquals(5, photos[4].galleryPhotoInfo!!.galleryPhotoId)
    }

    @Test
    fun test_should_clear_old_gallery_photo_id_cache_and_cache_new_ones() {
        val lastId = Long.MAX_VALUE
        val userId = "123"
        val photosCount = 10

        val photoIds = listOf(1L, 2L, 3L, 4L, 5L)
        val galleryPhotos = listOf(
            GalleryPhotosResponse.GalleryPhotoResponseData(1L, "1", 1.1, 1.1, 11L, 0),
            GalleryPhotosResponse.GalleryPhotoResponseData(2L, "2", 1.1, 1.1, 11L, 0),
            GalleryPhotosResponse.GalleryPhotoResponseData(3L, "3", 1.1, 1.1, 11L, 0),
            GalleryPhotosResponse.GalleryPhotoResponseData(4L, "4", 1.1, 1.1, 11L, 0),
            GalleryPhotosResponse.GalleryPhotoResponseData(5L, "5", 1.1, 1.1, 11L, 0)
        )
        val galleryPhotosInfo = listOf(
            GalleryPhotoInfoResponse.GalleryPhotosInfoData(1L, true, false),
            GalleryPhotoInfoResponse.GalleryPhotosInfoData(2L, false, true),
            GalleryPhotoInfoResponse.GalleryPhotosInfoData(3L, true, false),
            GalleryPhotoInfoResponse.GalleryPhotosInfoData(4L, false, true),
            GalleryPhotoInfoResponse.GalleryPhotosInfoData(5L, true, false)
        )

        Mockito.`when`(apiClient.getGalleryPhotoIds(lastId, photosCount))
            .thenReturn(Single.just(GalleryPhotoIdsResponse.success(photoIds)))
        Mockito.`when`(apiClient.getGalleryPhotos("1,2,3,4,5"))
            .thenReturn(Single.just(GalleryPhotosResponse.success(galleryPhotos)))
        Mockito.`when`(apiClient.getGalleryPhotoInfo(userId, "1,2,3,4,5"))
            .thenReturn(Single.just(GalleryPhotoInfoResponse.success(galleryPhotosInfo)))

        Mockito.`when`(timeUtils.getTimeFast())
            .thenReturn(10L)
            .thenReturn(30L)

        cachedPhotoIdRepository.insert(1L, CachedPhotoIdEntity.PhotoType.UploadedPhoto)
        cachedPhotoIdRepository.insert(1L, CachedPhotoIdEntity.PhotoType.ReceivedPhoto)
        cachedPhotoIdRepository.insert(1L, CachedPhotoIdEntity.PhotoType.GalleryPhoto)

        settingsRepository.saveUserId(userId)

        val values = viewModel.loadNextPageOfGalleryPhotos(lastId, photosCount, true)
            .test()
            .await()
            .values()

        Assert.assertEquals(true, values[0] is Either.Value)
        val photos = (values[0] as Either.Value).value

        Assert.assertEquals(5, photos.size)

        Assert.assertEquals(1L, photos[0].galleryPhotoId)
        Assert.assertEquals("1", photos[0].photoName)
        Assert.assertEquals(1, photos[0].galleryPhotoInfo!!.galleryPhotoId)

        Assert.assertEquals(2L, photos[1].galleryPhotoId)
        Assert.assertEquals("2", photos[1].photoName)
        Assert.assertEquals(2, photos[1].galleryPhotoInfo!!.galleryPhotoId)

        Assert.assertEquals(3L, photos[2].galleryPhotoId)
        Assert.assertEquals("3", photos[2].photoName)
        Assert.assertEquals(3, photos[2].galleryPhotoInfo!!.galleryPhotoId)

        Assert.assertEquals(4L, photos[3].galleryPhotoId)
        Assert.assertEquals("4", photos[3].photoName)
        Assert.assertEquals(4, photos[3].galleryPhotoInfo!!.galleryPhotoId)

        Assert.assertEquals(5L, photos[4].galleryPhotoId)
        Assert.assertEquals("5", photos[4].photoName)
        Assert.assertEquals(5, photos[4].galleryPhotoInfo!!.galleryPhotoId)

        val cachedPhotoIds = cachedPhotoIdRepository.findAll()
        Assert.assertEquals(7, cachedPhotoIds.size)
    }

    @Test
    fun test_should_get_fresh_gallery_photos_from_server_when_fragment_was_just_created() {
        val lastId = Long.MAX_VALUE
        val userId = "123"
        val photosCount = 10

        val photoIds = listOf(1L, 2L, 3L, 4L, 5L)
        val galleryPhotos = listOf(
            GalleryPhotosResponse.GalleryPhotoResponseData(1L, "1", 1.1, 1.1, 11L, 0),
            GalleryPhotosResponse.GalleryPhotoResponseData(2L, "2", 1.1, 1.1, 11L, 0),
            GalleryPhotosResponse.GalleryPhotoResponseData(3L, "3", 1.1, 1.1, 11L, 0),
            GalleryPhotosResponse.GalleryPhotoResponseData(4L, "4", 1.1, 1.1, 11L, 0),
            GalleryPhotosResponse.GalleryPhotoResponseData(5L, "5", 1.1, 1.1, 11L, 0)
        )
        val galleryPhotosInfo = listOf(
            GalleryPhotoInfoResponse.GalleryPhotosInfoData(1L, true, false),
            GalleryPhotoInfoResponse.GalleryPhotosInfoData(2L, false, true),
            GalleryPhotoInfoResponse.GalleryPhotosInfoData(3L, true, false),
            GalleryPhotoInfoResponse.GalleryPhotosInfoData(4L, false, true),
            GalleryPhotoInfoResponse.GalleryPhotosInfoData(5L, true, false)
        )

        Mockito.`when`(apiClient.getGalleryPhotoIds(lastId, photosCount))
            .thenReturn(Single.just(GalleryPhotoIdsResponse.success(photoIds)))
        Mockito.`when`(apiClient.getGalleryPhotos("1,2,3,4,5"))
            .thenReturn(Single.just(GalleryPhotosResponse.success(galleryPhotos)))
        Mockito.`when`(apiClient.getGalleryPhotoInfo(userId, "1,2,3,4,5"))
            .thenReturn(Single.just(GalleryPhotoInfoResponse.success(galleryPhotosInfo)))

        Mockito.`when`(timeUtils.getTimeFast())
            .thenReturn(10)
            .thenReturn(30L)

        galleryPhotoRepository.saveMany(listOf(
            GalleryPhotosResponse.GalleryPhotoResponseData(1, "111", 1.1, 1.1, 11L, 0),
            GalleryPhotosResponse.GalleryPhotoResponseData(2, "222", 1.1, 1.1, 11L, 0),
            GalleryPhotosResponse.GalleryPhotoResponseData(3, "333", 1.1, 1.1, 11L, 0)
        ))

        settingsRepository.saveUserId(userId)

        val values = viewModel.loadNextPageOfGalleryPhotos(lastId, photosCount, true)
            .test()
            .await()
            .values()

        Assert.assertEquals(true, values[0] is Either.Value)
        val photos = (values[0] as Either.Value).value

        Assert.assertEquals(5, photos.size)

        Assert.assertEquals(1L, photos[0].galleryPhotoId)
        Assert.assertEquals("1", photos[0].photoName)
        Assert.assertEquals(1, photos[0].galleryPhotoInfo!!.galleryPhotoId)

        Assert.assertEquals(2L, photos[1].galleryPhotoId)
        Assert.assertEquals("2", photos[1].photoName)
        Assert.assertEquals(2, photos[1].galleryPhotoInfo!!.galleryPhotoId)

        Assert.assertEquals(3L, photos[2].galleryPhotoId)
        Assert.assertEquals("3", photos[2].photoName)
        Assert.assertEquals(3, photos[2].galleryPhotoInfo!!.galleryPhotoId)

        Assert.assertEquals(4L, photos[3].galleryPhotoId)
        Assert.assertEquals("4", photos[3].photoName)
        Assert.assertEquals(4, photos[3].galleryPhotoInfo!!.galleryPhotoId)

        Assert.assertEquals(5L, photos[4].galleryPhotoId)
        Assert.assertEquals("5", photos[4].photoName)
        Assert.assertEquals(5, photos[4].galleryPhotoInfo!!.galleryPhotoId)
    }
}