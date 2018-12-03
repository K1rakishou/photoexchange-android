package com.kirakishou.photoexchange.ui.epoxy.row

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.constraintlayout.widget.ConstraintLayout
import com.airbnb.epoxy.CallbackProp
import com.airbnb.epoxy.ModelProp
import com.airbnb.epoxy.ModelView
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.request.RequestOptions
import com.kirakishou.fixmypc.photoexchange.R
import com.kirakishou.photoexchange.di.module.GlideApp
import com.kirakishou.photoexchange.mvp.model.photo.ReceivedPhoto
import com.kirakishou.photoexchange.helper.Constants

@ModelView(autoLayout = ModelView.Size.MATCH_WIDTH_WRAP_HEIGHT)
class ReceivedPhotoRow @JvmOverloads constructor(
  context: Context,
  attrs: AttributeSet? = null,
  defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr) {
  private val photoView: ImageView
  private val staticMapView: ImageView
  private val clickView: ConstraintLayout

  init {
    inflate(context, R.layout.epoxy_adapter_item_received_photo, this)

    photoView = findViewById(R.id.photo_view)
    staticMapView = findViewById(R.id.static_map_view)
    clickView = findViewById(R.id.click_view)

    orientation = VERTICAL
  }

  @ModelProp
  fun setPhoto(photo: ReceivedPhoto) {
    if (photo.showPhoto) {
      staticMapView.visibility = View.GONE
      photoView.visibility = View.VISIBLE

      val fullUrl = "${Constants.BASE_PHOTOS_URL}/${photo.receivedPhotoName}/${photo.photoSize.value}"

      GlideApp.with(context)
        .load(fullUrl)
        .diskCacheStrategy(DiskCacheStrategy.AUTOMATIC)
        .apply(RequestOptions().centerCrop())
        .into(photoView)
    } else {
      staticMapView.visibility = View.VISIBLE
      photoView.visibility = View.GONE

      val fullUrl = "${Constants.BASE_STATIC_MAP_URL}/${photo.uploadedPhotoName}"

      GlideApp.with(context)
        .load(fullUrl)
        .diskCacheStrategy(DiskCacheStrategy.AUTOMATIC)
        .apply(RequestOptions().centerCrop())
        .into(staticMapView)
    }
  }

  @CallbackProp
  fun setCallback(listener: OnClickListener?) {
    clickView.setOnClickListener(listener)
  }
}