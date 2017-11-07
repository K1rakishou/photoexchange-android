package com.kirakishou.photoexchange.ui.widget

import android.support.v4.app.Fragment
import android.support.v4.app.FragmentManager
import android.support.v4.app.FragmentStatePagerAdapter

/**
 * Created by kirakishou on 11/7/2017.
 */

class FragmentTabsPager(fm: FragmentManager) : FragmentStatePagerAdapter(fm) {

    override fun getItem(position: Int): Fragment {
        return when (position) {
            else -> throw IllegalArgumentException("No fragment for the current position $position")
        }
    }

    override fun getCount(): Int = 2
}