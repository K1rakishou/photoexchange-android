package com.kirakishou.photoexchange.ui.activity

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import butterknife.BindView
import com.davemorrissey.labs.subscaleview.ImageSource
import com.davemorrissey.labs.subscaleview.SubsamplingScaleImageView
import com.kirakishou.fixmypc.photoexchange.R
import com.kirakishou.photoexchange.PhotoExchangeApplication
import com.kirakishou.photoexchange.base.BaseActivityWithoutViewModel
import com.kirakishou.photoexchange.di.component.DaggerViewPhotoFullSizeActivityComponent
import com.kirakishou.photoexchange.helper.CompositeJob
import com.kirakishou.photoexchange.helper.ImageLoader
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.rxkotlin.plusAssign
import io.reactivex.schedulers.Schedulers
import java.io.File
import javax.inject.Inject

class ViewPhotoFullSizeActivity : BaseActivityWithoutViewModel() {

    @BindView(R.id.full_size_image_view)
    lateinit var fullSizeImageView: SubsamplingScaleImageView

    @Inject
    lateinit var imageLoader: ImageLoader

    override fun getContentView(): Int = R.layout.activity_view_photo_full_size

    override fun onActivityCreate(savedInstanceState: Bundle?, intent: Intent) {
        loadFullSizePhoto(intent)
    }

    override fun onActivityDestroy() {
    }

    private fun loadFullSizePhoto(intent: Intent) {
        val photoName = intent.getStringExtra("photo_name")

        compositeDisposable += imageLoader.downloadPhotoAsync(photoName, ImageLoader.PhotoSize.Original)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .doOnSuccess { photoFile ->
                    fullSizeImageView.setImage(ImageSource.uri(Uri.fromFile(photoFile)))
                }
                .subscribe()
    }

    override fun resolveDaggerDependency() {
        DaggerViewPhotoFullSizeActivityComponent.builder()
                .applicationComponent(PhotoExchangeApplication.applicationComponent)
                .build()
                .inject(this)
    }
}
