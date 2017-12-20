package com.kirakishou.photoexchange.base

import android.app.Activity
import android.arch.lifecycle.LifecycleRegistry
import android.arch.lifecycle.ViewModel
import android.content.Context
import android.os.Bundle
import android.support.annotation.CallSuper
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import butterknife.ButterKnife
import butterknife.Unbinder
import com.kirakishou.photoexchange.PhotoExchangeApplication
import com.kirakishou.photoexchange.helper.CompositeJob
import io.reactivex.disposables.CompositeDisposable
import timber.log.Timber

/**
 * Created by kirakishou on 11/7/2017.
 */

abstract class BaseFragment<out T : ViewModel> : Fragment() {

    protected val registry by lazy {
        LifecycleRegistry(this)
    }

    override fun getLifecycle(): LifecycleRegistry = registry

    private lateinit var unBinder: Unbinder
    private lateinit var viewModel: T
    protected val compositeDisposable = CompositeDisposable()
    protected val compositeJob = CompositeJob()

    protected fun getViewModel(): T {
        return viewModel
    }

    override fun onAttach(context: Context?) {
        super.onAttach(context)
        Timber.d("${this::class.java}.onAttach")

        resolveDaggerDependency()

        viewModel = initViewModel()
        initRx()
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

        onFragmentViewCreated(savedInstanceState)
    }

    override fun onDestroyView() {
        onFragmentViewDestroy()

        unBinder.unbind()
        super.onDestroyView()
    }

    override fun onDetach() {
        Timber.d("${this::class.java}.onDetach")

        compositeDisposable.clear()
        compositeJob.cancelAll()

        PhotoExchangeApplication.watch(this, this::class.simpleName)
        super.onDetach()
    }

    @CallSuper
    override fun onResume() {
        Timber.d("${this::class.java}.onResume")
        super.onResume()
    }

    @CallSuper
    override fun onPause() {
        Timber.d("${this::class.java}.onPause")
        super.onPause()
    }

    protected abstract fun initViewModel(): T
    protected abstract fun getContentView(): Int
    protected abstract fun initRx()
    protected abstract fun onFragmentViewCreated(savedInstanceState: Bundle?)
    protected abstract fun onFragmentViewDestroy()
    protected abstract fun resolveDaggerDependency()
}