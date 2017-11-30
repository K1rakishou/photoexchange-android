package com.kirakishou.photoexchange.ui.fragment

import com.kirakishou.fixmypc.photoexchange.BuildConfig
import com.kirakishou.photoexchange.PhotoExchangeApplication
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.shadows.support.v4.SupportFragmentTestUtil

/**
 * Created by kirakishou on 11/30/2017.
 */

@RunWith(RobolectricTestRunner::class)
@Config(constants = BuildConfig::class)
class QueuedUpPhotosListFragmentTest {

    @Test
    fun test() {
        SupportFragmentTestUtil.startFragment(QueuedUpPhotosListFragment.newInstance())
    }
}