package com.kirakishou.photoexchange.ui.epoxy.controller

import android.content.Context
import android.widget.Toast
import com.airbnb.epoxy.AsyncEpoxyController
import com.airbnb.mvrx.*
import com.kirakishou.photoexchange.helper.extension.safe
import com.kirakishou.photoexchange.mvp.viewmodel.GalleryFragmentViewModel
import com.kirakishou.photoexchange.ui.epoxy.row.galleryPhotoRow
import com.kirakishou.photoexchange.ui.epoxy.row.loadingRow
import com.kirakishou.photoexchange.ui.epoxy.row.textRow
import timber.log.Timber

class GalleryFragmentEpoxyController {
  private val TAG = "GalleryFragmentEpoxyController"

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
            if (state.galleryPhotosRequest is Loading) {
              Timber.tag(TAG).d("Loading gallery photos")

              loadingRow {
                id("gallery_photos_loading_row")
              }
            } else {
              Timber.tag(TAG).d("Success gallery photos")
            }

            if (state.galleryPhotos.isEmpty()) {
              textRow {
                id("no_gallery_photos")
                text("The gallery is empty")
              }
            } else {
              state.galleryPhotos.forEach { photo ->
                galleryPhotoRow {
                  id("gallery_photo_${photo.photoName}")
                  photo(photo)
                  favouriteButtonEnabled(state.isFavouriteRequestActive)
                  reportButtonEnabled(state.isReportRequestActive)
                  clickViewCallback { model, _, _, _ ->
                    viewModel.swapPhotoAndMap(model.photo().photoName)
                  }
                  favouriteButtonCallback { model, _, _, _ ->
                    viewModel.favouritePhoto(model.photo().photoName)
                  }
                  reportButtonCallback { model, _, _, _ ->
                    viewModel.reportPhotos(model.photo().photoName)
                  }
                }
              }

              if (state.isEndReached) {
                textRow {
                  id("list_end_footer_text")
                  text("End of the list reached.\nClick here to reload")
                  callback { _ ->
                    Timber.tag(TAG).d("Reloading")
                    viewModel.resetState()
                  }
                }
              } else {
                loadingRow {
                  //we should change the id to trigger the binding
                  id("load_next_page_${state.galleryPhotos.size}")
                  onBind { _, _, _ -> viewModel.loadGalleryPhotos() }
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
      text("Unknown error has occurred while trying to load photos from the database. \nClick here to retry")
      callback { _ ->
        Timber.tag(TAG).d("Reloading")
        viewModel.resetState()
      }
    }
  }
}