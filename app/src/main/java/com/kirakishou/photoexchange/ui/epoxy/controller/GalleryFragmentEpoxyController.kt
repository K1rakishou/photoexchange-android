package com.kirakishou.photoexchange.ui.epoxy.controller

import android.content.Context
import android.widget.ImageView
import android.widget.Toast
import com.airbnb.epoxy.AsyncEpoxyController
import com.airbnb.mvrx.*
import com.kirakishou.fixmypc.photoexchange.R
import com.kirakishou.photoexchange.helper.ImageLoader
import com.kirakishou.photoexchange.helper.extension.safe
import com.kirakishou.photoexchange.mvrx.model.photo.GalleryPhoto
import com.kirakishou.photoexchange.mvrx.viewmodel.GalleryFragmentViewModel
import com.kirakishou.photoexchange.ui.epoxy.row.galleryPhotoRow
import com.kirakishou.photoexchange.ui.epoxy.row.loadingRow
import com.kirakishou.photoexchange.ui.epoxy.row.textRow
import timber.log.Timber
import java.lang.ref.WeakReference
import javax.inject.Inject

class GalleryFragmentEpoxyController
@Inject constructor(
  private val imageLoader: ImageLoader
) : BaseEpoxyController() {
  private val TAG = "GalleryFragmentEpoxyController"

  fun cancelPendingImageLoadingRequests() {
    imageLoader.cancelAll()
  }

  fun rebuild(
    context: Context,
    controller: AsyncEpoxyController,
    viewModel: GalleryFragmentViewModel
  ) {
    withState(viewModel) { state ->
      controller.apply {
        when (state.galleryPhotosRequest) {
          is Loading,
          is Success -> {
            if (state.galleryPhotosRequest is Loading && state.galleryPhotos.isEmpty()) {
              Timber.tag(TAG).d("Loading gallery photos")

              loadingRow {
                id("gallery_photos_loading_row")
              }

              return@withState
            }

            if (state.galleryPhotos.isEmpty()) {
              textRow {
                id("no_gallery_photos")
                text(context.getString(R.string.the_gallery_is_empty_text))
              }
            } else {
              state.galleryPhotos.forEach { photo ->
                galleryPhotoRow {
                  id("gallery_photo_${photo.photoName}")
                  photo(photo)
                  clickViewCallback { model, _, _, _ ->
                    viewModel.swapPhotoAndMap(model.photo().photoName)
                  }
                  favouriteButtonCallback { model, _, _, _ ->
                    viewModel.favouritePhoto(model.photo().photoName)
                  }
                  reportButtonCallback { model, _, _, _ ->
                    viewModel.reportPhotos(model.photo().photoName)
                  }
                  onBind { model, view, _ ->
                    loadPhotoOrImage(model.photo(), view.photoView, view.staticMapView)
                  }
                }
              }

              if (state.isEndReached) {
                textRow {
                  id("list_end_footer_text")
                  text(context.getString(R.string.end_of_list_reached_text))
                  callback { _ ->
                    Timber.tag(TAG).d("Reloading")
                    viewModel.resetState()
                  }
                }
              } else {
                loadingRow {
                  //we should change the id to trigger the binding
                  id("load_next_page_${state.galleryPhotos.size}")
                  onBind { _, _, _ -> viewModel.loadGalleryPhotos(false) }
                }
              }
            }
          }
          is Fail -> {
            Timber.tag(TAG).d("Fail uploaded photos")

            buildErrorNotification(state.galleryPhotosRequest.error, context, viewModel)
          }
          Uninitialized -> {
            //do nothing
          }
        }.safe
      }
    }
  }

  private fun AsyncEpoxyController.buildErrorNotification(
    error: Throwable,
    context: Context,
    viewModel: GalleryFragmentViewModel
  ) {
    textRow {
      val exceptionMessage = error.message ?: "Unknown error message"
      Toast.makeText(context, "Exception message is: \"$exceptionMessage\"", Toast.LENGTH_LONG).show()

      id("unknown_error")
      text(context.getString(R.string.unknown_error_while_trying_to_load_photos_text))
      callback { _ ->
        Timber.tag(TAG).d("Reloading")
        viewModel.resetState()
      }
    }
  }

  private fun loadPhotoOrImage(
    photo: GalleryPhoto,
    photoView: ImageView,
    mapView: ImageView
  ) {
    if (photo.showPhoto) {
      imageLoader.loadPhotoFromNetInto(photo.photoName, WeakReference(photoView))
    } else {
      imageLoader.loadStaticMapImageFromNetInto(photo.photoName, WeakReference(mapView))
    }
  }
}