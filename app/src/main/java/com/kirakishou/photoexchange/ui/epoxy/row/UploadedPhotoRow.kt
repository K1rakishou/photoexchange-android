package com.kirakishou.photoexchange.ui.epoxy.row

import android.content.Context
import android.graphics.drawable.ColorDrawable
import android.util.AttributeSet
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import com.airbnb.epoxy.CallbackProp
import com.airbnb.epoxy.ModelProp
import com.airbnb.epoxy.ModelView
import com.kirakishou.fixmypc.photoexchange.R
import com.kirakishou.photoexchange.mvrx.model.photo.UploadedPhoto

@ModelView(autoLayout = ModelView.Size.MATCH_WIDTH_WRAP_HEIGHT, fullSpan = false)
class UploadedPhotoRow @JvmOverloads constructor(
  context: Context,
  attrs: AttributeSet? = null,
  defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr) {
  val photoView: ImageView
  val staticMapView: ImageView
  private val photoIdTextView: TextView
  private val photoUploadingStateIndicator: View
  private val receivedIconImageView: ImageView
  private val clickView: ConstraintLayout

  init {
    inflate(context, R.layout.epoxy_adapter_item_uploaded_photo, this)

    photoIdTextView = findViewById(R.id.photo_id_text_view)
    photoView = findViewById(R.id.photo_view)
    staticMapView = findViewById(R.id.static_map_view)
    photoUploadingStateIndicator = findViewById<View>(R.id.photo_uploading_state_indicator)
    receivedIconImageView = findViewById(R.id.received_icon_image_view)
    clickView = findViewById(R.id.click_view)

    orientation = VERTICAL
  }

  @ModelProp
  fun setPhoto(uploadedPhoto: UploadedPhoto) {
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

    if (uploadedPhoto.showPhoto) {
      staticMapView.visibility = View.GONE
      photoView.visibility = View.VISIBLE
    } else {
      staticMapView.visibility = View.VISIBLE
      photoView.visibility = View.GONE
    }
  }

  @CallbackProp
  fun setCallback(listener: OnClickListener?) {
    clickView.setOnClickListener(listener)
  }
}