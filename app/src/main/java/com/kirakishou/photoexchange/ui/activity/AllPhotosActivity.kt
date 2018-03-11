package com.kirakishou.photoexchange.ui.activity

import android.arch.lifecycle.ViewModelProviders
import android.content.Intent
import android.os.Bundle
import com.kirakishou.fixmypc.photoexchange.R
import com.kirakishou.photoexchange.PhotoExchangeApplication
import com.kirakishou.photoexchange.base.BaseActivity
import com.kirakishou.photoexchange.di.module.AllPhotosActivityModule
import com.kirakishou.photoexchange.mvp.view.AllPhotosActivityView
import com.kirakishou.photoexchange.mvp.viewmodel.AllPhotosActivityViewModel
import com.kirakishou.photoexchange.mvp.viewmodel.factory.AllPhotosActivityViewModelFactory
import javax.inject.Inject

class AllPhotosActivity : BaseActivity<AllPhotosActivityViewModel>(), AllPhotosActivityView {

    @Inject
    lateinit var viewModelFactory: AllPhotosActivityViewModelFactory

    override fun initViewModel(): AllPhotosActivityViewModel? {
        return ViewModelProviders.of(this, viewModelFactory).get(AllPhotosActivityViewModel::class.java)
    }

    override fun getContentView(): Int = R.layout.activity_all_photos

    override fun onActivityCreate(savedInstanceState: Bundle?, intent: Intent) {
    }

    override fun onActivityDestroy() {
        getViewModel().detach()
    }

    override fun showToast(message: String, duration: Int) {
        onShowToast(message, duration)
    }

    override fun resolveDaggerDependency() {
        (application as PhotoExchangeApplication).applicationComponent
            .plus(AllPhotosActivityModule(this))
            .inject(this)
    }

}
