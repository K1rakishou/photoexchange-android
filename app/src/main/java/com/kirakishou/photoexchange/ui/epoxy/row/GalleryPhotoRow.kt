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
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.request.RequestOptions
import com.kirakishou.fixmypc.photoexchange.R
import com.kirakishou.photoexchange.di.module.GlideApp
import com.kirakishou.photoexchange.helper.Constants
import com.kirakishou.photoexchange.mvp.model.photo.GalleryPhoto

@ModelView(autoLayout = ModelView.Size.MATCH_WIDTH_WRAP_HEIGHT)
class GalleryPhotoRow @JvmOverloads constructor(
  context: Context,
  attrs: AttributeSet? = null,
  defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr) {
  private val clickView: ConstraintLayout
  private val photoView: ImageView
  private val staticMapView: ImageView
  private val photoButtonsHolder: LinearLayout
  private val favouriteButton: LinearLayout
  private val favouriteIcon: ImageView
  private val favouritesCount: TextView
  private val reportButton: LinearLayout
  private val reportIcon: ImageView

  init {
    inflate(context, R.layout.epoxy_adapter_item_gallery_photo, this)

    clickView = findViewById(R.id.click_view)
    photoView = findViewById(R.id.photo_view)
    staticMapView = findViewById(R.id.static_map_view)
    photoButtonsHolder = findViewById(R.id.photo_buttons_holder)
    favouriteButton = findViewById(R.id.favourite_button)
    favouriteIcon = findViewById(R.id.favourite_icon)
    favouritesCount = findViewById(R.id.favourites_count_text_view)
    reportButton = findViewById(R.id.report_button)
    reportIcon = findViewById(R.id.report_icon)

    orientation = VERTICAL
  }

  @ModelProp
  fun setPhoto(photo: GalleryPhoto) {
    if (photo.showPhoto) {
      showPhotoHideMap()

      if (photo.galleryPhotoInfo != null) {
        showControls()

        if (photo.galleryPhotoInfo.isFavourited) {
          favouriteIcon.setImageDrawable(context.resources.getDrawable(R.drawable.ic_favorite))
        } else {
          favouriteIcon.setImageDrawable(context.resources.getDrawable(R.drawable.ic_favorite_border))
        }

        if (photo.galleryPhotoInfo.isReported) {
          reportIcon.setImageDrawable(context.resources.getDrawable(R.drawable.ic_reported))
        } else {
          reportIcon.setImageDrawable(context.resources.getDrawable(R.drawable.ic_report_border))
        }
      } else {
        hideControls()
      }
    } else {
      showMapHidePhoto()
      loadStaticMap(photo)
    }

    favouritesCount.text = photo.favouritesCount.toString()
    loadGalleryPhoto(photo)
  }

  @ModelProp
  fun setFavouriteButtonEnabled(isFavouriteRequestActive: Boolean) {
    favouriteButton.isEnabled = !isFavouriteRequestActive
  }

  @ModelProp
  fun setReportButtonEnabled(isReportRequestActive: Boolean) {
    reportButton.isEnabled = !isReportRequestActive
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

  private fun showControls() {
    photoButtonsHolder.visibility = View.VISIBLE
    favouriteButton.isClickable = true
    reportButton.isClickable = true
  }

  private fun hideControls() {
    photoButtonsHolder.visibility = View.GONE
    favouriteButton.isClickable = false
    reportButton.isClickable = false
  }

  private fun loadGalleryPhoto(photo: GalleryPhoto) {
    val fullUrl = "${Constants.BASE_PHOTOS_URL}/${photo.photoName}/${photo.photoSize.value}"
    GlideApp.with(context)
      .load(fullUrl)
      .diskCacheStrategy(DiskCacheStrategy.AUTOMATIC)
      .apply(RequestOptions().centerCrop())
      .into(photoView)
  }

  private fun loadStaticMap(photo: GalleryPhoto) {
    val fullUrl = "${Constants.BASE_STATIC_MAP_URL}/${photo.photoName}"
    GlideApp.with(context)
      .load(fullUrl)
      .diskCacheStrategy(DiskCacheStrategy.AUTOMATIC)
      .apply(RequestOptions().centerCrop())
      .into(staticMapView)
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