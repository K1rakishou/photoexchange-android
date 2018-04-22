package com.kirakishou.photoexchange.helper

import android.content.Context
import android.widget.ImageView
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.request.RequestOptions
import com.kirakishou.photoexchange.PhotoExchangeApplication
import com.kirakishou.photoexchange.di.module.GlideApp
import java.io.File
import javax.inject.Inject


/**
 * Created by kirakishou on 12/20/2017.
 */
class ImageLoader
@Inject constructor(
    private val context: Context
) {
    private val basePhotosUrl = "${PhotoExchangeApplication.baseUrl}v1/api/get_photo"

    fun loadImageFromDiskInto(imageFile: File, view: ImageView) {
        GlideApp.with(context)
            //we do not need to cache this image
            .applyDefaultRequestOptions(RequestOptions.diskCacheStrategyOf(DiskCacheStrategy.NONE))
            .load(imageFile)
            .apply(RequestOptions().centerCrop())
            .into(view)
    }

    fun loadImageFromNetInto(photoName: String, photoSize: PhotoSize, view: ImageView) {
        val fullUrl = "$basePhotosUrl/$photoName/${photoSize.value}"
        GlideApp.with(context)
            .load(fullUrl)
            .diskCacheStrategy(DiskCacheStrategy.DATA)
            .apply(RequestOptions().centerCrop())
            .into(view)
    }

    enum class PhotoSize(val value: String) {
        Big("b"),
        Small("s")
    }
}