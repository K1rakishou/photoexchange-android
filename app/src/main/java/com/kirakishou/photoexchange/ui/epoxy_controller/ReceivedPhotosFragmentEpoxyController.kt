package com.kirakishou.photoexchange.ui.epoxy_controller

import android.content.Context
import android.widget.Toast
import com.airbnb.epoxy.AsyncEpoxyController
import com.airbnb.mvrx.*
import com.kirakishou.photoexchange.helper.exception.EmptyUserIdException
import com.kirakishou.photoexchange.helper.extension.safe
import com.kirakishou.photoexchange.mvp.viewmodel.ReceivedPhotosFragmentViewModel
import com.kirakishou.photoexchange.ui.adapter.epoxy.loadingRow
import com.kirakishou.photoexchange.ui.adapter.epoxy.receivedPhotoRow
import com.kirakishou.photoexchange.ui.adapter.epoxy.textRow
import timber.log.Timber

class ReceivedPhotosFragmentEpoxyController {
  private val TAG = "ReceivedPhotosFragmentEpoxyController"

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
            if (state.receivedPhotosRequest is Loading) {
              Timber.tag(TAG).d("Loading received photos")

              loadingRow {
                id("received_photos_loading_row")
              }
            } else {
              Timber.tag(TAG).d("Success received photos")
            }

            if (state.receivedPhotos.isEmpty()) {
              textRow {
                id("no_received_photos")
                text("You have no photos yet")
              }
            } else {
              state.receivedPhotos.forEach { photo ->
                receivedPhotoRow {
                  id("received_photo_${photo.photoId}")
                  photo(photo)
                  callback { model, _, _, _ ->
                    viewModel.swapPhotoAndMap(model.photo().receivedPhotoName)
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
                  id("load_next_page_${state.receivedPhotos.size}")
                  onBind { _, _, _ -> viewModel.loadReceivedPhotos() }
                }
              }
            }
          }
          is Fail -> {
            Timber.tag(TAG).d("Fail uploaded photos")

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
      is EmptyUserIdException -> {
        textRow {
          id("no_received_photos_message")
          text("You have no received photos yet")
        }
      }
      else -> {
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
  }
}