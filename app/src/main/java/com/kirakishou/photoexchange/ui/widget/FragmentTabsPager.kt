package com.kirakishou.photoexchange.ui.widget

import android.support.v4.app.Fragment
import android.support.v4.app.FragmentManager
import android.support.v4.app.FragmentStatePagerAdapter
import com.kirakishou.photoexchange.ui.fragment.GalleryFragment
import com.kirakishou.photoexchange.ui.fragment.MyPhotosFragment
import com.kirakishou.photoexchange.ui.fragment.ReceivedPhotosFragment
import java.lang.ref.WeakReference

/**
 * Created by kirakishou on 11/7/2017.
 */

class FragmentTabsPager(fm: FragmentManager) : FragmentStatePagerAdapter(fm) {

    private var myPhotosFragment = WeakReference<MyPhotosFragment>(null)

    fun getMyPhotosFragment(): MyPhotosFragment? {
        return myPhotosFragment.get()
    }

    override fun getItem(position: Int): Fragment {
        return when (position) {
            0 -> {
                val fragment = MyPhotosFragment.newInstance()
                myPhotosFragment = WeakReference(fragment)
                return fragment
            }
            1 -> ReceivedPhotosFragment.newInstance()
            2 -> GalleryFragment.newInstance()
            else -> throw IllegalArgumentException("No fragment for the current position $position")
        }
    }

    override fun getCount(): Int = 3
}