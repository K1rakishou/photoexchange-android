package com.kirakishou.photoexchange.ui.fragment


import android.arch.lifecycle.ViewModelProviders
import android.os.Bundle
import com.kirakishou.fixmypc.photoexchange.R
import com.kirakishou.photoexchange.PhotoExchangeApplication
import com.kirakishou.photoexchange.base.BaseFragment
import com.kirakishou.photoexchange.di.component.DaggerAllPhotoViewActivityComponent
import com.kirakishou.photoexchange.di.module.AllPhotoViewActivityModule
import com.kirakishou.photoexchange.mvvm.viewmodel.AllPhotoViewActivityViewModel
import com.kirakishou.photoexchange.mvvm.viewmodel.factory.AllPhotoViewActivityViewModelFactory
import com.kirakishou.photoexchange.ui.activity.AllPhotoViewActivity
import javax.inject.Inject

class SentPhotosListFragment : BaseFragment<AllPhotoViewActivityViewModel>() {

    @Inject
    lateinit var viewModelFactory: AllPhotoViewActivityViewModelFactory

    override fun initViewModel(): AllPhotoViewActivityViewModel {
        return ViewModelProviders.of(activity, viewModelFactory).get(AllPhotoViewActivityViewModel::class.java)
    }

    override fun getContentView(): Int = R.layout.fragment_sent_photos_list

    override fun onFragmentViewCreated(savedInstanceState: Bundle?) {
    }

    override fun onFragmentViewDestroy() {
    }

    override fun resolveDaggerDependency() {
        DaggerAllPhotoViewActivityComponent.builder()
                .applicationComponent(PhotoExchangeApplication.applicationComponent)
                .allPhotoViewActivityModule(AllPhotoViewActivityModule(activity as AllPhotoViewActivity))
                .build()
                .inject(this)
    }
}
