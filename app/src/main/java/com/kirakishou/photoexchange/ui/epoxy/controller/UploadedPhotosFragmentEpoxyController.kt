package com.kirakishou.photoexchange.ui.epoxy.controller

import android.content.Context
import android.widget.ImageView
import android.widget.Toast
import com.airbnb.epoxy.AsyncEpoxyController
import com.airbnb.mvrx.*
import com.kirakishou.fixmypc.photoexchange.R
import com.kirakishou.photoexchange.helper.ImageLoader
import com.kirakishou.photoexchange.helper.Paged
import com.kirakishou.photoexchange.helper.exception.EmptyUserUuidException
import com.kirakishou.photoexchange.mvrx.model.PhotoState
import com.kirakishou.photoexchange.mvrx.model.photo.UploadedPhoto
import com.kirakishou.photoexchange.mvrx.model.photo.UploadingPhoto
import com.kirakishou.photoexchange.mvrx.viewmodel.UploadedPhotosFragmentViewModel
import com.kirakishou.photoexchange.mvrx.viewmodel.state.UploadedPhotosFragmentState
import com.kirakishou.photoexchange.ui.epoxy.row.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import timber.log.Timber

class UploadedPhotosFragmentEpoxyController(
  private val imageLoader: ImageLoader
) : BaseEpoxyController() {
  private val TAG = "UploadedPhotosFragmentEpoxyController"

  fun rebuild(
    context: Context,
    coroutineScope: CoroutineScope,
    controller: AsyncEpoxyController,
    viewModel: UploadedPhotosFragmentViewModel
  ) {
    withState(viewModel) { state ->
      controller.apply {
        if (state.takenPhotos.isNotEmpty()) {
          buildTakenPhotos(context, state, viewModel)
        }

        if (state.takenPhotosRequest !is Fail) {
          buildUploadedPhotos(
            coroutineScope,
            context,
            state,
            viewModel,
            state.uploadedPhotosRequest
          )
        }
      }
    }
  }

  private fun AsyncEpoxyController.buildUploadedPhotos(
    coroutineScope: CoroutineScope,
    context: Context,
    state: UploadedPhotosFragmentState,
    viewModel: UploadedPhotosFragmentViewModel,
    uploadedPhotosRequest: Async<Paged<UploadedPhoto>>
  ) {
    if (state.uploadedPhotos.isNotEmpty()) {
      state.uploadedPhotos.forEach { photo ->
        uploadedPhotoRow {
          id("uploaded_photo_${photo.photoName}")
          photo(photo)
          callback { model, _, _, _ -> viewModel.swapPhotoAndMap(model.photo().photoName) }
          onBind { model, view, _ ->
            loadPhotoOrImage(coroutineScope, model.photo(), view.photoView, view.staticMapView)
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
    } else {
      if (state.isEndReached) {
        textRow {
          id("list_end_footer_text")
          text(context.getString(R.string.end_of_list_reached_text))
          callback { _ ->
            Timber.tag(TAG).d("Reloading")
            viewModel.resetState()
          }
        }
      }
    }

    if (uploadedPhotosRequest is Fail) {
      Timber.tag(TAG).d("Fail uploaded photos")

      val error = uploadedPhotosRequest.error
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
            Toast.makeText(
              context,
              "Exception message is: \"$exceptionMessage\"",
              Toast.LENGTH_LONG
            ).show()

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
  }

  private fun AsyncEpoxyController.buildTakenPhotos(
    context: Context,
    state: UploadedPhotosFragmentState,
    viewModel: UploadedPhotosFragmentViewModel
  ) {
    when (state.takenPhotosRequest) {
      is Loading,
      is Success -> {
        if (state.takenPhotosRequest is Loading && state.takenPhotos.isEmpty()) {
          Timber.tag(TAG).d("Loading taken photos")

          loadingRow {
            id("taken_photos_loading_row")
          }

          return
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
      is Fail -> {
        Timber.tag(TAG).d("Fail taken photos")

        textRow {
          val error = state.takenPhotosRequest.error
          val exceptionMessage = error.message ?: "Unknown error message"

          Toast.makeText(
            context,
            "Exception message is: \"$exceptionMessage\"",
            Toast.LENGTH_LONG
          ).show()

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
    coroutineScope: CoroutineScope,
    photo: UploadedPhoto,
    photoView: ImageView,
    mapView: ImageView
  ) {
    coroutineScope.launch {
      if (photo.showPhoto) {
        imageLoader.loadPhotoFromNetInto(photo.photoName, photoView)
      } else {
        imageLoader.loadStaticMapImageFromNetInto(photo.photoName, mapView)
      }
    }
  }
}