package com.kirakishou.photoexchange.mvrx.viewmodel

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
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Test

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
}