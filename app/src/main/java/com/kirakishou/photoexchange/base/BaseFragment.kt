package com.kirakishou.photoexchange.base

import android.arch.lifecycle.LifecycleRegistry
import android.arch.lifecycle.ViewModel
import android.os.Bundle
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import butterknife.ButterKnife
import butterknife.Unbinder
import io.reactivex.disposables.CompositeDisposable

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

    protected fun getViewModel(): T {
        return viewModel
    }

    @Suppress("UNCHECKED_CAST")
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        super.onCreateView(inflater, container, savedInstanceState)

        resolveDaggerDependency()
        viewModel = initViewModel()

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
        compositeDisposable.clear()
        onFragmentViewDestroy()

        unBinder.unbind()
        super.onDestroyView()
    }

    protected abstract fun initViewModel(): T
    protected abstract fun getContentView(): Int
    protected abstract fun onFragmentViewCreated(savedInstanceState: Bundle?)
    protected abstract fun onFragmentViewDestroy()
    protected abstract fun resolveDaggerDependency()
}