package com.kirakishou.photoexchange.ui.epoxy.row

import android.content.Context
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
import com.kirakishou.photoexchange.mvrx.model.photo.ReceivedPhoto

@ModelView(autoLayout = ModelView.Size.MATCH_WIDTH_WRAP_HEIGHT, fullSpan = false)
class ReceivedPhotoRow @JvmOverloads constructor(
  context: Context,
  attrs: AttributeSet? = null,
  defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr) {
  val photoView: ImageView
  val staticMapView: ImageView
  private val clickView: ConstraintLayout
  private val photoButtonsHolder: LinearLayout
  private val favouriteButton: LinearLayout
  private val favouriteIcon: ImageView
  private val favouritesCount: TextView
  private val reportButton: LinearLayout
  private val reportIcon: ImageView

  init {
    inflate(context, R.layout.epoxy_adapter_item_received_photo, this)

    photoView = findViewById(R.id.photo_view)
    staticMapView = findViewById(R.id.static_map_view)
    clickView = findViewById(R.id.click_view)
    photoButtonsHolder = findViewById(R.id.photo_buttons_holder)
    favouriteButton = findViewById(R.id.favourite_button)
    favouriteIcon = findViewById(R.id.favourite_icon)
    favouritesCount = findViewById(R.id.favourites_count_text_view)
    reportButton = findViewById(R.id.report_button)
    reportIcon = findViewById(R.id.report_icon)

    orientation = VERTICAL
  }

  @ModelProp
  fun setPhoto(photo: ReceivedPhoto) {
    if (photo.showPhoto) {
      showPhotoHideMap()

      if (photo.photoAdditionalInfo != null) {
        showControls(photo)
        favouritesCount.text = photo.photoAdditionalInfo.favouritesCount.toString()
      } else {
        hideControls()
      }
    } else {
      showMapHidePhoto()
    }
  }

  @CallbackProp
  fun setClickViewCallback(listener: OnClickListener?) {
    clickView.setOnClickListener(listener)
  }

  @CallbackProp
  fun setFavouriteButtonCallback(listener: OnClickListener?) {
    favouriteButton.setOnClickListener(listener)
  }

  @CallbackProp
  fun setReportButtonCallback(listener: OnClickListener?) {
    reportButton.setOnClickListener(listener)
  }

  private fun showControls(photo: ReceivedPhoto) {
    photoButtonsHolder.visibility = View.VISIBLE
    favouriteButton.isClickable = true
    reportButton.isClickable = true

    photo.photoAdditionalInfo!!

    if (photo.photoAdditionalInfo.isFavourited) {
      favouriteIcon.setImageDrawable(context.resources.getDrawable(R.drawable.ic_favorite))
    } else {
      favouriteIcon.setImageDrawable(context.resources.getDrawable(R.drawable.ic_favorite_border))
    }

    if (photo.photoAdditionalInfo.isReported) {
      reportIcon.setImageDrawable(context.resources.getDrawable(R.drawable.ic_reported))
    } else {
      reportIcon.setImageDrawable(context.resources.getDrawable(R.drawable.ic_report_border))
    }
  }

  private fun hideControls() {
    photoButtonsHolder.visibility = View.GONE
    favouriteButton.isClickable = false
    reportButton.isClickable = false
  }

  private fun showPhotoHideMap() {
    staticMapView.visibility = View.GONE
    photoView.visibility = View.VISIBLE
  }

  private fun showMapHidePhoto() {
    staticMapView.visibility = View.VISIBLE
    photoView.visibility = View.GONE
  }
}