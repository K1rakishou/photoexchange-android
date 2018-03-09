package com.kirakishou.photoexchange.ui.activity

import android.arch.lifecycle.ViewModelProviders
import android.content.Intent
import android.os.Bundle
import com.kirakishou.fixmypc.photoexchange.R
import com.kirakishou.photoexchange.PhotoExchangeApplication
import com.kirakishou.photoexchange.base.BaseActivity
import com.kirakishou.photoexchange.di.module.ViewTakenPhotoActivityModule
import com.kirakishou.photoexchange.mvp.view.ViewTakenPhotoActivityView
import com.kirakishou.photoexchange.mvp.viewmodel.ViewTakenPhotoActivityViewModel
import com.kirakishou.photoexchange.mvp.viewmodel.factory.ViewTakenPhotoActivityViewModelFactory
import javax.inject.Inject

class ViewTakenPhotoActivity : BaseActivity<ViewTakenPhotoActivityViewModel>(), ViewTakenPhotoActivityView {

    @Inject
    lateinit var viewModelFactory: ViewTakenPhotoActivityViewModelFactory

    override fun initViewModel(): ViewTakenPhotoActivityViewModel? {
        return ViewModelProviders.of(this, viewModelFactory).get(ViewTakenPhotoActivityViewModel::class.java)
    }

    override fun getContentView(): Int = R.layout.activity_view_taken_photo

    override fun onActivityCreate(savedInstanceState: Bundle?, intent: Intent) {
    }

    override fun onActivityDestroy() {
    }

    override fun showToast(message: String, duration: Int) {
        onShowToast(message, duration)
    }

    override fun resolveDaggerDependency() {
        (application as PhotoExchangeApplication).applicationComponent
            .plus(ViewTakenPhotoActivityModule(this))
            .inject(this)
    }
}
