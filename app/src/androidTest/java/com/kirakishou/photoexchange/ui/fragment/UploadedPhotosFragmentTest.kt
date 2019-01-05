package com.kirakishou.photoexchange.ui.fragment

import androidx.test.platform.app.InstrumentationRegistry.getInstrumentation
import androidx.test.rule.ActivityTestRule
import androidx.test.runner.AndroidJUnit4
import com.kirakishou.photoexchange.mock.FragmentTestingActivity
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith


@RunWith(AndroidJUnit4::class)
class UploadedPhotosFragmentTest {

  @get:Rule
  var activityRule: ActivityTestRule<FragmentTestingActivity> = ActivityTestRule(
    FragmentTestingActivity::class.java
  )

  @Test
  fun simpleTest() {
    val fragment = activityRule.activity.setFragment<UploadedPhotosFragment>(UploadedPhotosFragment.newInstance()) {
      getInstrumentation().waitForIdleSync()
    }

    println()
    println()
    println()
    println()
    println()
    println()
    println()

//    val factory = FragmentFactory()
//    val scenario = launchFragmentInContainer<UploadedPhotosFragment>(factory = factory)
//
//    scenario.onFragment { fragment ->
//      val photo = TakenPhoto(1L, true, "test123", File("test_file"), PhotoState.PHOTO_QUEUED_UP)
//      val newState = UploadedPhotosFragmentState(takenPhotos = listOf(photo))
//      fragment.viewModel.uploadedPhotosFragmentViewModel.testSetState(newState)
//
//      println("count = ${fragment.epoxyController.adapter.copyOfModels.size}")
//
//      println()
//      println()
//      println()
//      println()
//      println()
//      println()
//      println()
//      println()
//    }
  }
}