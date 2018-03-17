package com.kirakishou.photoexchange.ui.fragment

import android.os.Bundle
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.kirakishou.fixmypc.photoexchange.R


class MyPhotosFragment : Fragment() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_my_photos, container, false)
    }

    companion object {
        fun newInstance(): MyPhotosFragment {
            val fragment = MyPhotosFragment()
            val args = Bundle()

            fragment.arguments = args
            return fragment
        }
    }
}
