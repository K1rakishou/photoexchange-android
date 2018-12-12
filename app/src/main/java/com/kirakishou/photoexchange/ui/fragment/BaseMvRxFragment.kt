package com.kirakishou.photoexchange.ui.fragment

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.CallSuper
import androidx.recyclerview.widget.GridLayoutManager
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.airbnb.epoxy.AsyncEpoxyController
import com.airbnb.epoxy.EpoxyController
import com.airbnb.epoxy.EpoxyRecyclerView
import com.airbnb.mvrx.BaseMvRxFragment
import com.kirakishou.fixmypc.photoexchange.BuildConfig
import com.kirakishou.fixmypc.photoexchange.R
import com.kirakishou.photoexchange.helper.RxLifecycle
import io.reactivex.disposables.CompositeDisposable
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.*
import kotlin.coroutines.CoroutineContext

abstract class BaseMvRxFragment : BaseMvRxFragment(), CoroutineScope {
  private val spanCount = 1

  private val invalidationActor: SendChannel<Unit>

  protected val compositeDisposable = CompositeDisposable()
  protected val lifecycle = RxLifecycle(this)

  private val job = Job()
  protected lateinit var recyclerView: EpoxyRecyclerView
  protected lateinit var swipeRefreshLayout: SwipeRefreshLayout

  override val coroutineContext: CoroutineContext
    get() = job + Dispatchers.Main

  protected val epoxyController: EpoxyController by lazy {
    buildEpoxyController().apply {
      isDebugLoggingEnabled = BuildConfig.DEBUG
    }
  }

  init {
    invalidationActor = actor(capacity = Channel.RENDEZVOUS) {
      consumeEach {
        postInvalidate()
      }
    }
  }

  override fun onAttach(context: Context?) {
    super.onAttach(context)
    lifecycle.start()
  }

  override fun onDetach() {
    super.onDetach()
    lifecycle.stop()
  }

  @CallSuper
  override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
    return inflater.inflate(getFragmentLayoutId(), container, false).apply {
      val recyclerViewInstance = findViewById<EpoxyRecyclerView>(R.id.recycler_view)
      if (recyclerViewInstance == null) {
        throw IllegalStateException("BaseMvRxFragment requires fragment to contain " +
          "RecyclerView with id = R.id.recycler_view!")
      }

      val srl = findViewById<SwipeRefreshLayout>(R.id.swipe_refresh_layout)
      if (srl == null) {
        throw IllegalStateException("Fragment should contain swipeRefreshLayout")
      }

      swipeRefreshLayout = srl

      recyclerView = recyclerViewInstance.apply {
        epoxyController.spanCount = spanCount

        layoutManager = GridLayoutManager(activity, spanCount).apply {
          spanSizeLookup = epoxyController.spanSizeLookup
        }

        setController(epoxyController)
      }
    }
  }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)

    resolveDaggerDependency()
  }

  @CallSuper
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    epoxyController.onRestoreInstanceState(savedInstanceState)
  }

  @CallSuper
  override fun onSaveInstanceState(outState: Bundle) {
    super.onSaveInstanceState(outState)
    epoxyController.onSaveInstanceState(outState)
  }

  @CallSuper
  override fun onDestroyView() {
    epoxyController.cancelPendingModelBuild()

    compositeDisposable.clear()
    job.cancelChildren()

    super.onDestroyView()
  }

  protected fun doInvalidate() {
    invalidationActor.offer(Unit)
  }

  @CallSuper
  override fun invalidate() {
    recyclerView.requestModelBuild()
  }

  protected fun simpleController(
    build: AsyncEpoxyController.() -> Unit
  ): AsyncEpoxyController {
    return object : AsyncEpoxyController() {
      override fun buildModels() {
        if (view == null || isRemoving) {
          return
        }

        build()
      }
    }
  }

  protected abstract fun getFragmentLayoutId(): Int
  protected abstract fun buildEpoxyController(): AsyncEpoxyController
  protected abstract fun resolveDaggerDependency()
}