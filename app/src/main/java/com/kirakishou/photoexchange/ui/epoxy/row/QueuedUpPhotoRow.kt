package com.kirakishou.photoexchange.ui.epoxy.row

import android.content.Context
import android.graphics.drawable.ColorDrawable
import android.util.AttributeSet
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.widget.AppCompatButton
import androidx.cardview.widget.CardView
import com.airbnb.epoxy.CallbackProp
import com.airbnb.epoxy.ModelProp
import com.airbnb.epoxy.ModelView
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.request.RequestOptions
import com.kirakishou.fixmypc.photoexchange.R
import com.kirakishou.photoexchange.di.module.GlideApp
import com.kirakishou.photoexchange.mvp.model.PhotoState
import com.kirakishou.photoexchange.mvp.model.photo.TakenPhoto
import java.lang.IllegalStateException

@ModelView(autoLayout = ModelView.Size.MATCH_WIDTH_WRAP_HEIGHT)
class QueuedUpPhotoRow @JvmOverloads constructor(
  context: Context,
  attrs: AttributeSet? = null,
  defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr) {
  val photoView: ImageView
  private val photoIdTextView: TextView
  private val uploadingMessageHolderView: CardView
  private val loadingProgress: ProgressBar
  private val photoUploadingStateIndicator: View
  private val cancelButton: AppCompatButton

  init {
    inflate(context, R.layout.epoxy_adapter_item_taken_photo, this)

    photoIdTextView = findViewById(R.id.photo_id_text_view)
    photoView = findViewById(R.id.photo_view)
    uploadingMessageHolderView = findViewById(R.id.uploading_message_holder)
    loadingProgress = findViewById(R.id.loading_progress)
    photoUploadingStateIndicator = findViewById<View>(R.id.photo_uploading_state_indicator)
    cancelButton = findViewById(R.id.cancel_button)

    orientation = VERTICAL
//    loadingProgress.isIndeterminate = true
    cancelButton.isEnabled = true
  }

  @ModelProp
  fun photo(photo: TakenPhoto) {
    if (photo.photoState != PhotoState.PHOTO_QUEUED_UP) {
      throw IllegalStateException("photo state should be PHOTO_QUEUED_UP not (${photo.photoState})")
    }

    photoIdTextView.text = photo.id.toString()
    photoUploadingStateIndicator.background = ColorDrawable(context.resources.getColor(R.color.photo_state_uploading_color))
    uploadingMessageHolderView.visibility = View.VISIBLE
  }

  @CallbackProp
  fun setCallback(listener: OnClickListener?) {
    cancelButton.setOnClickListener(listener)
  }
}