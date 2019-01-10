package com.kirakishou.photoexchange.ui.fragment

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
import com.kirakishou.photoexchange.helper.Constants
import com.kirakishou.photoexchange.helper.util.AndroidUtils
import io.reactivex.disposables.CompositeDisposable
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.*
import kotlin.coroutines.CoroutineContext

abstract class MyBaseMvRxFragment : BaseMvRxFragment(), CoroutineScope {
  private val invalidationActor: SendChannel<Unit>

  protected val compositeDisposable = CompositeDisposable()

  private val job = Job()
  lateinit var recyclerView: EpoxyRecyclerView
  lateinit var swipeRefreshLayout: SwipeRefreshLayout

  override val coroutineContext: CoroutineContext
    get() = job + Dispatchers.Main

  val spanCount by lazy {
    AndroidUtils.calculateNoOfColumns(requireContext(), Constants.DEFAULT_ADAPTER_ITEM_WIDTH)
  }

  val epoxyController: EpoxyController by lazy {
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

  @CallSuper
  override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
    return inflater.inflate(getFragmentLayoutId(), container, false).apply {
      val recyclerViewInstance = findViewById<EpoxyRecyclerView>(R.id.recycler_view)
      if (recyclerViewInstance == null) {
        throw IllegalStateException("MyBaseMvRxFragment requires fragment to contain " +
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

  // Notifies to actor to start models rebuilding process
  protected fun doInvalidate() {
    invalidationActor.offer(Unit)
  }

  // This is called internally by MvRx
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