package com.kirakishou.photoexchange.ui.activity

import androidx.lifecycle.LifecycleRegistry
import android.content.Intent
import android.os.Bundle
import androidx.annotation.CallSuper
import androidx.appcompat.app.AppCompatActivity
import android.widget.Toast
import butterknife.ButterKnife
import butterknife.Unbinder
import com.kirakishou.photoexchange.PhotoExchangeApplication
import core.ErrorCode
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.plusAssign
import io.reactivex.subjects.PublishSubject
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.ReceiveChannel
import timber.log.Timber
import kotlin.coroutines.CoroutineContext


/**
 * Created by kirakishou on 7/20/2017.
 */
abstract class BaseActivity : AppCompatActivity(), CoroutineScope {
  private val TAG = "${this::class.java}"

  private val registry by lazy {
    LifecycleRegistry(this)
  }

  override fun getLifecycle(): LifecycleRegistry = registry

  protected val unknownErrorsSubject = PublishSubject.create<Throwable>()
  protected val compositeDisposable = CompositeDisposable()

  private val job = Job()
  private var unBinder: Unbinder? = null

  override val coroutineContext: CoroutineContext
    get() = job + Dispatchers.Main

  @Suppress("UNCHECKED_CAST")
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    setContentView(getContentView())
    resolveDaggerDependency()

    unBinder = ButterKnife.bind(this)
    onActivityCreate(savedInstanceState, intent)
  }

  override fun onStart() {
    super.onStart()

    compositeDisposable += unknownErrorsSubject
      .observeOn(AndroidSchedulers.mainThread())
      .doOnError(this::onUnknownError)
      .subscribe(this::onUnknownError)

    onActivityStart()
  }

  override fun onStop() {
    onActivityStop()

    job.cancel()
    compositeDisposable.clear()
    super.onStop()
  }

  override fun onDestroy() {
    unBinder?.unbind()
    PhotoExchangeApplication.watch(this, this::class.simpleName)
    super.onDestroy()
  }

  open fun onShowToast(message: String?, duration: Int = Toast.LENGTH_LONG) {
    runOnUiThread {
      Toast.makeText(this, message ?: "Empty message", duration).show()
    }
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

  open fun runActivityWithArgs(clazz: Class<*>, args: Bundle, finishCurrentActivity: Boolean = false) {
    val intent = Intent(this, clazz)
    intent.putExtras(args)
    startActivity(intent)

    if (finishCurrentActivity) {
      finish()
    }
  }

  fun showErrorCodeToast(errorCode: ErrorCode) {
    val errorMessage = errorCode.getErrorMessage()

    Timber.tag(TAG).e(errorMessage)
    onShowToast(errorMessage, Toast.LENGTH_SHORT)
  }

  protected abstract fun getContentView(): Int
  protected abstract fun onActivityCreate(savedInstanceState: Bundle?, intent: Intent)
  protected abstract fun onActivityStart()
  protected abstract fun onActivityStop()
  protected abstract fun resolveDaggerDependency()
}