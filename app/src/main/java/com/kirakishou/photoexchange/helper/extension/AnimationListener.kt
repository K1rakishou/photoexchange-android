package com.kirakishou.photoexchange.helper.extension

import android.animation.Animator
import android.view.ViewPropertyAnimator
import com.kirakishou.photoexchange.mwvm.model.other.Fickle

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
    private var myOnAnimationRepeat = Fickle.empty<(animation: Animator?) -> Unit>()
    private var myOnAnimationEnd = Fickle.empty<(animation: Animator?) -> Unit>()
    private var myOnAnimationStart = Fickle.empty<(animation: Animator?) -> Unit>()
    private var myOnAnimationCancel = Fickle.empty<(animation: Animator?) -> Unit>()

    override fun onAnimationRepeat(animation: Animator?) {
        myOnAnimationRepeat.ifPresent {
            it.invoke(animation)
        }
    }

    override fun onAnimationEnd(animation: Animator?) {
        myOnAnimationEnd.ifPresent {
            it.invoke(animation)
        }
    }

    override fun onAnimationStart(animation: Animator?) {
        myOnAnimationStart.ifPresent {
            it.invoke(animation)
        }
    }

    override fun onAnimationCancel(animation: Animator?) {
        myOnAnimationCancel.ifPresent {
            it.invoke(animation)
        }
    }

    fun onAnimationRepeat(func: (animation: Animator?) -> Unit) {
        myOnAnimationRepeat = Fickle.of(func)
    }

    fun onAnimationEnd(func: (animation: Animator?) -> Unit) {
        myOnAnimationEnd = Fickle.of(func)
    }

    fun onAnimationStart(func: (animation: Animator?) -> Unit) {
        myOnAnimationStart = Fickle.of(func)
    }

    fun onAnimationCancel(func: (animation: Animator?) -> Unit) {
        myOnAnimationCancel = Fickle.of(func)
    }
}