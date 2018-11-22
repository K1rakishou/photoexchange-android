package com.kirakishou.photoexchange.ui.adapter.epoxy

import android.content.Context
import android.util.AttributeSet
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.appcompat.widget.AppCompatButton
import com.airbnb.epoxy.CallbackProp
import com.airbnb.epoxy.ModelProp
import com.airbnb.epoxy.ModelView
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.request.RequestOptions
import com.kirakishou.fixmypc.photoexchange.R
import com.kirakishou.photoexchange.di.module.GlideApp
import com.kirakishou.photoexchange.mvp.model.PhotoState
import com.kirakishou.photoexchange.mvp.model.TakenPhoto


@ModelView(autoLayout = ModelView.Size.MATCH_WIDTH_WRAP_HEIGHT)
class FailedToUploadPhotoRow @JvmOverloads constructor(
  context: Context,
  attrs: AttributeSet? = null,
  defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr) {
  private val photoView: ImageView
  private val deleteFailedToUploadPhotoButton: AppCompatButton
  private val retryToUploadFailedPhoto: AppCompatButton

  init {
    inflate(context, R.layout.epoxy_adapter_failed_to_upload_photo, this)

    photoView = findViewById(R.id.photo_view)
    deleteFailedToUploadPhotoButton = findViewById(R.id.delete_failed_to_upload_photo)
    retryToUploadFailedPhoto = findViewById(R.id.retry_to_upload_failed_photo)

    orientation = VERTICAL
  }

  @ModelProp
  fun photo(photo: TakenPhoto) {
    if (photo.photoState != PhotoState.FAILED_TO_UPLOAD) {
      throw IllegalStateException("photo state should be FAILED_TO_UPLOAD but actually is (${photo.photoState})")
    }

    photo.photoTempFile?.let { photoFile ->
      GlideApp.with(context)
        //we do not need to cache this image
        .applyDefaultRequestOptions(RequestOptions.diskCacheStrategyOf(DiskCacheStrategy.NONE))
        .load(photoFile)
        .apply(RequestOptions().centerCrop())
        .into(photoView)
    }
  }

  @CallbackProp
  fun deleteButtonCallback(listener: OnClickListener?) {
    deleteFailedToUploadPhotoButton.setOnClickListener(listener)
  }

  @CallbackProp
  fun retryButtonCallback(listener: OnClickListener?) {
    retryToUploadFailedPhoto.setOnClickListener(listener)
  }
}