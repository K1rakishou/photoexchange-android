package com.kirakishou.photoexchange.ui.epoxy.controller

import android.content.Context
import android.widget.ImageView
import android.widget.Toast
import com.airbnb.epoxy.AsyncEpoxyController
import com.airbnb.mvrx.*
import com.kirakishou.fixmypc.photoexchange.R
import com.kirakishou.photoexchange.helper.ImageLoader
import com.kirakishou.photoexchange.helper.Paged
import com.kirakishou.photoexchange.helper.exception.EmptyUserIdException
import com.kirakishou.photoexchange.mvp.model.PhotoState
import com.kirakishou.photoexchange.mvp.model.photo.UploadedPhoto
import com.kirakishou.photoexchange.mvp.model.photo.UploadingPhoto
import com.kirakishou.photoexchange.mvp.viewmodel.UploadedPhotosFragmentViewModel
import com.kirakishou.photoexchange.mvp.viewmodel.state.UploadedPhotosFragmentState
import com.kirakishou.photoexchange.ui.epoxy.row.*
import timber.log.Timber

class UploadedPhotosFragmentEpoxyController(
  private val imageLoader: ImageLoader
) : BaseEpoxyController() {
  private val TAG = "UploadedPhotosFragmentEpoxyController"

  fun rebuild(
    context: Context,
    controller: AsyncEpoxyController,
    viewModel: UploadedPhotosFragmentViewModel
  ) {
    withState(viewModel) { state ->
      controller.apply {
        if (state.takenPhotos.isNotEmpty()) {
          buildTakenPhotos(context, state, viewModel)
        }

        buildUploadedPhotos(context, state, viewModel, state.uploadedPhotosRequest)
      }
    }
  }

  private fun AsyncEpoxyController.buildUploadedPhotos(
    context: Context,
    state: UploadedPhotosFragmentState,
    viewModel: UploadedPhotosFragmentViewModel,
    uploadedPhotosRequest: Async<Paged<UploadedPhoto>>
  ) {
    if (uploadedPhotosRequest is Loading) {
      if (state.uploadedPhotos.isEmpty()) {
        Timber.tag(TAG).d("Loading uploaded photos")

        loadingRow {
          id("uploaded_photos_loading_row")
        }

        return
      }
    }

    if (state.uploadedPhotos.isEmpty()) {
      if (state.takenPhotos.isEmpty()) {
        textRow {
          id("no_uploaded_photos")
          text(context.getString(R.string.you_have_no_photos_yet))
        }
      }
    } else {
      sectionRow {
        id("uploaded_photos_section")
        text(context.getString(R.string.uploaded_photos_text))
      }

      state.uploadedPhotos.forEach { photo ->
        uploadedPhotoRow {
          id("uploaded_photo_${photo.photoName}")
          photo(photo)
          callback { model, _, _, _ -> viewModel.swapPhotoAndMap(model.photo().photoName) }
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
          id("load_next_page_${state.uploadedPhotos.size}")
          onBind { _, _, _ -> viewModel.loadUploadedPhotos(false) }
        }
      }
    }

    if (uploadedPhotosRequest is Fail) {
      Timber.tag(TAG).d("Fail uploaded photos")

      buildErrorNotification(uploadedPhotosRequest.error, context, viewModel)
    }
  }

  private fun AsyncEpoxyController.buildTakenPhotos(
    context: Context,
    state: UploadedPhotosFragmentState,
    viewModel: UploadedPhotosFragmentViewModel
  ) {
    sectionRow {
      id("queued_up_and_uploading_photos_section")
      text(context.getString(R.string.uploading_photos_text))
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
            onBind { model, view, _ ->
              model.photo().photoTempFile?.let { file ->
                imageLoader.loadPhotoFromDiskInto(file, view.photoView)
              }
            }
          }
        }
        PhotoState.PHOTO_UPLOADING -> {
          val uploadingPhoto = photo as UploadingPhoto

          uploadingPhotoRow {
            id("uploading_photo_${photo.id}")
            photo(uploadingPhoto)
            progress(uploadingPhoto.progress)
            onBind { model, view, _ ->
              model.photo().photoTempFile?.let { file ->
                imageLoader.loadPhotoFromDiskInto(file, view.photoView)
              }
            }
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
          text("You have to upload at least one photo first")
        }
      }
      else -> {
        textRow {
          val exceptionMessage = error.message ?: "Unknown error message"
          Toast.makeText(context, "Exception message is: \"$exceptionMessage\"", Toast.LENGTH_LONG).show()

          id("unknown_error")
          text(context.getString(R.string.unknown_error_while_trying_to_load_photos_text))
          callback { _ ->
            Timber.tag(TAG).d("Reloading")
            viewModel.resetState(true)
          }
        }
      }
    }
  }

  private fun loadPhotoOrImage(photo: UploadedPhoto, photoView: ImageView, mapView: ImageView) {
    if (photo.showPhoto) {
      imageLoader.loadPhotoFromNetInto(photo.photoName, photoView)
    } else {
      imageLoader.loadStaticMapImageFromNetInto(photo.receiverInfo!!.receiverPhotoName, mapView)
    }
  }
}