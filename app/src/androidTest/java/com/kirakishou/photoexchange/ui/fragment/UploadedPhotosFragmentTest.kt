package com.kirakishou.photoexchange.ui.fragment

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry.getInstrumentation
import androidx.test.rule.ActivityTestRule
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import android.content.Intent
import androidx.test.platform.app.InstrumentationRegistry
import com.kirakishou.photoexchange.mock.FragmentTestingActivity
import com.kirakishou.photoexchange.mvp.model.PhotoState
import com.kirakishou.photoexchange.mvp.model.photo.TakenPhoto
import com.kirakishou.photoexchange.mvp.viewmodel.state.UploadedPhotosFragmentState
import java.io.File


@RunWith(AndroidJUnit4::class)
class UploadedPhotosFragmentTest {

  @get:Rule
  var activityRule: ActivityTestRule<FragmentTestingActivity> = ActivityTestRule(
    FragmentTestingActivity::class.java,
    false,
    false
  )

  @Test
  fun simpleTest() {
    val intent = Intent(
      InstrumentationRegistry.getInstrumentation().targetContext,
      FragmentTestingActivity::class.java
    )
    activityRule.launchActivity(intent)

    val fragment = activityRule.activity.setFragment<UploadedPhotosFragment>(UploadedPhotosFragment.newInstance()) {
      getInstrumentation().waitForIdleSync()
    }

    val photo = TakenPhoto(1L, true, "test123", File("test_file"), PhotoState.PHOTO_QUEUED_UP)
    val newState = UploadedPhotosFragmentState(takenPhotos = listOf(photo))
    fragment.viewModel.uploadedPhotosFragmentViewModel.testSetState(newState)
    println("count = ${fragment.epoxyController.adapter.copyOfModels.size}")
  }
}