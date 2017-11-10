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

    var sentPhotos: SentPhotosListFragment? = null
    var receivedPhotos: ReceivedPhotosListFragment? = null

    override fun getItem(position: Int): Fragment {
        when (position) {
            0 -> {
                if (sentPhotos == null) {
                    sentPhotos = SentPhotosListFragment.newInstance(isUploadingPhoto)
                }

                return sentPhotos!!
            }
            1 -> {
                if (receivedPhotos == null) {
                    receivedPhotos = ReceivedPhotosListFragment()
                }

                return receivedPhotos!!
            }
            else -> throw IllegalArgumentException("No fragment for the current position $position")
        }
    }

    override fun getCount(): Int = 2
}