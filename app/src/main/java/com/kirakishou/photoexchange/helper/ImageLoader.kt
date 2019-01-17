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
import com.kirakishou.photoexchange.mvrx.model.PhotoSize
import com.kirakishou.photoexchange.ui.widget.TextDrawable
import java.io.File
import javax.inject.Inject
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine


/**
 * Created by kirakishou on 12/20/2017.
 */
open class ImageLoader
@Inject constructor(
  private val context: Context,
  private val netUtils: NetUtils
) {
  private val TAG = "ImageLoader"

  private val noUnmeteredNetworkAvailableText = "No unmetered network available."
  private val couldNotLoadPhotoText = "Error. Could not load photo."
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

  open fun loadPhotoFromDiskInto(imageFile: File, view: ImageView) {
    GlideApp.with(context)
      //we do not need to cache this image
      .applyDefaultRequestOptions(RequestOptions.diskCacheStrategyOf(DiskCacheStrategy.NONE))
      .load(imageFile)
      .apply(RequestOptions().centerCrop())
      .into(view)
  }

  open suspend fun loadPhotoFromNetInto(photoName: String, view: ImageView) {
    val fullUrl = "${Constants.BASE_PHOTOS_URL}/${photoName}/${photoSize.value}"

    if (!netUtils.canLoadImages()) {
      if (isCached(fullUrl)) {
        GlideApp.with(context)
          .load(fullUrl)
          .onlyRetrieveFromCache(true)
          .placeholder(createProgressDrawable())
          .error(createErrorTextDrawable(view))
          .apply(RequestOptions().centerCrop())
          .into(view)
      } else {
        GlideApp.with(context)
          .load(createNoUnmeteredNetworkTextDrawable(view))
          .into(view)
      }
    } else {
      GlideApp.with(context)
        .load(fullUrl)
        .placeholder(createProgressDrawable())
        .diskCacheStrategy(DiskCacheStrategy.AUTOMATIC)
        .error(createErrorTextDrawable(view))
        .apply(RequestOptions().centerCrop())
        .into(view)
    }
  }

  open suspend fun loadStaticMapImageFromNetInto(photoName: String, view: ImageView) {
    val fullUrl = "${Constants.BASE_STATIC_MAP_URL}/${photoName}"

    if (!netUtils.canLoadImages()) {
      if (isCached(fullUrl)) {
        GlideApp.with(context)
          .load(fullUrl)
          .onlyRetrieveFromCache(true)
          .placeholder(createProgressDrawable())
          .error(createErrorTextDrawable(view))
          .apply(RequestOptions().centerCrop())
          .into(view)
      } else {
        GlideApp.with(context)
          .load(createNoUnmeteredNetworkTextDrawable(view))
          .into(view)
      }
    } else {
      GlideApp.with(context)
        .load(fullUrl)
        .placeholder(createProgressDrawable())
        .diskCacheStrategy(DiskCacheStrategy.AUTOMATIC)
        .error(createErrorTextDrawable(view))
        .apply(RequestOptions().centerCrop())
        .into(view)
    }
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

  private fun createProgressDrawable(): CircularProgressDrawable {
    return CircularProgressDrawable(context).apply {
      strokeWidth = 10f
      centerRadius = 50f
      start()
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

  private fun createErrorTextDrawable(view: ImageView): TextDrawable {
    return TextDrawable(
      couldNotLoadPhotoText,
      Color.parseColor(textColor),
      AndroidUtils.spToPx(context, 18f),
      view.width,
      view.height
    )
  }
}