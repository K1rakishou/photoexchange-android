package com.kirakishou.photoexchange.ui.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import butterknife.ButterKnife
import butterknife.Unbinder
import com.kirakishou.photoexchange.PhotoExchangeApplication
import io.reactivex.disposables.CompositeDisposable
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlin.coroutines.CoroutineContext

/**
 * Created by kirakishou on 11/7/2017.
 */

abstract class BaseFragment : Fragment(), CoroutineScope {
  protected val compositeDisposable = CompositeDisposable()
  private var job = Job()

  private lateinit var unBinder: Unbinder

  override val coroutineContext: CoroutineContext
    get() = job + Dispatchers.Main

  override fun onDetach() {
    super.onDetach()

    job.cancel()
    job = Job()

    compositeDisposable.clear()

    PhotoExchangeApplication.watch(this, this::class.simpleName)
  }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    retainInstance = false
  }

  @Suppress("UNCHECKED_CAST")
  override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
    super.onCreateView(inflater, container, savedInstanceState)

    val viewId = getContentView()
    val root = inflater.inflate(viewId, container, false)
    unBinder = ButterKnife.bind(this, root)

    return root
  }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)

    resolveDaggerDependency()
    onFragmentViewCreated(savedInstanceState)
  }

  override fun onDestroyView() {
    super.onDestroyView()

    onFragmentViewDestroy()
    unBinder.unbind()
  }

  protected abstract fun getContentView(): Int
  protected abstract fun onFragmentViewCreated(savedInstanceState: Bundle?)
  protected abstract fun onFragmentViewDestroy()
  protected abstract fun resolveDaggerDependency()
}