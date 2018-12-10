package com.kirakishou.photoexchange.helper

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.widget.ImageView
import androidx.swiperefreshlayout.widget.CircularProgressDrawable
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.RequestOptions
import com.bumptech.glide.request.target.Target
import com.kirakishou.photoexchange.di.module.GlideApp
import com.kirakishou.photoexchange.helper.util.AndroidUtils
import com.kirakishou.photoexchange.helper.util.NetUtils
import com.kirakishou.photoexchange.mvp.model.PhotoSize
import com.kirakishou.photoexchange.ui.widget.TextDrawable
import timber.log.Timber
import java.io.File
import javax.inject.Inject
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine


/**
 * Created by kirakishou on 12/20/2017.
 */
class ImageLoader
@Inject constructor(
  private val context: Context,
  private val netUtils: NetUtils
) {
  private val TAG = "ImageLoader"

  private val noUnmeteredNetworkAvailableText = "No unmetered network available"
  private val textColor = "#505050"

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

  suspend fun loadPhotoFromNetInto(photoName: String, view: ImageView) {
    val fullUrl = "${Constants.BASE_PHOTOS_URL}/${photoName}/${photoSize.value}"

    if (!netUtils.canLoadImages()) {
      Timber.tag(TAG).d("No wifi")

      if (isCached(fullUrl)) {
        GlideApp.with(context)
          .load(fullUrl)
          .onlyRetrieveFromCache(true)
          .placeholder(createProgressDrawable())
          .apply(RequestOptions().centerCrop())
          .into(view)
      } else {
        val textDrawable = createNoUnmeteredNetworkTextDrawable(view)

        GlideApp.with(context)
          .load(textDrawable)
          .into(view)
      }

      return
    }

    GlideApp.with(context)
      .load(fullUrl)
      .placeholder(createProgressDrawable())
      .diskCacheStrategy(DiskCacheStrategy.AUTOMATIC)
      .apply(RequestOptions().centerCrop())
      .into(view)
  }

  suspend fun loadStaticMapImageFromNetInto(photoName: String, view: ImageView) {
    val fullUrl = "${Constants.BASE_STATIC_MAP_URL}/${photoName}"

    if (!netUtils.canLoadImages()) {
      Timber.tag(TAG).d("No wifi")

      if (isCached(fullUrl)) {
        GlideApp.with(context)
          .load(fullUrl)
          .onlyRetrieveFromCache(true)
          .placeholder(createProgressDrawable())
          .apply(RequestOptions().centerCrop())
          .into(view)
      } else {
        val textDrawable = createNoUnmeteredNetworkTextDrawable(view)

        GlideApp.with(context)
          .load(textDrawable)
          .into(view)
      }

      return
    }

    GlideApp.with(context)
      .load(fullUrl)
      .placeholder(createProgressDrawable())
      .diskCacheStrategy(DiskCacheStrategy.AUTOMATIC)
      .apply(RequestOptions().centerCrop())
      .into(view)
  }

  private suspend fun isCached(fullUrl: String): Boolean {
    return suspendCoroutine { continuation ->
      GlideApp.with(context)
        .load(fullUrl)
        .onlyRetrieveFromCache(true)
        .placeholder(createProgressDrawable())
        .listener(object : RequestListener<Drawable?> {
          override fun onLoadFailed(
            e: GlideException?,
            model: Any?,
            target: Target<Drawable?>?,
            isFirstResource: Boolean
          ): Boolean {
            continuation.resume(false)
            return true
          }

          override fun onResourceReady(
            resource: Drawable?,
            model: Any?,
            target: Target<Drawable?>?,
            dataSource: DataSource?,
            isFirstResource: Boolean
          ): Boolean {
            continuation.resume(true)
            return true
          }
        })
        .submit()
    }
  }

  private fun createNoUnmeteredNetworkTextDrawable(view: ImageView): TextDrawable {
    return TextDrawable(
      noUnmeteredNetworkAvailableText,
      Color.parseColor(textColor),
      AndroidUtils.spToPx(context, 18f),
      view.width,
      view.height
    )
  }
}