package com.kirakishou.photoexchange.mvrx.viewmodel

import com.airbnb.mvrx.Fail
import com.kirakishou.photoexchange.helper.concurrency.coroutines.MockDispatchers
import com.kirakishou.photoexchange.helper.database.repository.TakenPhotosRepository
import com.kirakishou.photoexchange.helper.database.repository.UploadedPhotosRepository
import com.kirakishou.photoexchange.helper.intercom.PhotosActivityViewModelIntercom
import com.kirakishou.photoexchange.helper.intercom.event.PhotosActivityEvent
import com.kirakishou.photoexchange.mvrx.model.PhotoState
import com.kirakishou.photoexchange.mvrx.model.photo.TakenPhoto
import com.kirakishou.photoexchange.mvrx.viewmodel.state.UploadedPhotosFragmentState
import com.kirakishou.photoexchange.ui.activity.PhotosActivity
import com.kirakishou.photoexchange.usecases.CancelPhotoUploadingUseCase
import com.kirakishou.photoexchange.usecases.GetFreshPhotosUseCase
import com.kirakishou.photoexchange.usecases.GetUploadedPhotosUseCase
import com.nhaarman.mockitokotlin2.*
import junit.framework.Assert.assertEquals
import junit.framework.Assert.assertTrue
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Test
import java.io.IOException

class UploadedPhotosFragmentViewModelTest {
  private val intercom: PhotosActivityViewModelIntercom = spy(PhotosActivityViewModelIntercom())
  private val takenPhotosRepository: TakenPhotosRepository = mock()
  private val uploadedPhotosRepository: UploadedPhotosRepository = mock()
  private val getUploadedPhotosUseCase: GetUploadedPhotosUseCase = mock()
  private val getFreshPhotosUseCase: GetFreshPhotosUseCase = mock()
  private val cancelPhotoUploadingUseCase: CancelPhotoUploadingUseCase = mock()

  lateinit var viewModel: UploadedPhotosFragmentViewModel

  @Before
  fun setUp() {
    viewModel = UploadedPhotosFragmentViewModel(
      UploadedPhotosFragmentState(),
      intercom,
      takenPhotosRepository,
      uploadedPhotosRepository,
      getUploadedPhotosUseCase,
      getFreshPhotosUseCase,
      cancelPhotoUploadingUseCase,
      MockDispatchers()
    )
  }

  @Test
  fun `reset state should set the default state`() {
    runBlocking {
      val state = UploadedPhotosFragmentState(
        takenPhotos = listOf(TakenPhoto(1L, true, "12121", null, PhotoState.PHOTO_QUEUED_UP))
      )
      val defaultState = UploadedPhotosFragmentState()

      viewModel.testSetState(state)
      assertEquals(state, viewModel.testGetState())

      viewModel.resetState(false)
      assertEquals(defaultState, viewModel.testGetState())
    }
  }

  @Test
  fun `load queued up photos should filter duplicates`() {
    runBlocking {
      whenever(takenPhotosRepository.loadNotUploadedPhotos()).thenReturn(
        listOf(
          TakenPhoto(1L, true, "123", null, PhotoState.PHOTO_QUEUED_UP)
        )
      )

      viewModel.testSetState(UploadedPhotosFragmentState(
        takenPhotos = listOf(TakenPhoto(1L, true, "123", null, PhotoState.PHOTO_QUEUED_UP))
      ))

      viewModel.loadQueuedUpPhotos()
      val takenPhotos = viewModel.testGetState().takenPhotos

      assertEquals(1, takenPhotos.size)
      assertEquals(1L, takenPhotos.first().id)
      assertEquals("123", takenPhotos.first().photoName)

      verify(intercom, times(1)).tell<PhotosActivity>().to(PhotosActivityEvent.StartUploadingService)
    }
  }

  @Test
  fun `should show error in recycler view when failed to load taken photos`() {
    runBlocking {
      doAnswer { throw IOException("BAM") }.`when`(takenPhotosRepository).loadNotUploadedPhotos()

      viewModel.loadQueuedUpPhotos()
      val state = viewModel.testGetState()

      assertTrue(state.takenPhotosRequest is Fail<List<TakenPhoto>>)

      verify(intercom, never()).tell<PhotosActivity>().to(PhotosActivityEvent.StartUploadingService)
    }
  }

  @Test
  fun `cancel queued up photo should remove photo from the state`() {
    runBlocking {
      val queuedUpPhotos = listOf(
        TakenPhoto(1L, true, "111", null, PhotoState.PHOTO_QUEUED_UP),
        TakenPhoto(2L, true, "112", null, PhotoState.PHOTO_QUEUED_UP),
        TakenPhoto(3L, true, "113", null, PhotoState.PHOTO_QUEUED_UP)
      )

      doAnswer {
        //throws exception without empty stub
      }.whenever(cancelPhotoUploadingUseCase).cancelPhotoUploading(any())

      viewModel.testSetState(UploadedPhotosFragmentState(takenPhotos = queuedUpPhotos))

      viewModel.cancelPhotoUploading(1L)
      viewModel.cancelPhotoUploading(2L)

      assertEquals(1, viewModel.testGetState().takenPhotos.size)
      assertEquals(3, viewModel.testGetState().takenPhotos.first().id)

      viewModel.cancelPhotoUploading(3L)

      assertTrue(viewModel.testGetState().takenPhotos.isEmpty())
    }
  }
}