package com.kirakishou.photoexchange.mvp.viewmodel

import android.support.test.runner.AndroidJUnit4
import com.kirakishou.photoexchange.helper.Either
import com.kirakishou.photoexchange.helper.RxLifecycle
import com.kirakishou.photoexchange.helper.database.entity.CachedPhotoIdEntity
import com.kirakishou.photoexchange.helper.intercom.event.PhotosActivityEvent
import com.kirakishou.photoexchange.helper.intercom.event.UploadedPhotosFragmentEvent
import com.kirakishou.photoexchange.mvp.model.PhotoState
import com.kirakishou.photoexchange.mvp.model.net.response.GetUploadedPhotoIdsResponse
import com.kirakishou.photoexchange.mvp.model.net.response.GetUploadedPhotosResponse
import com.kirakishou.photoexchange.mvp.model.other.Constants
import io.reactivex.Single
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito

@RunWith(AndroidJUnit4::class)
class PhotosActivityViewModelTest_UploadedPhotosFragment : AbstractPhotosActivityViewModelTest() {

    @Before
    override fun init() {
        super.init()
    }

    @After
    override fun tearDown() {
        super.tearDown()
    }


    /**
     * Uploaded photos
     * */
    @Test
    fun test_load_page_of_uploaded_photos_should_not_delete_cached_uploaded_photos_and_concat_them_with_fresh_photos() {
        val lastId = Long.MAX_VALUE
        val userId = "123"
        val photosCount = 10

        val photoIds = listOf(1L, 2L, 3L, 4L, 5L)
        val uploadedPhotos = listOf(
            GetUploadedPhotosResponse.UploadedPhotoData(4L, "4", 1.1, 1.1, false, 1L),
            GetUploadedPhotosResponse.UploadedPhotoData(5L, "5", 1.1, 1.1, false, 1L)
        )

        Mockito.`when`(apiClient.getUploadedPhotoIds(userId, lastId, photosCount))
            .thenReturn(Single.just(GetUploadedPhotoIdsResponse.success(photoIds)))
        Mockito.`when`(apiClient.getUploadedPhotos(userId, "4,5"))
            .thenReturn(Single.just(GetUploadedPhotosResponse.success(uploadedPhotos)))

        Mockito.`when`(timeUtils.getTimeFast())
            .thenReturn(10L)
            .thenReturn(10L)
            .thenReturn(10L)
            .thenReturn(19L)

        uploadedPhotosRepository.save(1L, "123", 1.1, 2.2, 10L)
        uploadedPhotosRepository.save(2L, "456", 2.2, 3.3, 10L)
        uploadedPhotosRepository.save(3L, "789", 4.4, 5.6, 10L)

        settingsRepository.saveUserId(userId)

        val values = viewModel.loadNextPageOfUploadedPhotos(lastId, photosCount, true)
            .test()
            .await()
            .values()

        assertEquals(true, values[0] is Either.Value)
        val photos = (values[0] as Either.Value).value

        assertEquals(5, photos.size)

        assertEquals(1L, photos[0].photoId)
        assertEquals("123", photos[0].photoName)
        assertEquals(2L, photos[1].photoId)
        assertEquals("456", photos[1].photoName)
        assertEquals(3L, photos[2].photoId)
        assertEquals("789", photos[2].photoName)
        assertEquals(4L, photos[3].photoId)
        assertEquals("4", photos[3].photoName)
        assertEquals(5L, photos[4].photoId)
        assertEquals("5", photos[4].photoName)
    }

    @Test
    fun test_load_page_of_uploaded_photos_should_delete_cached_uploaded_photos_and_concat_them_with_fresh_photos() {
        val lastId = Long.MAX_VALUE
        val userId = "123"
        val photosCount = 10

        val photoIds = listOf(1L, 2L, 3L, 4L, 5L)
        val uploadedPhotos = listOf(
            GetUploadedPhotosResponse.UploadedPhotoData(1L, "1", 1.1, 1.1, false, 1L),
            GetUploadedPhotosResponse.UploadedPhotoData(2L, "2", 1.1, 1.1, false, 1L),
            GetUploadedPhotosResponse.UploadedPhotoData(3L, "3", 1.1, 1.1, false, 1L),
            GetUploadedPhotosResponse.UploadedPhotoData(4L, "4", 1.1, 1.1, false, 1L),
            GetUploadedPhotosResponse.UploadedPhotoData(5L, "5", 1.1, 1.1, false, 1L)
        )

        Mockito.`when`(apiClient.getUploadedPhotoIds(userId, lastId, photosCount))
            .thenReturn(Single.just(GetUploadedPhotoIdsResponse.success(photoIds)))
        Mockito.`when`(apiClient.getUploadedPhotos(userId, "1,2,3,4,5"))
            .thenReturn(Single.just(GetUploadedPhotosResponse.success(uploadedPhotos)))

        Mockito.`when`(timeUtils.getTimeFast())
            .thenReturn(1L)
            .thenReturn(2L)
            .thenReturn(3L)
            .thenReturn(30L)

        uploadedPhotosRepository.save(1L, "123", 1.1, 2.2, 10L)
        uploadedPhotosRepository.save(2L, "456", 2.2, 3.3, 10L)
        uploadedPhotosRepository.save(3L, "789", 4.4, 5.6, 10L)

        settingsRepository.saveUserId(userId)

        val values = viewModel.loadNextPageOfUploadedPhotos(lastId, photosCount, true)
            .test()
            .await()
            .values()

        assertEquals(true, values[0] is Either.Value)
        val photos = (values[0] as Either.Value).value

        assertEquals(5, photos.size)

        assertEquals(1L, photos[0].photoId)
        assertEquals("1", photos[0].photoName)
        assertEquals(2L, photos[1].photoId)
        assertEquals("2", photos[1].photoName)
        assertEquals(3L, photos[2].photoId)
        assertEquals("3", photos[2].photoName)
        assertEquals(4L, photos[3].photoId)
        assertEquals("4", photos[3].photoName)
        assertEquals(5L, photos[4].photoId)
        assertEquals("5", photos[4].photoName)
    }

    @Test
    fun test_should_clear_old_uploaded_photo_id_cache_and_cache_new_ones() {
        val lastId = Long.MAX_VALUE
        val userId = "123"
        val photosCount = 10

        val photoIds = listOf(1L, 2L, 3L, 4L, 5L)
        val uploadedPhotos = listOf(
            GetUploadedPhotosResponse.UploadedPhotoData(1L, "1", 1.1, 1.1, false, 1L),
            GetUploadedPhotosResponse.UploadedPhotoData(2L, "2", 1.1, 1.1, false, 1L),
            GetUploadedPhotosResponse.UploadedPhotoData(3L, "3", 1.1, 1.1, false, 1L),
            GetUploadedPhotosResponse.UploadedPhotoData(4L, "4", 1.1, 1.1, false, 1L),
            GetUploadedPhotosResponse.UploadedPhotoData(5L, "5", 1.1, 1.1, false, 1L)
        )

        Mockito.`when`(apiClient.getUploadedPhotoIds(userId, lastId, photosCount))
            .thenReturn(Single.just(GetUploadedPhotoIdsResponse.success(photoIds)))
        Mockito.`when`(apiClient.getUploadedPhotos(userId, photoIds.joinToString(Constants.PHOTOS_DELIMITER)))
            .thenReturn(Single.just(GetUploadedPhotosResponse.success(uploadedPhotos)))

        Mockito.`when`(timeUtils.getTimeFast())
            .thenReturn(1L)
            .thenReturn(2L)
            .thenReturn(3L)
            .thenReturn(30L)

        cachedPhotoIdRepository.insert(1L, CachedPhotoIdEntity.PhotoType.UploadedPhoto)
        cachedPhotoIdRepository.insert(1L, CachedPhotoIdEntity.PhotoType.ReceivedPhoto)
        cachedPhotoIdRepository.insert(1L, CachedPhotoIdEntity.PhotoType.GalleryPhoto)

        settingsRepository.saveUserId(userId)

        val values = viewModel.loadNextPageOfUploadedPhotos(lastId, photosCount, true)
            .test()
            .await()
            .values()

        assertEquals(true, values[0] is Either.Value)
        val photos = (values[0] as Either.Value).value

        assertEquals(5, photos.size)
        assertEquals(1L, photos[0].photoId)
        assertEquals("1", photos[0].photoName)
        assertEquals(2L, photos[1].photoId)
        assertEquals("2", photos[1].photoName)
        assertEquals(3L, photos[2].photoId)
        assertEquals("3", photos[2].photoName)
        assertEquals(4L, photos[3].photoId)
        assertEquals("4", photos[3].photoName)
        assertEquals(5L, photos[4].photoId)
        assertEquals("5", photos[4].photoName)

        val cachedPhotoIds = cachedPhotoIdRepository.findAll()
        assertEquals(7, cachedPhotoIds.size)
    }

    @Test
    fun test_should_get_fresh_uploaded_photos_from_server_when_fragment_was_just_created() {
        val lastId = Long.MAX_VALUE
        val userId = "123"
        val photosCount = 10

        val photoIds = listOf(1L, 2L, 3L, 4L, 5L)
        val uploadedPhotos = listOf(
            GetUploadedPhotosResponse.UploadedPhotoData(1L, "1", 1.1, 1.1, false, 1L),
            GetUploadedPhotosResponse.UploadedPhotoData(2L, "2", 1.1, 1.1, false, 1L),
            GetUploadedPhotosResponse.UploadedPhotoData(3L, "3", 1.1, 1.1, false, 1L),
            GetUploadedPhotosResponse.UploadedPhotoData(4L, "4", 1.1, 1.1, false, 1L),
            GetUploadedPhotosResponse.UploadedPhotoData(5L, "5", 1.1, 1.1, false, 1L)
        )

        Mockito.`when`(apiClient.getUploadedPhotoIds(userId, lastId, photosCount))
            .thenReturn(Single.just(GetUploadedPhotoIdsResponse.success(photoIds)))
        Mockito.`when`(apiClient.getUploadedPhotos(userId, photoIds.joinToString(Constants.PHOTOS_DELIMITER)))
            .thenReturn(Single.just(GetUploadedPhotosResponse.success(uploadedPhotos)))

        Mockito.`when`(timeUtils.getTimeFast())
            .thenReturn(1L)
            .thenReturn(2L)
            .thenReturn(3L)
            .thenReturn(30L)

        settingsRepository.saveUserId(userId)

        val values = viewModel.loadNextPageOfUploadedPhotos(lastId, photosCount, true)
            .test()
            .await()
            .values()

        assertEquals(true, values[0] is Either.Value)
        val photos = (values[0] as Either.Value).value

        assertEquals(5, photos.size)
        assertEquals(1L, photos[0].photoId)
        assertEquals("1", photos[0].photoName)
        assertEquals(2L, photos[1].photoId)
        assertEquals("2", photos[1].photoName)
        assertEquals(3L, photos[2].photoId)
        assertEquals("3", photos[2].photoName)
        assertEquals(4L, photos[3].photoId)
        assertEquals("4", photos[3].photoName)
        assertEquals(5L, photos[4].photoId)
        assertEquals("5", photos[4].photoName)
    }

    @Test
    fun test_should_start_service_and_return_queued_up_photos_when_there_are_queued_up_photos_and_refreshing_is_manual() {
        takenPhotosRepository.saveTakenPhoto(tempFileRepository.create())
            .also { takenPhotosRepository.updatePhotoState(it.id, PhotoState.PHOTO_QUEUED_UP) }
        takenPhotosRepository.saveTakenPhoto(tempFileRepository.create())
            .also { takenPhotosRepository.updatePhotoState(it.id, PhotoState.PHOTO_QUEUED_UP) }
        takenPhotosRepository.saveTakenPhoto(tempFileRepository.create())
            .also { takenPhotosRepository.updatePhotoState(it.id, PhotoState.PHOTO_QUEUED_UP) }
        takenPhotosRepository.saveTakenPhoto(tempFileRepository.create())
            .also { takenPhotosRepository.updatePhotoState(it.id, PhotoState.PHOTO_QUEUED_UP) }

        val uploadedPhotosFragmentEventsTestObservable = viewModel.intercom.uploadedPhotosFragmentEvents.listen()
            .test()

        val photosActivityEventsTestObservable = viewModel.intercom.photosActivityEvents.listen()
            .test()

        val photosTypesTestObservable = viewModel.uploadedPhotosFragmentPhotosTypeToRefresh
            .test()

        viewModel.uploadedPhotosFragmentLifecycle.onNext(RxLifecycle.FragmentLifecycle.onResume)
        viewModel.uploadedPhotosFragmentLoadPhotosSubject.onNext(true)

        val uploadedPhotosFragmentEventsValues = uploadedPhotosFragmentEventsTestObservable
            .assertNoErrors()
            .assertNoTimeout()
            .assertNotTerminated()
            .values()

        assertEquals(4, uploadedPhotosFragmentEventsValues.size)
        assertEquals(true, uploadedPhotosFragmentEventsValues[0] is UploadedPhotosFragmentEvent.GeneralEvents.StartRefreshing)
        assertEquals(true, uploadedPhotosFragmentEventsValues[1] is UploadedPhotosFragmentEvent.GeneralEvents.DisableEndlessScrolling)
        assertEquals(true, uploadedPhotosFragmentEventsValues[2] is UploadedPhotosFragmentEvent.GeneralEvents.StopRefreshing)
        assertEquals(true, uploadedPhotosFragmentEventsValues[3] is UploadedPhotosFragmentEvent.GeneralEvents.AddQueuedUpPhotos)

        val queuedUpPhotos = (uploadedPhotosFragmentEventsValues[3] as UploadedPhotosFragmentEvent.GeneralEvents.AddQueuedUpPhotos).photos

        assertEquals(4, queuedUpPhotos.size)
        assertEquals(1L, queuedUpPhotos[0].id)
        assertEquals(2L, queuedUpPhotos[1].id)
        assertEquals(3L, queuedUpPhotos[2].id)
        assertEquals(4L, queuedUpPhotos[3].id)

        val photosActivityEventsValues = photosActivityEventsTestObservable
            .assertNoErrors()
            .assertNoTimeout()
            .assertNotTerminated()
            .values()

        assertEquals(1, photosActivityEventsValues.size)
        assertEquals(true, photosActivityEventsValues[0] is PhotosActivityEvent.StartUploadingService)

        val photosTypesValues = photosTypesTestObservable
            .assertNoErrors()
            .assertNoTimeout()
            .assertNotTerminated()
            .values()

        assertEquals(1, photosTypesValues.size)
        assertEquals(true, photosTypesValues[0] == PhotosActivityViewModel.PhotosToRefresh.QueuedUp)
    }

    @Test
    fun test_should_not_start_service_but_should_return_queued_up_photos_when_there_are_queued_up_photos_and_refreshing_is_not_manual() {
        takenPhotosRepository.saveTakenPhoto(tempFileRepository.create())
            .also { takenPhotosRepository.updatePhotoState(it.id, PhotoState.PHOTO_QUEUED_UP) }
        takenPhotosRepository.saveTakenPhoto(tempFileRepository.create())
            .also { takenPhotosRepository.updatePhotoState(it.id, PhotoState.PHOTO_QUEUED_UP) }
        takenPhotosRepository.saveTakenPhoto(tempFileRepository.create())
            .also { takenPhotosRepository.updatePhotoState(it.id, PhotoState.PHOTO_QUEUED_UP) }
        takenPhotosRepository.saveTakenPhoto(tempFileRepository.create())
            .also { takenPhotosRepository.updatePhotoState(it.id, PhotoState.PHOTO_QUEUED_UP) }

        val uploadedPhotosFragmentEventsTestObservable = viewModel.intercom.uploadedPhotosFragmentEvents.listen()
            .test()

        val photosActivityEventsTestObservable = viewModel.intercom.photosActivityEvents.listen()
            .test()

        val photosTypesTestObservable = viewModel.uploadedPhotosFragmentPhotosTypeToRefresh
            .test()

        viewModel.uploadedPhotosFragmentLifecycle.onNext(RxLifecycle.FragmentLifecycle.onResume)
        viewModel.uploadedPhotosFragmentLoadPhotosSubject.onNext(false)

        val uploadedPhotosFragmentEventsValues = uploadedPhotosFragmentEventsTestObservable
            .assertNoErrors()
            .assertNoTimeout()
            .assertNotTerminated()
            .values()

        assertEquals(4, uploadedPhotosFragmentEventsValues.size)
        assertEquals(true, uploadedPhotosFragmentEventsValues[0] is UploadedPhotosFragmentEvent.GeneralEvents.ShowProgressFooter)
        assertEquals(true, uploadedPhotosFragmentEventsValues[1] is UploadedPhotosFragmentEvent.GeneralEvents.DisableEndlessScrolling)
        assertEquals(true, uploadedPhotosFragmentEventsValues[2] is UploadedPhotosFragmentEvent.GeneralEvents.HideProgressFooter)
        assertEquals(true, uploadedPhotosFragmentEventsValues[3] is UploadedPhotosFragmentEvent.GeneralEvents.AddQueuedUpPhotos)

        val queuedUpPhotos = (uploadedPhotosFragmentEventsValues[3] as UploadedPhotosFragmentEvent.GeneralEvents.AddQueuedUpPhotos).photos

        assertEquals(4, queuedUpPhotos.size)
        assertEquals(1L, queuedUpPhotos[0].id)
        assertEquals(2L, queuedUpPhotos[1].id)
        assertEquals(3L, queuedUpPhotos[2].id)
        assertEquals(4L, queuedUpPhotos[3].id)

        photosActivityEventsTestObservable
            .assertNoErrors()
            .assertNoTimeout()
            .assertNotTerminated()
            .assertNoValues()

        val photosTypesValues = photosTypesTestObservable
            .assertNoErrors()
            .assertNoTimeout()
            .assertNotTerminated()
            .values()

        assertEquals(1, photosTypesValues.size)
        assertEquals(true, photosTypesValues[0] == PhotosActivityViewModel.PhotosToRefresh.QueuedUp)
    }

    @Test
    fun test_should_start_service_and_return_queued_up_photos_when_there_are_queued_up_photos_and_failed_to_upload_photos_and_refreshing_is_manual() {
        takenPhotosRepository.saveTakenPhoto(tempFileRepository.create())
            .also { takenPhotosRepository.updatePhotoState(it.id, PhotoState.PHOTO_QUEUED_UP) }
        takenPhotosRepository.saveTakenPhoto(tempFileRepository.create())
            .also { takenPhotosRepository.updatePhotoState(it.id, PhotoState.PHOTO_QUEUED_UP) }
        takenPhotosRepository.saveTakenPhoto(tempFileRepository.create())
            .also { takenPhotosRepository.updatePhotoState(it.id, PhotoState.PHOTO_QUEUED_UP) }
        takenPhotosRepository.saveTakenPhoto(tempFileRepository.create())
            .also { takenPhotosRepository.updatePhotoState(it.id, PhotoState.PHOTO_QUEUED_UP) }
        takenPhotosRepository.saveTakenPhoto(tempFileRepository.create())
            .also { takenPhotosRepository.updatePhotoState(it.id, PhotoState.FAILED_TO_UPLOAD) }
        takenPhotosRepository.saveTakenPhoto(tempFileRepository.create())
            .also { takenPhotosRepository.updatePhotoState(it.id, PhotoState.FAILED_TO_UPLOAD) }

        val uploadedPhotosFragmentEventsTestObservable = viewModel.intercom.uploadedPhotosFragmentEvents.listen()
            .test()

        val photosActivityEventsTestObservable = viewModel.intercom.photosActivityEvents.listen()
            .test()

        val photosTypesTestObservable = viewModel.uploadedPhotosFragmentPhotosTypeToRefresh
            .test()

        viewModel.uploadedPhotosFragmentLifecycle.onNext(RxLifecycle.FragmentLifecycle.onResume)
        viewModel.uploadedPhotosFragmentLoadPhotosSubject.onNext(true)

        val uploadedPhotosFragmentEventsValues = uploadedPhotosFragmentEventsTestObservable
            .assertNoErrors()
            .assertNoTimeout()
            .assertNotTerminated()
            .values()

        assertEquals(4, uploadedPhotosFragmentEventsValues.size)
        assertEquals(true, uploadedPhotosFragmentEventsValues[0] is UploadedPhotosFragmentEvent.GeneralEvents.StartRefreshing)
        assertEquals(true, uploadedPhotosFragmentEventsValues[1] is UploadedPhotosFragmentEvent.GeneralEvents.DisableEndlessScrolling)
        assertEquals(true, uploadedPhotosFragmentEventsValues[2] is UploadedPhotosFragmentEvent.GeneralEvents.StopRefreshing)
        assertEquals(true, uploadedPhotosFragmentEventsValues[3] is UploadedPhotosFragmentEvent.GeneralEvents.AddQueuedUpPhotos)

        val queuedUpPhotos = (uploadedPhotosFragmentEventsValues[3] as UploadedPhotosFragmentEvent.GeneralEvents.AddQueuedUpPhotos).photos

        assertEquals(4, queuedUpPhotos.size)
        assertEquals(1L, queuedUpPhotos[0].id)
        assertEquals(2L, queuedUpPhotos[1].id)
        assertEquals(3L, queuedUpPhotos[2].id)
        assertEquals(4L, queuedUpPhotos[3].id)

        val photosActivityEventsValues = photosActivityEventsTestObservable
            .assertNoErrors()
            .assertNoTimeout()
            .assertNotTerminated()
            .values()

        assertEquals(1, photosActivityEventsValues.size)
        assertEquals(true, photosActivityEventsValues[0] is PhotosActivityEvent.StartUploadingService)

        val photosTypesValues = photosTypesTestObservable
            .assertNoErrors()
            .assertNoTimeout()
            .assertNotTerminated()
            .values()

        assertEquals(1, photosTypesValues.size)
        assertEquals(true, photosTypesValues[0] == PhotosActivityViewModel.PhotosToRefresh.QueuedUp)
    }

    @Test
    fun test_should_not_start_service_but_return_queued_up_photos_when_there_are_queued_up_photos_and_failed_to_upload_photos_and_refreshing_is_not_manual() {
        takenPhotosRepository.saveTakenPhoto(tempFileRepository.create())
            .also { takenPhotosRepository.updatePhotoState(it.id, PhotoState.PHOTO_QUEUED_UP) }
        takenPhotosRepository.saveTakenPhoto(tempFileRepository.create())
            .also { takenPhotosRepository.updatePhotoState(it.id, PhotoState.PHOTO_QUEUED_UP) }
        takenPhotosRepository.saveTakenPhoto(tempFileRepository.create())
            .also { takenPhotosRepository.updatePhotoState(it.id, PhotoState.PHOTO_QUEUED_UP) }
        takenPhotosRepository.saveTakenPhoto(tempFileRepository.create())
            .also { takenPhotosRepository.updatePhotoState(it.id, PhotoState.PHOTO_QUEUED_UP) }
        takenPhotosRepository.saveTakenPhoto(tempFileRepository.create())
            .also { takenPhotosRepository.updatePhotoState(it.id, PhotoState.FAILED_TO_UPLOAD) }
        takenPhotosRepository.saveTakenPhoto(tempFileRepository.create())
            .also { takenPhotosRepository.updatePhotoState(it.id, PhotoState.FAILED_TO_UPLOAD) }

        val uploadedPhotosFragmentEventsTestObservable = viewModel.intercom.uploadedPhotosFragmentEvents.listen()
            .test()

        val photosActivityEventsTestObservable = viewModel.intercom.photosActivityEvents.listen()
            .test()

        val photosTypesTestObservable = viewModel.uploadedPhotosFragmentPhotosTypeToRefresh
            .test()

        viewModel.uploadedPhotosFragmentLifecycle.onNext(RxLifecycle.FragmentLifecycle.onResume)
        viewModel.uploadedPhotosFragmentLoadPhotosSubject.onNext(false)

        val uploadedPhotosFragmentEventsValues = uploadedPhotosFragmentEventsTestObservable
            .assertNoErrors()
            .assertNoTimeout()
            .assertNotTerminated()
            .values()

        assertEquals(4, uploadedPhotosFragmentEventsValues.size)
        assertEquals(true, uploadedPhotosFragmentEventsValues[0] is UploadedPhotosFragmentEvent.GeneralEvents.ShowProgressFooter)
        assertEquals(true, uploadedPhotosFragmentEventsValues[1] is UploadedPhotosFragmentEvent.GeneralEvents.DisableEndlessScrolling)
        assertEquals(true, uploadedPhotosFragmentEventsValues[2] is UploadedPhotosFragmentEvent.GeneralEvents.HideProgressFooter)
        assertEquals(true, uploadedPhotosFragmentEventsValues[3] is UploadedPhotosFragmentEvent.GeneralEvents.AddQueuedUpPhotos)

        val queuedUpPhotos = (uploadedPhotosFragmentEventsValues[3] as UploadedPhotosFragmentEvent.GeneralEvents.AddQueuedUpPhotos).photos

        assertEquals(4, queuedUpPhotos.size)
        assertEquals(1L, queuedUpPhotos[0].id)
        assertEquals(2L, queuedUpPhotos[1].id)
        assertEquals(3L, queuedUpPhotos[2].id)
        assertEquals(4L, queuedUpPhotos[3].id)

        photosActivityEventsTestObservable
            .assertNoErrors()
            .assertNoTimeout()
            .assertNotTerminated()
            .assertNoValues()

        val photosTypesValues = photosTypesTestObservable
            .assertNoErrors()
            .assertNoTimeout()
            .assertNotTerminated()
            .values()

        assertEquals(1, photosTypesValues.size)
        assertEquals(true, photosTypesValues[0] == PhotosActivityViewModel.PhotosToRefresh.QueuedUp)
    }

    @Test
    fun test_should_not_start_service_and_return_empty_list_when_there_are_no_queued_up_photos() {
        val uploadedPhotosFragmentEventsTestObservable = viewModel.intercom.uploadedPhotosFragmentEvents.listen()
            .test()

        val photosActivityEventsTestObservable = viewModel.intercom.photosActivityEvents.listen()
            .test()

        val photosTypesTestObservable = viewModel.uploadedPhotosFragmentPhotosTypeToRefresh
            .test()

        viewModel.uploadedPhotosFragmentLifecycle.onNext(RxLifecycle.FragmentLifecycle.onResume)
        viewModel.uploadedPhotosFragmentLoadPhotosSubject.onNext(false)
        viewModel.uploadedPhotosFragmentIsFreshlyCreated.onNext(true)

        val photosTypesValues = photosTypesTestObservable
            .assertNoErrors()
            .assertNoTimeout()
            .assertNotTerminated()
            .values()

        assertEquals(1, photosTypesValues.size)
        assertEquals(true, photosTypesValues[0] == PhotosActivityViewModel.PhotosToRefresh.Uploaded)

        val uploadedPhotosFragmentEventsValues = uploadedPhotosFragmentEventsTestObservable
            .assertNoErrors()
            .assertNoTimeout()
            .assertNotTerminated()
            .values()

        assertEquals(4, uploadedPhotosFragmentEventsValues.size)
        assertEquals(true, uploadedPhotosFragmentEventsValues[0] is UploadedPhotosFragmentEvent.GeneralEvents.EnableEndlessScrolling)
        assertEquals(true, uploadedPhotosFragmentEventsValues[1] is UploadedPhotosFragmentEvent.GeneralEvents.ShowProgressFooter)
        assertEquals(true, uploadedPhotosFragmentEventsValues[2] is UploadedPhotosFragmentEvent.GeneralEvents.HideProgressFooter)
        assertEquals(true, uploadedPhotosFragmentEventsValues[3] is UploadedPhotosFragmentEvent.GeneralEvents.AddUploadedPhotos)

        val queuedUpPhotos = (uploadedPhotosFragmentEventsValues[3] as UploadedPhotosFragmentEvent.GeneralEvents.AddUploadedPhotos).photos
        assertEquals(0, queuedUpPhotos.size)

        photosActivityEventsTestObservable
            .assertNoErrors()
            .assertNoTimeout()
            .assertNotTerminated()
            .assertNoValues()
    }

    @Test
    fun test_should_return_failed_to_upload_photos_when_there_are_no_queued_up_photos_and_there_are_failed_to_upload_photos() {
        takenPhotosRepository.saveTakenPhoto(tempFileRepository.create())
            .also { takenPhotosRepository.updatePhotoState(it.id, PhotoState.FAILED_TO_UPLOAD) }
        takenPhotosRepository.saveTakenPhoto(tempFileRepository.create())
            .also { takenPhotosRepository.updatePhotoState(it.id, PhotoState.FAILED_TO_UPLOAD) }
        takenPhotosRepository.saveTakenPhoto(tempFileRepository.create())
            .also { takenPhotosRepository.updatePhotoState(it.id, PhotoState.FAILED_TO_UPLOAD) }
        takenPhotosRepository.saveTakenPhoto(tempFileRepository.create())
            .also { takenPhotosRepository.updatePhotoState(it.id, PhotoState.FAILED_TO_UPLOAD) }

        val uploadedPhotosFragmentEventsTestObservable = viewModel.intercom.uploadedPhotosFragmentEvents.listen()
            .test()

        val photosActivityEventsTestObservable = viewModel.intercom.photosActivityEvents.listen()
            .test()

        val photosTypesTestObservable = viewModel.uploadedPhotosFragmentPhotosTypeToRefresh
            .test()

        viewModel.uploadedPhotosFragmentLifecycle.onNext(RxLifecycle.FragmentLifecycle.onResume)
        viewModel.uploadedPhotosFragmentLoadPhotosSubject.onNext(false)

        val photosTypesValues = photosTypesTestObservable
            .assertNoErrors()
            .assertNoTimeout()
            .assertNotTerminated()
            .values()

        assertEquals(1, photosTypesValues.size)
        assertEquals(true, photosTypesValues[0] == PhotosActivityViewModel.PhotosToRefresh.FailedToUpload)

        val uploadedPhotosFragmentEventsValues = uploadedPhotosFragmentEventsTestObservable
            .assertNoErrors()
            .assertNoTimeout()
            .assertNotTerminated()
            .values()

        assertEquals(4, uploadedPhotosFragmentEventsValues.size)
        assertEquals(true, uploadedPhotosFragmentEventsValues[0] is UploadedPhotosFragmentEvent.GeneralEvents.ShowProgressFooter)
        assertEquals(true, uploadedPhotosFragmentEventsValues[1] is UploadedPhotosFragmentEvent.GeneralEvents.DisableEndlessScrolling)
        assertEquals(true, uploadedPhotosFragmentEventsValues[2] is UploadedPhotosFragmentEvent.GeneralEvents.HideProgressFooter)
        assertEquals(true, uploadedPhotosFragmentEventsValues[3] is UploadedPhotosFragmentEvent.GeneralEvents.AddFailedToUploadPhotos)

        val queuedUpPhotos = (uploadedPhotosFragmentEventsValues[3] as UploadedPhotosFragmentEvent.GeneralEvents.AddFailedToUploadPhotos).photos

        assertEquals(4, queuedUpPhotos.size)
        assertEquals(1L, queuedUpPhotos[0].id)
        assertEquals(2L, queuedUpPhotos[1].id)
        assertEquals(3L, queuedUpPhotos[2].id)
        assertEquals(4L, queuedUpPhotos[3].id)

        photosActivityEventsTestObservable
            .assertNoErrors()
            .assertNoTimeout()
            .assertNotTerminated()
            .assertNoValues()
    }

    @Test
    fun test_should_refresh_queued_up_photos_when_there_are_queued_up_photos() {
        val uploadedPhotosFragmentLoadPhotosTestObservable = viewModel.uploadedPhotosFragmentLoadPhotosSubject
            .test()

        val uploadedPhotosFragmentEventsTestObservable = viewModel.intercom.uploadedPhotosFragmentEvents.listen()
            .test()

        viewModel.uploadedPhotosFragmentRefreshPhotos.onNext(Unit)
        viewModel.uploadedPhotosFragmentPhotosTypeToRefresh.onNext(PhotosActivityViewModel.PhotosToRefresh.QueuedUp)

        val uploadedPhotosFragmentLoadPhotosValues = uploadedPhotosFragmentLoadPhotosTestObservable
            .assertNoErrors()
            .assertNoTimeout()
            .assertNotTerminated()
            .values()

        assertEquals(1, uploadedPhotosFragmentLoadPhotosValues.size)
        assertEquals(true, uploadedPhotosFragmentLoadPhotosValues[0])

        val uploadedPhotosFragmentEventsValues = uploadedPhotosFragmentEventsTestObservable
            .assertNoErrors()
            .assertNoTimeout()
            .assertNotTerminated()
            .values()

        assertEquals(2, uploadedPhotosFragmentEventsValues.size)
        assertEquals(true, uploadedPhotosFragmentEventsValues[0] is UploadedPhotosFragmentEvent.GeneralEvents.DisableEndlessScrolling)
        assertEquals(true, uploadedPhotosFragmentEventsValues[1] is UploadedPhotosFragmentEvent.GeneralEvents.ClearAdapter)
    }

    @Test
    fun test_should_refresh_failed_to_upload_photos_when_there_are_failed_to_upload_photos() {
        val uploadedPhotosFragmentLoadPhotosTestObservable = viewModel.uploadedPhotosFragmentLoadPhotosSubject
            .test()

        val uploadedPhotosFragmentEventsTestObservable = viewModel.intercom.uploadedPhotosFragmentEvents.listen()
            .test()

        viewModel.uploadedPhotosFragmentRefreshPhotos.onNext(Unit)
        viewModel.uploadedPhotosFragmentPhotosTypeToRefresh.onNext(PhotosActivityViewModel.PhotosToRefresh.FailedToUpload)

        val uploadedPhotosFragmentLoadPhotosValues = uploadedPhotosFragmentLoadPhotosTestObservable
            .assertNoErrors()
            .assertNoTimeout()
            .assertNotTerminated()
            .values()

        assertEquals(1, uploadedPhotosFragmentLoadPhotosValues.size)
        assertEquals(true, uploadedPhotosFragmentLoadPhotosValues[0])

        val uploadedPhotosFragmentEventsValues = uploadedPhotosFragmentEventsTestObservable
            .assertNoErrors()
            .assertNoTimeout()
            .assertNotTerminated()
            .values()

        assertEquals(2, uploadedPhotosFragmentEventsValues.size)
        assertEquals(true, uploadedPhotosFragmentEventsValues[0] is UploadedPhotosFragmentEvent.GeneralEvents.DisableEndlessScrolling)
        assertEquals(true, uploadedPhotosFragmentEventsValues[1] is UploadedPhotosFragmentEvent.GeneralEvents.ClearAdapter)
    }

    @Test
    fun test_should_refresh_uploaded_photos_when_there_are_uploaded_photos() {
        val uploadedPhotosFragmentLoadPhotosTestObservable = viewModel.uploadedPhotosFragmentLoadPhotosSubject
            .test()

        val uploadedPhotosFragmentEventsTestObservable = viewModel.intercom.uploadedPhotosFragmentEvents.listen()
            .test()

        viewModel.uploadedPhotosFragmentRefreshPhotos.onNext(Unit)
        viewModel.uploadedPhotosFragmentPhotosTypeToRefresh.onNext(PhotosActivityViewModel.PhotosToRefresh.FailedToUpload)
        viewModel.uploadedPhotosFragmentIsFreshlyCreated.onNext(false)

        val uploadedPhotosFragmentLoadPhotosValues = uploadedPhotosFragmentLoadPhotosTestObservable
            .assertNoErrors()
            .assertNoTimeout()
            .assertNotTerminated()
            .values()

        assertEquals(1, uploadedPhotosFragmentLoadPhotosValues.size)
        assertEquals(true, uploadedPhotosFragmentLoadPhotosValues[0])

        val uploadedPhotosFragmentEventsValues = uploadedPhotosFragmentEventsTestObservable
            .assertNoErrors()
            .assertNoTimeout()
            .assertNotTerminated()
            .values()

        assertEquals(2, uploadedPhotosFragmentEventsValues.size)
        assertEquals(true, uploadedPhotosFragmentEventsValues[0] is UploadedPhotosFragmentEvent.GeneralEvents.DisableEndlessScrolling)
        assertEquals(true, uploadedPhotosFragmentEventsValues[1] is UploadedPhotosFragmentEvent.GeneralEvents.ClearAdapter)
    }
}




























