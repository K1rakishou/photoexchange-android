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
import com.kirakishou.photoexchange.helper.CompositeJob
import kotlinx.coroutines.experimental.android.UI
import kotlinx.coroutines.experimental.async
import java.io.File

class ViewPhotoFullSizeActivity : BaseActivityWithoutViewModel() {

    @BindView(R.id.full_size_image_view)
    lateinit var fullSizeImageView: SubsamplingScaleImageView

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
        val fullPath = "${PhotoExchangeApplication.baseUrl}v1/api/get_photo/$photoName/o"

        compositeJob += async {
            photoFile = Glide.with(applicationContext)
                    .downloadOnly()
                    .load(fullPath)
                    .submit()
                    .get()

            compositeJob += async(UI) {
                fullSizeImageView.setImage(ImageSource.uri(Uri.fromFile(photoFile)))
            }
        }
    }

    private fun deleteFile() {
        if (photoFile != null) {
            if (photoFile!!.exists()) {
                photoFile!!.delete()
            }
        }
    }

    override fun resolveDaggerDependency() {
    }

}
