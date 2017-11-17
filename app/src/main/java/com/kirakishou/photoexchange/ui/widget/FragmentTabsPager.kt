package com.kirakishou.photoexchange.ui.widget

import android.support.v4.app.Fragment
import android.support.v4.app.FragmentManager
import android.support.v4.app.FragmentStatePagerAdapter
import com.kirakishou.photoexchange.ui.fragment.ReceivedPhotosListFragment
import com.kirakishou.photoexchange.ui.fragment.SentPhotosListFragment

/**
 * Created by kirakishou on 11/7/2017.
 */

class FragmentTabsPager(fm: FragmentManager) : FragmentStatePagerAdapter(fm) {

    var isUploadingPhoto = false

    var sentPhotosFragment: SentPhotosListFragment? = null
    var receivedPhotosFragment: ReceivedPhotosListFragment? = null

    override fun getItem(position: Int): Fragment {
        when (position) {
            0 -> {
                if (sentPhotosFragment == null) {
                    sentPhotosFragment = SentPhotosListFragment.newInstance(isUploadingPhoto)
                }

                return sentPhotosFragment!!
            }
            1 -> {
                if (receivedPhotosFragment == null) {
                    receivedPhotosFragment = ReceivedPhotosListFragment.newInstance(isUploadingPhoto)
                }

                return receivedPhotosFragment!!
            }
            else -> throw IllegalArgumentException("No fragment for the current position $position")
        }
    }

    override fun getCount(): Int = 2
}