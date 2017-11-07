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
import com.kirakishou.photoexchange.mvvm.model.Fickle
import io.reactivex.disposables.CompositeDisposable

/**
 * Created by kirakishou on 11/7/2017.
 */

abstract class BaseFragment<T : ViewModel> : Fragment() {

    protected val mRegistry by lazy {
        LifecycleRegistry(this)
    }

    override fun getLifecycle(): LifecycleRegistry = mRegistry

    private lateinit var mUnBinder: Unbinder
    protected var mViewModel: Fickle<T> = Fickle.empty()
    protected val mCompositeDisposable = CompositeDisposable()

    protected fun getViewModel(): T {
        return mViewModel.get()
    }

    @Suppress("UNCHECKED_CAST")
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        super.onCreateView(inflater, container, savedInstanceState)

        resolveDaggerDependency()
        mViewModel = Fickle.of(initViewModel())

        val viewId = getContentView()
        val root = inflater.inflate(viewId, container, false)
        mUnBinder = ButterKnife.bind(this, root)

        return root
    }

    override fun onViewCreated(view: View?, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        onFragmentViewCreated(savedInstanceState)
    }

    override fun onDestroyView() {
        mCompositeDisposable.clear()
        onFragmentViewDestroy()

        mUnBinder.unbind()
        super.onDestroyView()
    }

    protected abstract fun initViewModel(): T?
    protected abstract fun getContentView(): Int
    protected abstract fun onFragmentViewCreated(savedInstanceState: Bundle?)
    protected abstract fun onFragmentViewDestroy()
    protected abstract fun resolveDaggerDependency()
}