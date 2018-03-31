package com.kirakishou.photoexchange.ui.activity

import android.arch.lifecycle.LifecycleRegistry
import android.content.Intent
import android.os.Bundle
import android.support.annotation.CallSuper
import android.support.v7.app.AppCompatActivity
import android.widget.Toast
import butterknife.ButterKnife
import butterknife.Unbinder
import com.crashlytics.android.Crashlytics
import com.kirakishou.photoexchange.PhotoExchangeApplication
import com.kirakishou.photoexchange.mvp.model.other.ServerErrorCode
import io.fabric.sdk.android.Fabric
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.plusAssign
import io.reactivex.subjects.PublishSubject
import timber.log.Timber


/**
 * Created by kirakishou on 7/20/2017.
 */
abstract class BaseActivity : AppCompatActivity() {

    private val registry by lazy {
        LifecycleRegistry(this)
    }

    override fun getLifecycle(): LifecycleRegistry = registry

    protected val unknownErrorsSubject = PublishSubject.create<Throwable>()!!

    protected val compositeDisposable = CompositeDisposable()
    private var unBinder: Unbinder? = null

    @Suppress("UNCHECKED_CAST")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        //Timber.d("${this::class.java}.onCreate")

        setContentView(getContentView())
        unBinder = ButterKnife.bind(this)

        resolveDaggerDependency()

        Fabric.with(this, Crashlytics())

        compositeDisposable += unknownErrorsSubject
            .observeOn(AndroidSchedulers.mainThread())
            .doOnError(this::onUnknownError)
            .subscribe(this::onUnknownError)

        onActivityCreate(savedInstanceState, intent)
    }

    override fun onDestroy() {
        //Timber.d("${this::class.java}.onDestroy")

        onActivityDestroy()

        unBinder?.unbind()
        compositeDisposable.clear()

        PhotoExchangeApplication.watch(this, this::class.simpleName)
        super.onDestroy()
    }

    open fun onShowToast(message: String, duration: Int = Toast.LENGTH_LONG) {
        runOnUiThread {
            Toast.makeText(this, message, duration).show()
        }
    }

    @CallSuper
    open fun onBadResponse(serverErrorCode: ServerErrorCode) {
        Timber.e("ServerErrorCode: $serverErrorCode")
    }

    @CallSuper
    open fun onUnknownError(error: Throwable) {
        Timber.e(error)

        if (error.message != null) {
            onShowToast(error.message!!)
        } else {
            onShowToast("Unknown error")
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

    open fun runActivityWithArgs(clazz: Class<*>, args: Bundle, finishCurrentActivity: Boolean) {
        val intent = Intent(this, clazz)
        intent.putExtras(args)
        startActivity(intent)

        if (finishCurrentActivity) {
            finish()
        }
    }

    protected abstract fun getContentView(): Int
    protected abstract fun onActivityCreate(savedInstanceState: Bundle?, intent: Intent)
    protected abstract fun onActivityDestroy()
    protected abstract fun resolveDaggerDependency()
}