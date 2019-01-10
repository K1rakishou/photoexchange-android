package com.kirakishou.photoexchange.ui.activity

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.LifecycleRegistry
import butterknife.ButterKnife
import butterknife.Unbinder
import com.kirakishou.photoexchange.PhotoExchangeApplication
import io.reactivex.disposables.CompositeDisposable
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlin.coroutines.CoroutineContext


/**
 * Created by kirakishou on 7/20/2017.
 */
abstract class BaseActivity : AppCompatActivity(), CoroutineScope {
  private val TAG = "${this::class.java}"
  protected val compositeDisposable = CompositeDisposable()

  private var job = Job()
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
    onActivityStart()
  }

  override fun onStop() {
    onActivityStop()

    job.cancel()
    job = Job()

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

  open fun runActivity(clazz: Class<*>, finishCurrentActivity: Boolean = false) {
    val intent = Intent(this, clazz)
    startActivity(intent)

    if (finishCurrentActivity) {
      finish()
    }
  }

  protected abstract fun getContentView(): Int
  protected abstract fun onActivityCreate(savedInstanceState: Bundle?, intent: Intent)
  protected abstract fun onActivityStart()
  protected abstract fun onActivityStop()
  protected abstract fun resolveDaggerDependency()
}