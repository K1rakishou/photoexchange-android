package com.kirakishou.photoexchange.helper

import android.content.Context
import android.widget.ImageView
import androidx.swiperefreshlayout.widget.CircularProgressDrawable
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.request.RequestOptions
import com.kirakishou.photoexchange.di.module.GlideApp
import com.kirakishou.photoexchange.helper.util.NetUtils
import com.kirakishou.photoexchange.mvp.model.PhotoSize
import timber.log.Timber
import java.io.File
import javax.inject.Inject


/**
 * Created by kirakishou on 12/20/2017.
 */
class ImageLoader
@Inject constructor(
  private val context: Context,
  private val netUtils: NetUtils
) {
  private val TAG = "ImageLoader"

  private val photoSize by lazy {
    val density = context.resources.displayMetrics.density

    if (density < 2.0) {
      return@lazy PhotoSize.Small
    } else if (density >= 2.0 && density < 3.0) {
      return@lazy PhotoSize.Medium
    } else {
      return@lazy PhotoSize.Big
    }
  }

  private fun createProgressDrawable(): CircularProgressDrawable {
    return CircularProgressDrawable(context).apply {
      strokeWidth = 10f
      centerRadius = 50f
      start()
    }
  }

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
  fun loadPhotoFromNetInto(photoName: String, view: ImageView) {
    val fullUrl = "${Constants.BASE_PHOTOS_URL}/${photoName}/${photoSize.value}"

    if (!netUtils.canLoadImages()) {
      //TODO: load default "no wifi" image
      Timber.tag(TAG).d("No wifi")

      GlideApp.with(context)
        .load(fullUrl)
        .onlyRetrieveFromCache(true)
        .placeholder(createProgressDrawable())
        .apply(RequestOptions().centerCrop())
        .into(view)

      return
    }

    GlideApp.with(context)
      .load(fullUrl)
      .placeholder(createProgressDrawable())
      .diskCacheStrategy(DiskCacheStrategy.AUTOMATIC)
      .apply(RequestOptions().centerCrop())
      .into(view)
  }

  fun loadStaticMapImageFromNetInto(photoName: String, view: ImageView) {
    val fullUrl = "${Constants.BASE_STATIC_MAP_URL}/${photoName}"

    if (!netUtils.canLoadImages()) {
      //TODO: load default "no wifi" image
      Timber.tag(TAG).d("No wifi")

      GlideApp.with(context)
        .load(fullUrl)
        .onlyRetrieveFromCache(true)
        .placeholder(createProgressDrawable())
        .apply(RequestOptions().centerCrop())
        .into(view)

      return
    }

    GlideApp.with(context)
      .load(fullUrl)
      .placeholder(createProgressDrawable())
      .diskCacheStrategy(DiskCacheStrategy.AUTOMATIC)
      .apply(RequestOptions().centerCrop())
      .into(view)
  }
}