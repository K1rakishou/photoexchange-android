package com.kirakishou.photoexchange.mock

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.kirakishou.fixmypc.photoexchange.R
import com.kirakishou.photoexchange.dagger.module.activity.MockPhotosActivityModule
import com.kirakishou.photoexchange.di.component.activity.BasePhotosActivityComponent
import com.kirakishou.photoexchange.ui.activity.HasActivityComponent


class FragmentTestingActivity : AppCompatActivity(), HasActivityComponent<BasePhotosActivityComponent<FragmentTestingActivity>> {

  private val activityComponentInternal by lazy {
    (application as MockApplication).applicationComponent
      .plus(MockPhotosActivityModule(this))
  }

  override fun getActivityComponent(): BasePhotosActivityComponent<FragmentTestingActivity> {
    return activityComponentInternal
  }

  override fun onCreate(savedInstance: Bundle?) {
    super.onCreate(savedInstance)
    setContentView(R.layout.activity_fragment_testing)
  }

  fun <T : Fragment> setFragment(fragment: Fragment, waitForIdleSyncFunc: () -> Unit): T {
    supportFragmentManager.beginTransaction().apply {
      add(R.id.test_fragment_container, fragment, "tag")
      commit()
    }

    waitForIdleSyncFunc()
    return supportFragmentManager.findFragmentByTag("tag") as T
  }

}