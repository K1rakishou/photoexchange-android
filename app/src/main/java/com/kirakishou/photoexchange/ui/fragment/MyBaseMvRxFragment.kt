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
import com.kirakishou.photoexchange.PhotoExchangeApplication
import com.kirakishou.photoexchange.helper.Constants
import com.kirakishou.photoexchange.helper.util.AndroidUtils
import io.reactivex.disposables.CompositeDisposable
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.*
import kotlin.coroutines.CoroutineContext

abstract class MyBaseMvRxFragment : BaseMvRxFragment(), CoroutineScope {
  private val invalidationActor: SendChannel<Unit>

  protected val compositeDisposable = CompositeDisposable()

  private var job = Job()
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

  override fun onDetach() {
    super.onDetach()

    job.cancel()
    job = Job()

    compositeDisposable.clear()

    PhotoExchangeApplication.watch(this, this::class.simpleName)
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

    super.onDestroyView()
  }

  @CallSuper
  override fun invalidate() {
    // We don't call recyclerView.requestModelBuild() here because we manually subscribe to only
    // those parts of the state that we need in order to rebuild the epoxy models.
    // Otherwise the rebuilding process would start every time even if we don't want it
    // (e.g. when we update lastSeenColorPosition in the state we don't want to start the rebuilding process).
    // By subscribing manually and then manually rebuilding epoxy we can store more things in
    // the state while not being afraid of triggering recyclerview redrawing  with every state change.
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