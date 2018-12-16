package com.kirakishou.photoexchange.helper

import android.content.Context
import com.kirakishou.photoexchange.interactors.TakePhotoUseCase
import com.kirakishou.photoexchange.mvp.model.photo.TakenPhoto
import io.fotoapparat.Fotoapparat
import io.fotoapparat.configuration.CameraConfiguration
import io.fotoapparat.selector.back
import io.fotoapparat.selector.highestFps
import io.fotoapparat.selector.highestResolution
import io.fotoapparat.selector.manualJpegQuality
import io.fotoapparat.view.CameraView
import timber.log.Timber
import java.lang.ref.WeakReference

/**
 * Created by kirakishou on 1/4/2018.
 */
open class CameraProvider(
  private val context: Context,
  private val takePhotoUseCase: TakePhotoUseCase
)  {
  private val TAG = "CameraProvider"

  private var fotoapparat: Fotoapparat? = null

  private fun createConfiguration(): CameraConfiguration {
    return CameraConfiguration(
      previewResolution = highestResolution(),
      previewFpsRange = highestFps(),
      jpegQuality = manualJpegQuality(100)
    )
  }

  fun initCamera(cameraView: CameraView) {
    if (fotoapparat != null) {
      return
    }

    fotoapparat = Fotoapparat(
      context = context,
      view = cameraView,
      lensPosition = back(),
      cameraConfiguration = createConfiguration()
    )
  }

  fun startCamera() {
    if (fotoapparat == null) {
      throw RuntimeException("Camera is null")
    }

    fotoapparat!!.start()
    Timber.tag(TAG).d("Camera started")
  }

  fun stopCamera() {
    if (fotoapparat == null) {
      throw RuntimeException("Camera is null")
    }

    fotoapparat!!.stop()
    Timber.tag(TAG).d("Camera stopped")
  }

  fun isAvailable(): Boolean = fotoapparat?.isAvailable(back()) ?: false
  fun isStarted() = fotoapparat != null
  fun takePicture() = fotoapparat?.takePicture()

  suspend fun takePhoto(): TakenPhoto? {
    return takePhotoUseCase.takePhoto(WeakReference(this))
  }
}