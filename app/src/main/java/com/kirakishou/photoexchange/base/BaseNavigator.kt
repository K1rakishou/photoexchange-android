package com.kirakishou.photoexchange.base

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.support.v4.app.Fragment
import android.support.v7.app.AppCompatActivity
import com.kirakishou.fixmypc.photoexchange.R
import com.kirakishou.photoexchange.helper.extension.newInstance
import kotlin.reflect.KClass

/**
 * Created by kirakishou on 11/7/2017.
 */
open class BaseNavigator(activity: AppCompatActivity) {

    protected val fragmentManager = activity.supportFragmentManager

    //we use handler to ensure the order of execution
    protected val navigatorHandler = Handler(Looper.getMainLooper())

    fun popFragment() {
        navigatorHandler.post {
            val currentFragment = getVisibleFragment()
            if (currentFragment != null) {
                fragmentManager.popBackStack()
            }
        }
    }

    fun getVisibleFragment(): Fragment? {
        val fragments = fragmentManager.fragments
        if (fragments != null) {
            for (fragment in fragments) {
                if (fragment != null && fragment.isVisible)
                    return fragment
            }
        }

        return null
    }

    fun getFragmentByTag(tag: String): Fragment? {
        val fragments = fragmentManager.fragments
        if (fragments != null) {
            for (fragment in fragments) {
                if (fragment != null && fragment.tag == tag)
                    return fragment
            }
        }

        return null
    }

    fun navigateToFragment(fragmentClass: KClass<*>, fragmentTag: String,
                           bundle: Bundle? = null, fragmentFrameId: Int = R.id.fragment_frame) {
        navigatorHandler.post {
            val fragmentTransaction = fragmentManager.beginTransaction()
            val visibleFragment = getVisibleFragment()

            if (visibleFragment != null) {
                if (visibleFragment::class == fragmentClass) {
                    //do nothing if we are already showing this fragment
                    return@post
                }

                fragmentTransaction.hide(visibleFragment)
            }

            val fragmentInStack = fragmentManager.findFragmentByTag(fragmentTag)
            if (fragmentInStack == null) {
                val newFragment = fragmentClass.newInstance<Fragment>()
                if (bundle != null) {
                    newFragment.arguments = bundle
                }

                fragmentTransaction
                        .add(fragmentFrameId, newFragment, fragmentTag)
                        .addToBackStack(null)
            } else {
                fragmentTransaction
                        .show(fragmentInStack)
            }

            fragmentTransaction.commit()
        }
    }
}