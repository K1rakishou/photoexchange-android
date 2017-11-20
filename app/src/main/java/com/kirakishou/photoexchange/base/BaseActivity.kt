package com.kirakishou.photoexchange.base

import android.arch.lifecycle.LifecycleRegistry
import android.arch.lifecycle.ViewModel
import android.content.Intent
import android.os.Bundle
import android.support.annotation.CallSuper
import android.support.v7.app.AppCompatActivity
import android.widget.Toast
import butterknife.ButterKnife
import butterknife.Unbinder
import com.crashlytics.android.Crashlytics
import com.kirakishou.photoexchange.mwvm.model.other.ServerErrorCode
import com.kirakishou.photoexchange.mwvm.model.other.Fickle
import io.fabric.sdk.android.Fabric
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.plusAssign
import io.reactivex.subjects.PublishSubject
import timber.log.Timber


/**
 * Created by kirakishou on 7/20/2017.
 */
abstract class BaseActivity<out T: ViewModel> : AppCompatActivity() {

    private val registry by lazy {
        LifecycleRegistry(this)
    }

    override fun getLifecycle(): LifecycleRegistry = registry

    protected val compositeDisposable = CompositeDisposable()
    protected val unknownErrorsSubject = PublishSubject.create<Throwable>()!!

    private lateinit var viewModel: T
    private var unBinder: Fickle<Unbinder> = Fickle.empty()

    protected fun getViewModel(): T {
        return viewModel
    }

    @Suppress("UNCHECKED_CAST")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        Fabric.with(this, Crashlytics())

        compositeDisposable += unknownErrorsSubject
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(this::onUnknownError)

        resolveDaggerDependency()
        viewModel = initViewModel()

        setContentView(getContentView())
        unBinder = Fickle.of(ButterKnife.bind(this))

        onActivityCreate(savedInstanceState, intent)
    }

    override fun onDestroy() {
        compositeDisposable.clear()
        onActivityDestroy()

        unBinder.ifPresent {
            it.unbind()
        }

        super.onDestroy()
    }

    open fun onShowToast(message: String, duration: Int = Toast.LENGTH_LONG) {
        runOnUiThread {
            Toast.makeText(this, message, duration).show()
        }
    }

    @CallSuper
    open fun onBadResponse(serverErrorCode: ServerErrorCode) {
        Timber.d("ServerErrorCode: $serverErrorCode")
    }

    @CallSuper
    open fun onUnknownError(error: Throwable) {
        Timber.e(error)

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

    protected abstract fun initViewModel(): T
    protected abstract fun getContentView(): Int
    protected abstract fun onActivityCreate(savedInstanceState: Bundle?, intent: Intent)
    protected abstract fun onActivityDestroy()
    protected abstract fun resolveDaggerDependency()
}