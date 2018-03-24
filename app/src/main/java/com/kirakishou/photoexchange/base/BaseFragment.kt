package com.kirakishou.photoexchange.base

import android.arch.lifecycle.LifecycleRegistry
import android.arch.lifecycle.ViewModel
import android.content.Context
import android.os.Bundle
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import butterknife.ButterKnife
import butterknife.Unbinder
import com.kirakishou.photoexchange.PhotoExchangeApplication
import io.reactivex.disposables.CompositeDisposable

/**
 * Created by kirakishou on 11/7/2017.
 */

abstract class BaseFragment<T : BaseViewModel<*>> : Fragment() {

    protected val registry by lazy {
        LifecycleRegistry(this)
    }

    override fun getLifecycle(): LifecycleRegistry = registry

    private var viewModel: T? = null
    private lateinit var unBinder: Unbinder
    protected val compositeDisposable = CompositeDisposable()

    //for tests
    fun setViewModel(viewModel: T) {
        this.viewModel = viewModel
    }

    fun getViewModel(): T {
        if (viewModel == null) {
            throw IllegalStateException("Cannot call get viewModel from the activity that has no viewModel!")
        }

        return viewModel!!
    }

    override fun onAttach(context: Context?) {
        super.onAttach(context)

        resolveDaggerDependency()
        viewModel = initViewModel()
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
        super.onDestroyView()

        onFragmentViewDestroy()
        unBinder.unbind()
    }

    override fun onDetach() {
        super.onDetach()

        compositeDisposable.clear()

        PhotoExchangeApplication.watch(this, this::class.simpleName)
    }

    protected abstract fun initViewModel(): T?
    protected abstract fun getContentView(): Int
    protected abstract fun onFragmentViewCreated(savedInstanceState: Bundle?)
    protected abstract fun onFragmentViewDestroy()
    protected abstract fun resolveDaggerDependency()
}