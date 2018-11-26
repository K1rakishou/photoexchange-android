package com.kirakishou.photoexchange.ui.adapter.epoxy

import android.content.Context
import android.graphics.drawable.ColorDrawable
import android.util.AttributeSet
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import com.airbnb.epoxy.ModelProp
import com.airbnb.epoxy.ModelView
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.request.RequestOptions
import com.kirakishou.fixmypc.photoexchange.R
import com.kirakishou.photoexchange.di.module.GlideApp
import com.kirakishou.photoexchange.mvp.model.photo.UploadedPhoto
import com.kirakishou.photoexchange.mvp.model.other.Constants

@ModelView(autoLayout = ModelView.Size.MATCH_WIDTH_WRAP_HEIGHT)
class UploadedPhotoRow @JvmOverloads constructor(
  context: Context,
  attrs: AttributeSet? = null,
  defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr) {
  private val photoIdTextView: TextView
  private val photoView: ImageView
  private val photoUploadingStateIndicator: View
  private val receivedIconImageView: ImageView

  init {
    inflate(context, R.layout.epoxy_adapter_item_uploaded_photo, this)

    photoIdTextView = findViewById(R.id.photo_id_text_view)
    photoView = findViewById(R.id.photo_view)
    photoUploadingStateIndicator = findViewById<View>(R.id.photo_uploading_state_indicator)
    receivedIconImageView = findViewById(R.id.received_icon_image_view)

    orientation = VERTICAL
  }

  @ModelProp
  fun setPhoto(uploadedPhoto: UploadedPhoto) {
    val fullUrl = "${Constants.BASE_PHOTOS_URL}/${uploadedPhoto.photoName}/${uploadedPhoto.photoSize.value}"

    val drawable = if (uploadedPhoto.receiverInfo == null) {
      context.getDrawable(R.drawable.ic_done)
    } else {
      context.getDrawable(R.drawable.ic_done_all)
    }

    val color = if (uploadedPhoto.receiverInfo == null) {
      ColorDrawable(context.resources.getColor(R.color.photo_state_uploaded_color))
    } else {
      ColorDrawable(context.resources.getColor(R.color.photo_state_exchanged_color))
    }

    photoUploadingStateIndicator.background = color
    receivedIconImageView.visibility = View.VISIBLE
    receivedIconImageView.setImageDrawable(drawable)
    photoIdTextView.text = uploadedPhoto.photoId.toString()

    GlideApp.with(context)
      .load(fullUrl)
      .diskCacheStrategy(DiskCacheStrategy.AUTOMATIC)
      .apply(RequestOptions().centerCrop())
      .into(photoView)
  }
}