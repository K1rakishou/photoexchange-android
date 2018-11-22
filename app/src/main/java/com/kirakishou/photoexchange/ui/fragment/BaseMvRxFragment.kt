package com.kirakishou.photoexchange.ui.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.CallSuper
import androidx.recyclerview.widget.GridLayoutManager
import com.airbnb.epoxy.AsyncEpoxyController
import com.airbnb.epoxy.EpoxyController
import com.airbnb.epoxy.EpoxyRecyclerView
import com.airbnb.mvrx.BaseMvRxFragment
import com.kirakishou.fixmypc.photoexchange.BuildConfig
import com.kirakishou.fixmypc.photoexchange.R

abstract class BaseMvRxFragment : BaseMvRxFragment() {
  private val spanCount = 1
  protected lateinit var recyclerView: EpoxyRecyclerView

  protected val epoxyController: EpoxyController by lazy {
    buildEpoxyController().apply {
      isDebugLoggingEnabled = BuildConfig.DEBUG
    }
  }

  @CallSuper
  override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
    return inflater.inflate(getFragmentLayoutId(), container, false).apply {
      val recyclerViewInstance = findViewById<EpoxyRecyclerView>(R.id.recycler_view)
      if (recyclerViewInstance == null) {
        throw IllegalStateException("BaseMvRxFragment requires fragment to contain " +
          "RecyclerView with id = R.id.recycler_view!")
      }


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
    recyclerView.requestModelBuild()
  }

  protected fun simpleController(
    build: EpoxyController.() -> Unit
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