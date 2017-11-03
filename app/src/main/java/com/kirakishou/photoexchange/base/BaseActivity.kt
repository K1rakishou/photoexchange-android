package com.kirakishou.photoexchange.base

import android.animation.AnimatorSet
import android.arch.lifecycle.LifecycleRegistry
import android.arch.lifecycle.ViewModel
import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.widget.Toast
import butterknife.ButterKnife
import butterknife.Unbinder
import com.kirakishou.fixmypc.fixmypcapp.helper.extension.myAddListener
import com.kirakishou.fixmypc.fixmypcapp.mvvm.model.Fickle
import com.kirakishou.photoexchange.helper.extension.hideKeyboard
import io.reactivex.disposables.CompositeDisposable


/**
 * Created by kirakishou on 7/20/2017.
 */
abstract class BaseActivity<out T: ViewModel> : AppCompatActivity() {

    private val mRegistry by lazy {
        LifecycleRegistry(this)
    }

    override fun getLifecycle(): LifecycleRegistry = mRegistry

    protected val mCompositeDisposable = CompositeDisposable()
    private var mViewModel: Fickle<T> = Fickle.empty()
    private var mUnBinder: Fickle<Unbinder> = Fickle.empty()

    protected fun getViewModel(): T {
        return mViewModel.get()
    }

    private fun overridePendingTransitionEnter() {
        overridePendingTransition(0, 0)
    }

    private fun overridePendingTransitionExit() {
        overridePendingTransition(0, 0)
    }

    override fun startActivity(intent: Intent) {
        super.startActivity(intent)
        overridePendingTransitionEnter()
    }

    override fun finish() {
        overridePendingTransitionExit()
        super.finish()
    }

    @Suppress("UNCHECKED_CAST")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        resolveDaggerDependency()
        mViewModel = Fickle.of(initViewModel())

        setContentView(getContentView())
        mUnBinder = Fickle.of(ButterKnife.bind(this))
        //Fabric.with(this, Crashlytics())

        onActivityCreate(savedInstanceState, intent)
    }

    override fun onStart() {
        super.onStart()

        animateActivityStart()
    }

    override fun onStop() {
        super.onStop()

        hideKeyboard()
        animateActivityStop()
    }

    override fun onDestroy() {
        super.onDestroy()

        onActivityDestroy()
        mCompositeDisposable.clear()

        mUnBinder.ifPresent {
            it.unbind()
        }
    }

    private fun animateActivityStop() {
        runCallbackAfterAnimation(loadExitAnimations()) {
            onActivityStop()
        }
    }

    private fun animateActivityStart() {
        runCallbackAfterAnimation(loadStartAnimations()) {
            onActivityStart()
        }
    }

    protected fun runCallbackAfterAnimation(set: AnimatorSet, onExitAnimationCallback: () -> Unit) {
        set.myAddListener {
            onAnimationEnd {
                onExitAnimationCallback()
            }
        }

        set.start()
    }

    open fun onShowToast(message: String, duration: Int = Toast.LENGTH_LONG) {
        runOnUiThread {
            Toast.makeText(this, message, duration).show()
        }
    }

    open fun onUnknownError(error: Throwable) {
        if (error.message != null) {
            onShowToast(error.message!!)
        } else {
            onShowToast("Неизвестная ошибка")
        }

        finish()
    }

    open fun runActivity(clazz: Class<*>, finishCurrentActivity: Boolean = false) {
        val intent = Intent(this, clazz)
        startActivity(intent)

        if (finishCurrentActivity) {
            finish()
        }
    }

    open fun finishActivity() {
        finish()
    }

    open fun runActivityWithArgs(clazz: Class<*>, args: Bundle, finishCurrentActivity: Boolean) {
        val intent = Intent(this, clazz)
        intent.putExtras(args)
        startActivity(intent)

        if (finishCurrentActivity) {
            finish()
        }
    }

    protected abstract fun initViewModel(): T?
    protected abstract fun getContentView(): Int
    protected abstract fun loadStartAnimations(): AnimatorSet
    protected abstract fun loadExitAnimations(): AnimatorSet
    protected abstract fun onActivityCreate(savedInstanceState: Bundle?, intent: Intent)
    protected abstract fun onActivityDestroy()
    protected abstract fun onActivityStart()
    protected abstract fun onActivityStop()
    protected abstract fun resolveDaggerDependency()
}