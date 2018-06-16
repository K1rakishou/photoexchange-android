package com.kirakishou.photoexchange.helper

import android.content.Context
import android.widget.ImageView
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.RequestOptions
import com.bumptech.glide.request.target.Target
import com.kirakishou.photoexchange.PhotoExchangeApplication
import com.kirakishou.photoexchange.di.module.GlideApp
import io.reactivex.Single
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
    private val baseStaticMapUrl = "${PhotoExchangeApplication.baseUrl}v1/api/get_static_map"

    fun loadPhotoFromDiskInto(imageFile: File, view: ImageView) {
        GlideApp.with(context)
            //we do not need to cache this image
            .applyDefaultRequestOptions(RequestOptions.diskCacheStrategyOf(DiskCacheStrategy.NONE))
            .load(imageFile)
            .apply(RequestOptions().centerCrop())
            .into(view)
    }

    /**
     * We are pre-loading photos requested by this method in advance, so generally this method should load them from disk.
     * But in some rare cases (when photo could not be pre-loaded for some reason) this method will make a request to the server
     * */
    fun loadPhotoFromNetInto(photoName: String, photoSize: PhotoSize, view: ImageView) {
        val fullUrl = "$basePhotosUrl/$photoName/${photoSize.value}"

        GlideApp.with(context)
            .load(fullUrl)
            .diskCacheStrategy(DiskCacheStrategy.DATA)
            .apply(RequestOptions().centerCrop())
            .into(view)
    }

    fun loadStaticMapImageFromNetInto(photoName: String, view: ImageView) {
        val fullUrl = "$baseStaticMapUrl/$photoName"

        GlideApp.with(context)
            .load(fullUrl)
            .diskCacheStrategy(DiskCacheStrategy.DATA)
            .apply(RequestOptions().centerCrop())
            .into(view)
    }

    fun preloadImageFromNetAsync(photoName: String, photoSize: PhotoSize): Single<Boolean> {
        return Single.create { emitter ->
            val fullUrl = "$basePhotosUrl/$photoName/${photoSize.value}"

            GlideApp.with(context)
                .download(fullUrl)
                .listener(object : RequestListener<File> {
                    override fun onLoadFailed(error: GlideException?, model: Any?, target: Target<File>?, isFirstResource: Boolean): Boolean {
                        emitter.onSuccess(false)
                        return false
                    }

                    override fun onResourceReady(resource: File, model: Any?, target: Target<File>?, dataSource: DataSource?, isFirstResource: Boolean): Boolean {
                        emitter.onSuccess(true)
                        return false
                    }
                })
                .preload()
        }
    }

    enum class PhotoSize(val value: String) {
        Big("b"),
        Small("s")
    }
}