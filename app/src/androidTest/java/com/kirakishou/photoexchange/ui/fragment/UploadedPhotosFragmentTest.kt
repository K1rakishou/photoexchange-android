package com.kirakishou.photoexchange.ui.fragment

import android.content.Intent
import androidx.recyclerview.widget.RecyclerView
import androidx.room.Room
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.contrib.RecyclerViewActions
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.platform.app.InstrumentationRegistry.getInstrumentation
import androidx.test.rule.ActivityTestRule
import com.kirakishou.fixmypc.photoexchange.R
import com.kirakishou.photoexchange.helper.ImageLoader
import com.kirakishou.photoexchange.helper.LonLat
import com.kirakishou.photoexchange.helper.concurrency.coroutines.MockDispatchers
import com.kirakishou.photoexchange.helper.database.MyDatabase
import com.kirakishou.photoexchange.helper.database.repository.ReceivedPhotosRepository
import com.kirakishou.photoexchange.helper.database.repository.SettingsRepository
import com.kirakishou.photoexchange.helper.database.repository.TakenPhotosRepository
import com.kirakishou.photoexchange.helper.database.repository.UploadedPhotosRepository
import com.kirakishou.photoexchange.helper.database.source.local.TakenPhotosLocalSource
import com.kirakishou.photoexchange.helper.database.source.local.TempFileLocalSource
import com.kirakishou.photoexchange.helper.intercom.PhotosActivityViewModelIntercom
import com.kirakishou.photoexchange.helper.intercom.event.PhotosActivityEvent
import com.kirakishou.photoexchange.helper.intercom.event.UploadedPhotosFragmentEvent
import com.kirakishou.photoexchange.helper.util.FileUtilsImpl
import com.kirakishou.photoexchange.helper.util.NetUtils
import com.kirakishou.photoexchange.helper.util.TimeUtils
import com.kirakishou.photoexchange.interactors.BlacklistPhotoUseCase
import com.kirakishou.photoexchange.interactors.CheckFirebaseAvailabilityUseCase
import com.kirakishou.photoexchange.interactors.GetUploadedPhotosUseCase
import com.kirakishou.photoexchange.mock.FragmentTestingActivity
import com.kirakishou.photoexchange.mvp.model.PhotoState
import com.kirakishou.photoexchange.mvp.model.photo.TakenPhoto
import com.kirakishou.photoexchange.mvp.viewmodel.GalleryFragmentViewModel
import com.kirakishou.photoexchange.mvp.viewmodel.PhotosActivityViewModel
import com.kirakishou.photoexchange.mvp.viewmodel.ReceivedPhotosFragmentViewModel
import com.kirakishou.photoexchange.mvp.viewmodel.UploadedPhotosFragmentViewModel
import com.kirakishou.photoexchange.mvp.viewmodel.state.UploadedPhotosFragmentState
import com.kirakishou.photoexchange.ui.epoxy.controller.UploadedPhotosFragmentEpoxyController
import com.nhaarman.mockito_kotlin.doReturn
import junit.framework.Assert.*
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito


@RunWith(AndroidJUnit4::class)
class UploadedPhotosFragmentTest {
  val imageLoader = Mockito.mock(ImageLoader::class.java)

  val dispatchers = MockDispatchers()
  val intercom = PhotosActivityViewModelIntercom()
  val netUtils = Mockito.mock(NetUtils::class.java)
  val timeUtils = Mockito.mock(TimeUtils::class.java)
  val fileUtils = FileUtilsImpl()

  lateinit var takenPhotosLocalSource: TakenPhotosLocalSource
  lateinit var tempFilesLocalSource: TempFileLocalSource

  lateinit var takenPhotosRepository: TakenPhotosRepository
  lateinit var uploadedPhotosRepository: UploadedPhotosRepository
  lateinit var receivedPhotosRepository: ReceivedPhotosRepository
  lateinit var settingsRepository: SettingsRepository

  lateinit var getUploadedPhotosUseCase: GetUploadedPhotosUseCase
  lateinit var blackListPhotoUseCase: BlacklistPhotoUseCase
  lateinit var checkFirebaseAvailabilityUseCase: CheckFirebaseAvailabilityUseCase

  lateinit var uploadedPhotosFragmentViewModel: UploadedPhotosFragmentViewModel

  lateinit var receivedPhotosFragmentViewModel: ReceivedPhotosFragmentViewModel
  lateinit var galleryPhotosViewModel: GalleryFragmentViewModel

  lateinit var photosActivityViewModel: PhotosActivityViewModel

  @get:Rule
  var activityRule: ActivityTestRule<FragmentTestingActivity> = ActivityTestRule(
    FragmentTestingActivity::class.java,
    false,
    false
  )

  private val waitTime = 15L

  @Before
  fun init() {
    val context = InstrumentationRegistry.getInstrumentation().targetContext
    val database = Room.inMemoryDatabaseBuilder(context, MyDatabase::class.java).build()

    takenPhotosLocalSource = TakenPhotosLocalSource(
      database,
      timeUtils
    )

    tempFilesLocalSource = TempFileLocalSource(
      database,
      context.filesDir.absolutePath,
      timeUtils,
      fileUtils
    )

    takenPhotosRepository = TakenPhotosRepository(
      timeUtils,
      database,
      takenPhotosLocalSource,
      tempFilesLocalSource
    )

    uploadedPhotosRepository = Mockito.mock(UploadedPhotosRepository::class.java)
    receivedPhotosRepository = Mockito.mock(ReceivedPhotosRepository::class.java)
    settingsRepository = Mockito.mock(SettingsRepository::class.java)

    getUploadedPhotosUseCase = Mockito.mock(GetUploadedPhotosUseCase::class.java)
    blackListPhotoUseCase = Mockito.mock(BlacklistPhotoUseCase::class.java)
    checkFirebaseAvailabilityUseCase = Mockito.mock(CheckFirebaseAvailabilityUseCase::class.java)

    uploadedPhotosFragmentViewModel = UploadedPhotosFragmentViewModel(
      UploadedPhotosFragmentState(),
      intercom,
      takenPhotosRepository,
      uploadedPhotosRepository,
      getUploadedPhotosUseCase,
      dispatchers
    )

    receivedPhotosFragmentViewModel = Mockito.mock(ReceivedPhotosFragmentViewModel::class.java)
    galleryPhotosViewModel = Mockito.mock(GalleryFragmentViewModel::class.java)

    photosActivityViewModel = PhotosActivityViewModel(
      uploadedPhotosFragmentViewModel,
      receivedPhotosFragmentViewModel,
      galleryPhotosViewModel,
      intercom,
      netUtils,
      settingsRepository,
      takenPhotosRepository,
      uploadedPhotosRepository,
      receivedPhotosRepository,
      blackListPhotoUseCase,
      checkFirebaseAvailabilityUseCase,
      dispatchers
    )

    activityRule.launchActivity(
      Intent(
        context,
        FragmentTestingActivity::class.java
      )
    )
  }

  private fun attachFragment(): UploadedPhotosFragment {
    val newFragment = UploadedPhotosFragment.newInstance().apply {
      viewModel = photosActivityViewModel
      controller = UploadedPhotosFragmentEpoxyController(imageLoader)
    }

    return activityRule.activity.setFragment(newFragment) {
      getInstrumentation().waitForIdleSync()
    }
  }

  @Test
  fun test_uploading_of_50_queued_up_photos_should_end_up_with_50_uploaded_photo_and_0_queued_up() {
    runBlocking {
      val count = 50
      val takenPhotos = mutableListOf<TakenPhoto>()

      for (i in 0 until count) {
        val tempFile = takenPhotosRepository.createTempFile()
        val takenPhoto = takenPhotosRepository.saveTakenPhoto(tempFile)
        assertNotNull(takenPhoto)

        takenPhotosRepository.updatePhotoState(takenPhoto!!.id, PhotoState.PHOTO_QUEUED_UP)

        takenPhotos += TakenPhoto(
          takenPhoto.id,
          takenPhoto.isPublic,
          takenPhoto.photoName,
          takenPhoto.photoTempFile,
          PhotoState.PHOTO_QUEUED_UP
        )
      }

      doReturn(false).`when`(netUtils).canLoadImages()

      val fragment = attachFragment()
      uploadPhotos(takenPhotos)

      val state = fragment.viewModel.uploadedPhotosFragmentViewModel.testGetState()

      assertEquals(0, state.takenPhotos.size)
      assertEquals(count, state.uploadedPhotos.size)

      for (i in 0 until state.uploadedPhotos.size - 1) {
        val current = state.uploadedPhotos[i]
        val next = state.uploadedPhotos[i + 1]

        //ensure uploaded photos sorted in descending order
        assertTrue(current.photoId > next.photoId)
      }

      val position = state.uploadedPhotos.size - 1

      //scroll to bottom to ensure nothing crashes for no reason
      onView(withId(R.id.recycler_view)).perform(
        RecyclerViewActions.scrollToPosition<RecyclerView.ViewHolder>(position)
      )
      getInstrumentation().waitForIdleSync()
    }
  }

  @Test
  fun test_uploaded_photo_click_should_generate_show_toast_events_for_photos_with_no_receiver_info() {
    runBlocking {
      val count = 20
      val takenPhotos = mutableListOf<TakenPhoto>()

      for (i in 0 until count) {
        val tempFile = takenPhotosRepository.createTempFile()
        val takenPhoto = takenPhotosRepository.saveTakenPhoto(tempFile)
        assertNotNull(takenPhoto)

        takenPhotosRepository.updatePhotoState(takenPhoto!!.id, PhotoState.PHOTO_QUEUED_UP)

        takenPhotos += TakenPhoto(
          takenPhoto.id,
          takenPhoto.isPublic,
          takenPhoto.photoName,
          takenPhoto.photoTempFile,
          PhotoState.PHOTO_QUEUED_UP
        )
      }

      doReturn(false).`when`(netUtils).canLoadImages()

      attachFragment()
      uploadPhotos(takenPhotos)

      val testObserver = intercom.photosActivityEvents.listen().test()

      uploadedPhotosFragmentViewModel.swapPhotoAndMap("test_name_11")
      uploadedPhotosFragmentViewModel.swapPhotoAndMap("test_name_4")
      uploadedPhotosFragmentViewModel.swapPhotoAndMap("test_name_18")
      Thread.sleep(waitTime)

      val values = testObserver.values()
      assertEquals(3, values.size)
      assertTrue(values.all { it is PhotosActivityEvent.ShowToast })
    }
  }

  @Test
  fun test_should_be_able_to_cancel_queued_up_photos() {
    val count = 10
    val takenPhotos = mutableListOf<TakenPhoto>()

    runBlocking {
      for (i in 0 until count) {
        val tempFile = takenPhotosRepository.createTempFile()
        val takenPhoto = takenPhotosRepository.saveTakenPhoto(tempFile)
        assertNotNull(takenPhoto)

        takenPhotosRepository.updatePhotoState(takenPhoto!!.id, PhotoState.PHOTO_QUEUED_UP)

        takenPhotos += TakenPhoto(
          takenPhoto.id,
          takenPhoto.isPublic,
          takenPhoto.photoName,
          takenPhoto.photoTempFile,
          PhotoState.PHOTO_QUEUED_UP
        )
      }

      doReturn(false).`when`(netUtils).canLoadImages()

      val fragment = attachFragment()
      val testObserver = intercom.photosActivityEvents.listen().test()

      onView(withId(R.id.recycler_view)).perform(
        RecyclerViewActions.actionOnItemAtPosition<RecyclerView.ViewHolder>(0, click())
      )
      getInstrumentation().waitForIdleSync()

      val state = fragment.viewModel.uploadedPhotosFragmentViewModel.testGetState()
      assertEquals(0, state.takenPhotos.size)

      val values = testObserver.values()
      assertEquals(count + 1, values.size)

      assertTrue(values.take(1).all { it is PhotosActivityEvent.StartUploadingService })
      assertTrue(values.drop(1).all { it is PhotosActivityEvent.CancelPhotoUploading })
    }
  }

  private fun uploadPhotos(takenPhotos: MutableList<TakenPhoto>) {
    runBlocking {
      val state = uploadedPhotosFragmentViewModel.testGetState()

      for (i in 0 until state.takenPhotos.size - 1) {
        val current = state.takenPhotos[i]
        val next = state.takenPhotos[i + 1]

        //ensure queued up photos sorted in descending order
        assertTrue(current.id > next.id)
      }
    }

    for (takenPhoto in takenPhotos) {
      val newId = takenPhoto.id

      intercom.tell<UploadedPhotosFragment>()
        .that(UploadedPhotosFragmentEvent.PhotoUploadEvent.OnPhotoUploadingStart(takenPhoto))
      Thread.sleep(waitTime)

      intercom.tell<UploadedPhotosFragment>()
        .that(
          UploadedPhotosFragmentEvent.PhotoUploadEvent.OnPhotoUploadingProgress(
            takenPhoto,
            25
          )
        )
      Thread.sleep(waitTime)

      intercom.tell<UploadedPhotosFragment>()
        .that(
          UploadedPhotosFragmentEvent.PhotoUploadEvent.OnPhotoUploadingProgress(
            takenPhoto,
            50
          )
        )
      Thread.sleep(waitTime)

      intercom.tell<UploadedPhotosFragment>()
        .that(
          UploadedPhotosFragmentEvent.PhotoUploadEvent.OnPhotoUploadingProgress(
            takenPhoto,
            75
          )
        )
      Thread.sleep(waitTime)

      intercom.tell<UploadedPhotosFragment>()
        .that(
          UploadedPhotosFragmentEvent.PhotoUploadEvent.OnPhotoUploadingProgress(
            takenPhoto,
            100
          )
        )
      Thread.sleep(waitTime)

      intercom.tell<UploadedPhotosFragment>()
        .that(
          UploadedPhotosFragmentEvent.PhotoUploadEvent.OnPhotoUploaded(
            takenPhoto,
            newId,
            "test_name_$newId",
            666L,
            LonLat(11.1, 22.2)
          )
        )
      Thread.sleep(waitTime)
    }

    intercom.tell<UploadedPhotosFragment>()
      .that(UploadedPhotosFragmentEvent.PhotoUploadEvent.OnEnd())
    Thread.sleep(waitTime)
  }

}