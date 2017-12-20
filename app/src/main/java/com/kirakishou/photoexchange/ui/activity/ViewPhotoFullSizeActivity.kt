package com.kirakishou.photoexchange.ui.activity

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import butterknife.BindView
import com.bumptech.glide.Glide
import com.davemorrissey.labs.subscaleview.ImageSource
import com.davemorrissey.labs.subscaleview.SubsamplingScaleImageView
import com.kirakishou.fixmypc.photoexchange.R
import com.kirakishou.photoexchange.PhotoExchangeApplication
import com.kirakishou.photoexchange.base.BaseActivityWithoutViewModel
import com.kirakishou.photoexchange.di.component.DaggerTakePhotoActivityComponent
import com.kirakishou.photoexchange.di.component.DaggerViewPhotoFullSizeActivityComponent
import com.kirakishou.photoexchange.helper.CompositeJob
import com.kirakishou.photoexchange.helper.ImageLoader
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.rxkotlin.plusAssign
import io.reactivex.schedulers.Schedulers
import kotlinx.coroutines.experimental.android.UI
import kotlinx.coroutines.experimental.async
import java.io.File
import javax.inject.Inject

class ViewPhotoFullSizeActivity : BaseActivityWithoutViewModel() {

    @BindView(R.id.full_size_image_view)
    lateinit var fullSizeImageView: SubsamplingScaleImageView

    @Inject
    lateinit var imageLoader: ImageLoader

    private var photoFile: File? = null
    private val compositeJob = CompositeJob()

    override fun getContentView(): Int = R.layout.activity_view_photo_full_size

    override fun onActivityCreate(savedInstanceState: Bundle?, intent: Intent) {
        loadFullSizePhoto(intent)
    }

    override fun onActivityDestroy() {
        deleteFile()
        compositeJob.cancelAll()
    }

    private fun loadFullSizePhoto(intent: Intent) {
        val photoName = intent.getStringExtra("photo_name")
        val fullUrl = "${PhotoExchangeApplication.baseUrl}v1/api/get_photo/$photoName/o"

        compositeDisposable += imageLoader.downloadImageAsync(fullUrl)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .doOnSuccess { imageFile ->
                    photoFile = imageFile
                    fullSizeImageView.setImage(ImageSource.uri(Uri.fromFile(photoFile)))
                }
                .subscribe()
    }

    private fun deleteFile() {
        if (photoFile != null) {
            if (photoFile!!.exists()) {
                photoFile!!.delete()
            }
        }
    }

    override fun resolveDaggerDependency() {
        DaggerViewPhotoFullSizeActivityComponent.builder()
                .applicationComponent(PhotoExchangeApplication.applicationComponent)
                .build()
                .inject(this)
    }
}
