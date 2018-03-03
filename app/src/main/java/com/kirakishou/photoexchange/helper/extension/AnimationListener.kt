package com.kirakishou.photoexchange.helper.extension

import android.animation.Animator
import android.view.ViewPropertyAnimator

/**
 * Created by kirakishou on 7/22/2017.
 */

inline fun Animator.myAddListener(func: MyAnimationListener.() -> Unit) {
    val listener = MyAnimationListener()
    listener.func()
    addListener(listener)
}

inline fun ViewPropertyAnimator.mySetListener(func: MyAnimationListener.() -> Unit): ViewPropertyAnimator {
    val listener = MyAnimationListener()
    listener.func()
    setListener(listener)

    return this
}

class MyAnimationListener : Animator.AnimatorListener {
    private var myOnAnimationRepeat: ((animation: Animator?) -> Unit)? = null
    private var myOnAnimationEnd: ((animation: Animator?) -> Unit)? = null
    private var myOnAnimationStart: ((animation: Animator?) -> Unit)? = null
    private var myOnAnimationCancel: ((animation: Animator?) -> Unit)? = null

    override fun onAnimationRepeat(animation: Animator?) {
        myOnAnimationRepeat?.invoke(animation)
    }

    override fun onAnimationEnd(animation: Animator?) {
        myOnAnimationEnd?.invoke(animation)
    }

    override fun onAnimationStart(animation: Animator?) {
        myOnAnimationStart?.invoke(animation)
    }

    override fun onAnimationCancel(animation: Animator?) {
        myOnAnimationCancel?.invoke(animation)
    }

    fun onAnimationRepeat(func: (animation: Animator?) -> Unit) {
        myOnAnimationRepeat = func
    }

    fun onAnimationEnd(func: (animation: Animator?) -> Unit) {
        myOnAnimationEnd = func
    }

    fun onAnimationStart(func: (animation: Animator?) -> Unit) {
        myOnAnimationStart = func
    }

    fun onAnimationCancel(func: (animation: Animator?) -> Unit) {
        myOnAnimationCancel = func
    }
}