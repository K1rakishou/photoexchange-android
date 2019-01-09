package com.kirakishou.photoexchange.mock

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.kirakishou.fixmypc.photoexchange.R


class FragmentTestingActivity : AppCompatActivity() {

  override fun onCreate(savedInstance: Bundle?) {
    super.onCreate(savedInstance)
    setContentView(R.layout.activity_fragment_testing)
  }

  fun <T : Fragment> replaceFragment(fragment: Fragment, waitForIdleSyncFunc: (() -> Unit)? = null): T {
    runOnUiThread {
      supportFragmentManager.beginTransaction().apply {
        replace(R.id.test_fragment_container, fragment, "tag")
        commitNow()
      }
    }

    waitForIdleSyncFunc?.invoke()
    return supportFragmentManager.findFragmentByTag("tag") as T
  }

  fun getFragmentsCount(): Int {
    return supportFragmentManager.fragments.size
  }
}