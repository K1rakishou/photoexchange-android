package com.kirakishou.photoexchange.helper

import android.widget.ImageView
import com.bumptech.glide.Glide
import com.bumptech.glide.RequestManager
import com.bumptech.glide.annotation.GlideModule
import com.bumptech.glide.request.RequestOptions
import io.reactivex.Single
import java.io.File
import javax.inject.Inject

/**
 * Created by kirakishou on 12/20/2017.
 */
class ImageLoader
@Inject constructor(
        private val glideRequestManager: RequestManager
) {

    fun loadImageFromDiskInto(imageFile: File, view: ImageView) {
        glideRequestManager
                .load(imageFile)
                .apply(RequestOptions().centerCrop())
                .into(view)
    }

    fun loadImageFromNetInto(url: String, view: ImageView) {
        glideRequestManager
                .load(url)
                .apply(RequestOptions().centerCrop())
                .into(view)
    }

    fun downloadImageAsync(url: String): Single<File> {
        return Single.fromFuture(glideRequestManager
                .downloadOnly()
                .load(url)
                .submit())
    }
}