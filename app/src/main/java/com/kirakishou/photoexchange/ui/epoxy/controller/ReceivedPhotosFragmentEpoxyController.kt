package com.kirakishou.photoexchange.ui.epoxy.controller

import android.content.Context
import android.widget.ImageView
import android.widget.Toast
import com.airbnb.epoxy.AsyncEpoxyController
import com.airbnb.mvrx.*
import com.kirakishou.fixmypc.photoexchange.R
import com.kirakishou.photoexchange.helper.ImageLoader
import com.kirakishou.photoexchange.helper.exception.EmptyUserUuidException
import com.kirakishou.photoexchange.helper.extension.safe
import com.kirakishou.photoexchange.mvrx.model.photo.ReceivedPhoto
import com.kirakishou.photoexchange.mvrx.viewmodel.ReceivedPhotosFragmentViewModel
import com.kirakishou.photoexchange.ui.epoxy.row.loadingRow
import com.kirakishou.photoexchange.ui.epoxy.row.receivedPhotoRow
import com.kirakishou.photoexchange.ui.epoxy.row.textRow
import timber.log.Timber
import java.lang.ref.WeakReference

class ReceivedPhotosFragmentEpoxyController(
  private val imageLoader: ImageLoader
) : BaseEpoxyController() {
  private val TAG = "ReceivedPhotosFragmentEpoxyController"

  fun cancelPendingImageLoadingRequests() {
    imageLoader.cancelAll()
  }

  fun rebuild(
    context: Context,
    controller: AsyncEpoxyController,
    viewModel: ReceivedPhotosFragmentViewModel
  ) {
    withState(viewModel) { state ->
      controller.apply {
        when (state.receivedPhotosRequest) {
          is Loading,
          is Success -> {
            if (state.receivedPhotosRequest is Loading && state.receivedPhotos.isEmpty()) {
              Timber.tag(TAG).d("Loading received photos")

              loadingRow {
                id("received_photos_loading_row")
              }

              return@withState
            }

            if (state.receivedPhotos.isEmpty()) {
              textRow {
                id("no_received_photos")
                text(context.getString(R.string.you_have_no_photos_yet))
              }
            } else {
              state.receivedPhotos.forEach { photo ->
                receivedPhotoRow {
                  id("received_photo_${photo.receivedPhotoName}")
                  photo(photo)
                  clickViewCallback { model, _, _, _ ->
                    viewModel.swapPhotoAndMap(model.photo().receivedPhotoName)
                  }
                  favouriteButtonCallback { model, _, _, _ ->
                    viewModel.favouritePhoto(model.photo().receivedPhotoName)
                  }
                  reportButtonCallback { model, _, _, _ ->
                    viewModel.reportPhoto(model.photo().receivedPhotoName)
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
                  id("load_next_page_${state.receivedPhotos.size}")
                  onBind { _, _, _ -> viewModel.loadReceivedPhotos(false) }
                }
              }
            }
          }
          is Fail -> {
            Timber.tag(TAG).d("Fail received photos")

            buildErrorNotification(state.receivedPhotosRequest.error, context, viewModel)
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
    viewModel: ReceivedPhotosFragmentViewModel
  ) {
    when (error) {
      is EmptyUserUuidException -> {
        textRow {
          id("no_uploaded_photos_message")
          text("You have to upload at least one photo first")
        }
      }
      else -> {
        textRow {
          val exceptionMessage = error.message ?: "Unknown error message"
          Toast.makeText(context, "Exception message is: \"$exceptionMessage\"", Toast.LENGTH_LONG).show()

          Timber.tag(TAG).e(error)

          id("unknown_error")
          text(context.getString(R.string.unknown_error_while_trying_to_load_photos_text))
          callback { _ ->
            Timber.tag(TAG).d("Reloading")
            viewModel.resetState()
          }
        }
      }
    }
  }

  private fun loadPhotoOrImage(
    photo: ReceivedPhoto,
    photoView: ImageView,
    mapView: ImageView
  ) {
    if (photo.showPhoto) {
      imageLoader.loadPhotoFromNetInto(photo.receivedPhotoName, WeakReference(photoView))
    } else {
      imageLoader.loadStaticMapImageFromNetInto(photo.receivedPhotoName, WeakReference(mapView))
    }
  }
}