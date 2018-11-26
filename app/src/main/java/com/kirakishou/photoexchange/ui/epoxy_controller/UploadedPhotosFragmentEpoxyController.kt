package com.kirakishou.photoexchange.ui.epoxy_controller

import android.content.Context
import android.widget.Toast
import com.airbnb.epoxy.AsyncEpoxyController
import com.airbnb.mvrx.*
import com.kirakishou.photoexchange.helper.exception.EmptyUserIdException
import com.kirakishou.photoexchange.helper.extension.safe
import com.kirakishou.photoexchange.mvp.model.PhotoState
import com.kirakishou.photoexchange.mvp.model.photo.UploadedPhoto
import com.kirakishou.photoexchange.mvp.model.photo.UploadingPhoto
import com.kirakishou.photoexchange.mvp.viewmodel.UploadedPhotosFragmentViewModel
import com.kirakishou.photoexchange.mvp.viewmodel.state.UploadedPhotosFragmentState
import com.kirakishou.photoexchange.ui.adapter.epoxy.*
import timber.log.Timber

class UploadedPhotosFragmentEpoxyController {
  private val TAG = "UploadedPhotosFragmentEpoxyController"

  fun rebuild(
    context: Context,
    controller: AsyncEpoxyController,
    viewModel: UploadedPhotosFragmentViewModel
  ) {
    withState(viewModel) { state ->
      controller.apply {
        if (state.takenPhotos.isNotEmpty()) {
          buildTakenPhotos(state, viewModel)
        }

        buildUploadedPhotos(state, viewModel, state.uploadedPhotosRequest, context)
      }
    }
  }

  private fun AsyncEpoxyController.buildUploadedPhotos(
    state: UploadedPhotosFragmentState,
    viewModel: UploadedPhotosFragmentViewModel,
    uploadedPhotosRequest: Async<List<UploadedPhoto>>,
    context: Context
  ) {
    when (uploadedPhotosRequest) {
      is Loading,
      is Success -> {
        if (uploadedPhotosRequest is Loading) {
          Timber.tag(TAG).d("Loading uploaded photos")

          loadingRow {
            id("uploaded_photos_loading_row")
          }
        } else {
          Timber.tag(TAG).d("Success uploaded photos")
        }

        if (state.uploadedPhotos.isEmpty()) {
          textRow {
            id("no_uploaded_photos")
            text("You have no photos yet")
          }
        } else {
          sectionRow {
            id("uploaded_photos_section")
            text("Uploaded photos")
          }

          state.uploadedPhotos.forEach { photo ->
            uploadedPhotoRow {
              id("uploaded_photo_${photo.photoId}")
              photo(photo)
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
              id("load_next_page_${state.uploadedPhotos.size}")
              onBind { _, _, _ -> viewModel.loadUploadedPhotos() }
            }
          }
        }
      }
      is Fail -> {
        Timber.tag(TAG).d("Fail uploaded photos")

        buildErrorNotification(uploadedPhotosRequest.error, context, viewModel)
      }
      is Uninitialized -> {
        //do nothing
      }
    }.safe
  }

  private fun AsyncEpoxyController.buildTakenPhotos(
    state: UploadedPhotosFragmentState,
    viewModel: UploadedPhotosFragmentViewModel
  ) {
    sectionRow {
      id("queued_up_and_uploading_photos_section")
      text("Uploading photos")
    }

    state.takenPhotos.forEach { photo ->
      when (photo.photoState) {
        PhotoState.PHOTO_TAKEN -> {
        }
        PhotoState.PHOTO_QUEUED_UP -> {
          queuedUpPhotoRow {
            id("queued_up_photo_${photo.id}")
            photo(photo)
            callback { _ -> viewModel.cancelPhotoUploading(photo.id) }
          }
        }
        PhotoState.PHOTO_UPLOADING -> {
          val uploadingPhoto = photo as UploadingPhoto

          uploadingPhotoRow {
            id("uploading_photo_${photo.id}")
            photo(uploadingPhoto)
            progress(uploadingPhoto.progress)
          }
        }
      }
    }
  }

  private fun AsyncEpoxyController.buildErrorNotification(
    error: Throwable,
    context: Context,
    viewModel: UploadedPhotosFragmentViewModel
  ) {
    when (error) {
      is EmptyUserIdException -> {
        textRow {
          id("no_uploaded_photos_message")
          text("You have no uploaded photos yet")
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