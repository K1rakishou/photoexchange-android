package com.kirakishou.photoexchange.mvp.viewmodel

import android.support.test.runner.AndroidJUnit4
import com.kirakishou.photoexchange.helper.Either
import com.kirakishou.photoexchange.helper.database.entity.CachedPhotoIdEntity
import com.kirakishou.photoexchange.mvp.model.net.response.GetReceivedPhotoIdsResponse
import com.kirakishou.photoexchange.mvp.model.net.response.GetReceivedPhotosResponse
import com.kirakishou.photoexchange.mvp.model.other.Constants
import io.reactivex.Single
import junit.framework.Assert.assertEquals
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito

@RunWith(AndroidJUnit4::class)
class PhotosActivityViewModelTest_ReceivedPhotosFragment : AbstractPhotosActivityViewModelTest() {

    @Before
    override fun init() {
        super.init()
    }

    @After
    override fun tearDown() {
        super.tearDown()
    }


    /**
     * Received photos
     * */
    @Test
    fun test_load_page_of_received_photos_should_not_delete_cached_received_photos_and_concat_them_with_fresh_photos() {
        val lastId = Long.MAX_VALUE
        val userId = "123"
        val photosCount = 10

        val photoIds = listOf(1L, 2L, 3L, 4L, 5L)
        val receivedPhotos = listOf(
            GetReceivedPhotosResponse.ReceivedPhoto(4L, "4", "44", 1.1, 1.1),
            GetReceivedPhotosResponse.ReceivedPhoto(5L, "5", "55", 1.1, 1.1)
        )

        Mockito.`when`(apiClient.getReceivedPhotoIds(userId, lastId, photosCount))
            .thenReturn(Single.just(GetReceivedPhotoIdsResponse.success(photoIds)))
        Mockito.`when`(apiClient.getReceivedPhotos(userId, "4,5"))
            .thenReturn(Single.just(GetReceivedPhotosResponse.success(receivedPhotos)))

        Mockito.`when`(timeUtils.getTimeFast())
            .thenReturn(10L)
            .thenReturn(10L)
            .thenReturn(10L)
            .thenReturn(19L)

        receivedPhotosRepository.save(GetReceivedPhotosResponse.ReceivedPhoto(1L, "111", "11", 1.1, 1.1))
        receivedPhotosRepository.save(GetReceivedPhotosResponse.ReceivedPhoto(2L, "222", "22", 1.1, 1.1))
        receivedPhotosRepository.save(GetReceivedPhotosResponse.ReceivedPhoto(3L, "333", "33", 1.1, 1.1))

        settingsRepository.saveUserId(userId)

        val values = viewModel.loadNextPageOfReceivedPhotos(lastId, photosCount, true)
            .test()
            .await()
            .values()

        assertEquals(true, values[0] is Either.Value)
        val photos = (values[0] as Either.Value).value

        assertEquals(5, photos.size)

        assertEquals(1L, photos[0].photoId)
        assertEquals("111", photos[0].uploadedPhotoName)
        assertEquals(2L, photos[1].photoId)
        assertEquals("222", photos[1].uploadedPhotoName)
        assertEquals(3L, photos[2].photoId)
        assertEquals("333", photos[2].uploadedPhotoName)
        assertEquals(4L, photos[3].photoId)
        assertEquals("4", photos[3].uploadedPhotoName)
        assertEquals(5L, photos[4].photoId)
        assertEquals("5", photos[4].uploadedPhotoName)
    }

    @Test
    fun test_load_page_of_received_photos_should_delete_cached_received_photos_and_concat_them_with_fresh_photos() {
        val lastId = Long.MAX_VALUE
        val userId = "123"
        val photosCount = 10

        val photoIds = listOf(1L, 2L, 3L, 4L, 5L)
        val receivedPhotos = listOf(
            GetReceivedPhotosResponse.ReceivedPhoto(1L, "1", "11", 1.1, 1.1),
            GetReceivedPhotosResponse.ReceivedPhoto(2L, "2", "22", 1.1, 1.1),
            GetReceivedPhotosResponse.ReceivedPhoto(3L, "3", "33", 1.1, 1.1),
            GetReceivedPhotosResponse.ReceivedPhoto(4L, "4", "44", 1.1, 1.1),
            GetReceivedPhotosResponse.ReceivedPhoto(5L, "5", "55", 1.1, 1.1)
        )

        Mockito.`when`(apiClient.getReceivedPhotoIds(userId, lastId, photosCount))
            .thenReturn(Single.just(GetReceivedPhotoIdsResponse.success(photoIds)))
        Mockito.`when`(apiClient.getReceivedPhotos(userId, "1,2,3,4,5"))
            .thenReturn(Single.just(GetReceivedPhotosResponse.success(receivedPhotos)))

        Mockito.`when`(timeUtils.getTimeFast())
            .thenReturn(1L)
            .thenReturn(2L)
            .thenReturn(3L)
            .thenReturn(30L)

        receivedPhotosRepository.save(GetReceivedPhotosResponse
            .ReceivedPhoto(1L, "111", "11", 1.1, 1.1))
        receivedPhotosRepository.save(GetReceivedPhotosResponse
            .ReceivedPhoto(2L, "222", "22", 1.1, 1.1))
        receivedPhotosRepository.save(GetReceivedPhotosResponse
            .ReceivedPhoto(3L, "333", "33", 1.1, 1.1))

        settingsRepository.saveUserId(userId)

        val values = viewModel.loadNextPageOfReceivedPhotos(lastId, photosCount, true)
            .test()
            .await()
            .values()

        assertEquals(true, values[0] is Either.Value)
        val photos = (values[0] as Either.Value).value

        assertEquals(5, photos.size)

        assertEquals(1L, photos[0].photoId)
        assertEquals("1", photos[0].uploadedPhotoName)
        assertEquals(2L, photos[1].photoId)
        assertEquals("2", photos[1].uploadedPhotoName)
        assertEquals(3L, photos[2].photoId)
        assertEquals("3", photos[2].uploadedPhotoName)
        assertEquals(4L, photos[3].photoId)
        assertEquals("4", photos[3].uploadedPhotoName)
        assertEquals(5L, photos[4].photoId)
        assertEquals("5", photos[4].uploadedPhotoName)
    }

    @Test
    fun test_should_clear_old_received_photo_id_cache_and_cache_new_ones() {
        val lastId = Long.MAX_VALUE
        val userId = "123"
        val photosCount = 10

        val photoIds = listOf(1L, 2L, 3L, 4L, 5L)
        val receivedPhotos = listOf(
            GetReceivedPhotosResponse.ReceivedPhoto(1L, "1", "11", 1.1, 1.1),
            GetReceivedPhotosResponse.ReceivedPhoto(2L, "2", "22", 1.1, 1.1),
            GetReceivedPhotosResponse.ReceivedPhoto(3L, "3", "33", 1.1, 1.1),
            GetReceivedPhotosResponse.ReceivedPhoto(4L, "4", "44", 1.1, 1.1),
            GetReceivedPhotosResponse.ReceivedPhoto(5L, "5", "55", 1.1, 1.1)
        )

        Mockito.`when`(apiClient.getReceivedPhotoIds(userId, lastId, photosCount))
            .thenReturn(Single.just(GetReceivedPhotoIdsResponse.success(photoIds)))
        Mockito.`when`(apiClient.getReceivedPhotos(userId, photoIds.joinToString(Constants.PHOTOS_DELIMITER)))
            .thenReturn(Single.just(GetReceivedPhotosResponse.success(receivedPhotos)))

        Mockito.`when`(timeUtils.getTimeFast())
            .thenReturn(1L)
            .thenReturn(2L)
            .thenReturn(3L)
            .thenReturn(30L)

        cachedPhotoIdRepository.insert(1L, CachedPhotoIdEntity.PhotoType.UploadedPhoto)
        cachedPhotoIdRepository.insert(1L, CachedPhotoIdEntity.PhotoType.ReceivedPhoto)
        cachedPhotoIdRepository.insert(1L, CachedPhotoIdEntity.PhotoType.GalleryPhoto)

        settingsRepository.saveUserId(userId)

        val values = viewModel.loadNextPageOfReceivedPhotos(lastId, photosCount, true)
            .test()
            .await()
            .values()

        assertEquals(true, values[0] is Either.Value)
        val photos = (values[0] as Either.Value).value

        assertEquals(5, photos.size)
        assertEquals(1L, photos[0].photoId)
        assertEquals("1", photos[0].uploadedPhotoName)
        assertEquals(2L, photos[1].photoId)
        assertEquals("2", photos[1].uploadedPhotoName)
        assertEquals(3L, photos[2].photoId)
        assertEquals("3", photos[2].uploadedPhotoName)
        assertEquals(4L, photos[3].photoId)
        assertEquals("4", photos[3].uploadedPhotoName)
        assertEquals(5L, photos[4].photoId)
        assertEquals("5", photos[4].uploadedPhotoName)

        val cachedPhotoIds = cachedPhotoIdRepository.findAll()
        assertEquals(7, cachedPhotoIds.size)
    }

    @Test
    fun test_should_get_fresh_received_photos_from_server_when_fragment_was_just_created() {
        val lastId = Long.MAX_VALUE
        val userId = "123"
        val photosCount = 10

        val photoIds = listOf(1L, 2L, 3L, 4L, 5L)
        val receivedPhotos = listOf(
            GetReceivedPhotosResponse.ReceivedPhoto(1L, "1", "11", 1.1, 1.1),
            GetReceivedPhotosResponse.ReceivedPhoto(2L, "2", "22", 1.1, 1.1),
            GetReceivedPhotosResponse.ReceivedPhoto(3L, "3", "33", 1.1, 1.1),
            GetReceivedPhotosResponse.ReceivedPhoto(4L, "4", "44", 1.1, 1.1),
            GetReceivedPhotosResponse.ReceivedPhoto(5L, "5", "55", 1.1, 1.1)
        )

        Mockito.`when`(apiClient.getReceivedPhotoIds(userId, lastId, photosCount))
            .thenReturn(Single.just(GetReceivedPhotoIdsResponse.success(photoIds)))
        Mockito.`when`(apiClient.getReceivedPhotos(userId, photoIds.joinToString(Constants.PHOTOS_DELIMITER)))
            .thenReturn(Single.just(GetReceivedPhotosResponse.success(receivedPhotos)))

        Mockito.`when`(timeUtils.getTimeFast())
            .thenReturn(1L)
            .thenReturn(2L)
            .thenReturn(3L)
            .thenReturn(30L)

        settingsRepository.saveUserId(userId)

        val values = viewModel.loadNextPageOfReceivedPhotos(lastId, photosCount, true)
            .test()
            .await()
            .values()

        assertEquals(true, values[0] is Either.Value)
        val photos = (values[0] as Either.Value).value

        assertEquals(5, photos.size)
        assertEquals(1L, photos[0].photoId)
        assertEquals("1", photos[0].uploadedPhotoName)
        assertEquals(2L, photos[1].photoId)
        assertEquals("2", photos[1].uploadedPhotoName)
        assertEquals(3L, photos[2].photoId)
        assertEquals("3", photos[2].uploadedPhotoName)
        assertEquals(4L, photos[3].photoId)
        assertEquals("4", photos[3].uploadedPhotoName)
        assertEquals(5L, photos[4].photoId)
        assertEquals("5", photos[4].uploadedPhotoName)
    }

}