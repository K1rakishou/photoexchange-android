package com.kirakishou.photoexchange.helper

import android.widget.ImageView
import com.bumptech.glide.RequestManager
import com.bumptech.glide.request.RequestOptions
import com.kirakishou.photoexchange.PhotoExchangeApplication
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
    private val basePhotosUrl = "${PhotoExchangeApplication.baseUrl}v1/api/get_photo/"

    fun loadImageFromDiskInto(imageFile: File, view: ImageView) {
        glideRequestManager
                .load(imageFile)
                .apply(RequestOptions().centerCrop())
                .into(view)
    }

    fun loadImageFromNetInto(photoName: String, photoSize: PhotoSize, view: ImageView) {
        val fullUrl = "$basePhotosUrl/$photoName/${photoSize.value}"
        glideRequestManager
                .load(fullUrl)
                .apply(RequestOptions().centerCrop())
                .into(view)
    }

    fun downloadPhotoAsync(photoName: String, photoSize: PhotoSize): Single<File> {
        val fullUrl = "$basePhotosUrl/$photoName/${photoSize.value}"
        val future = glideRequestManager
                .downloadOnly()
                .load(fullUrl)
                .submit()

        return Single.fromFuture(future)
    }

    enum class PhotoSize(val value: String) {
        Original("o"),
        Small("s")
    }
}