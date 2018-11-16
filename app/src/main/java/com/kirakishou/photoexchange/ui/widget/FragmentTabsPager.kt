package com.kirakishou.photoexchange.ui.widget

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentStatePagerAdapter
import com.kirakishou.photoexchange.ui.fragment.GalleryFragment
import com.kirakishou.photoexchange.ui.fragment.UploadedPhotosFragment
import com.kirakishou.photoexchange.ui.fragment.ReceivedPhotosFragment

/**
 * Created by kirakishou on 11/7/2017.
 */

class FragmentTabsPager(fm: FragmentManager) : FragmentStatePagerAdapter(fm) {

  override fun getItem(position: Int): Fragment {
    return when (position) {
      0 -> UploadedPhotosFragment.newInstance()
      1 -> ReceivedPhotosFragment.newInstance()
      2 -> GalleryFragment.newInstance()
      else -> throw IllegalArgumentException("No fragment for the current position $position")
    }
  }

  override fun getCount(): Int = 3
}