package com.kirakishou.photoexchange.ui.activity

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.ImageView
import butterknife.BindView
import com.davemorrissey.labs.subscaleview.ImageSource
import com.davemorrissey.labs.subscaleview.SubsamplingScaleImageView
import com.jakewharton.rxbinding2.view.RxView
import com.kirakishou.fixmypc.photoexchange.R
import com.kirakishou.photoexchange.PhotoExchangeApplication
import com.kirakishou.photoexchange.base.BaseActivity
import com.kirakishou.photoexchange.di.component.DaggerViewPhotoFullSizeActivityComponent
import com.kirakishou.photoexchange.helper.ImageLoader
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.rxkotlin.plusAssign
import io.reactivex.schedulers.Schedulers
import javax.inject.Inject

class ViewPhotoFullSizeActivity : BaseActivity<Nothing>() {

    @BindView(R.id.full_size_image_view)
    lateinit var fullSizeImageView: SubsamplingScaleImageView

    @BindView(R.id.iv_close_button)
    lateinit var ivCloseButton: ImageView

    @Inject
    lateinit var imageLoader: ImageLoader

    override fun getContentView(): Int = R.layout.activity_view_photo_full_size
    override fun initViewModel(): Nothing? = null

    override fun onInitRx() {
        compositeDisposable += RxView.clicks(ivCloseButton)
                .subscribeOn(AndroidSchedulers.mainThread())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({ finish() })
    }

    override fun onActivityCreate(savedInstanceState: Bundle?, intent: Intent) {
        loadFullSizePhoto(intent)
    }

    override fun onActivityDestroy() {
    }

    private fun loadFullSizePhoto(intent: Intent) {
        val photoName = intent.getStringExtra("photo_name")
        checkNotNull(photoName)

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
