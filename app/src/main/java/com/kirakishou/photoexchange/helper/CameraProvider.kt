package com.kirakishou.photoexchange.helper

import android.content.Context
import com.kirakishou.photoexchange.helper.database.entity.TempFileEntity
import com.kirakishou.photoexchange.helper.database.repository.TakenPhotosRepository
import com.kirakishou.photoexchange.mvp.model.photo.TakenPhoto
import io.fotoapparat.Fotoapparat
import io.fotoapparat.configuration.CameraConfiguration
import io.fotoapparat.selector.back
import io.fotoapparat.selector.exactFixedFps
import io.fotoapparat.selector.highestResolution
import io.fotoapparat.selector.manualJpegQuality
import io.fotoapparat.view.CameraView
import timber.log.Timber
import java.lang.RuntimeException
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

/**
 * Created by kirakishou on 1/4/2018.
 */
open class CameraProvider(
  val context: Context,
  private val takenPhotosRepository: TakenPhotosRepository
) {

  private val TAG = "CameraProvider"
  private val isStarted = AtomicBoolean(false)
  private var camera: Fotoapparat? = null
  private val tag = "[${this::class.java.simpleName}]: "
  private val NORMAL_FPS = 30f

  private fun createConfiguration(fps: Float): CameraConfiguration {
    return CameraConfiguration(
      previewResolution = highestResolution(),
      previewFpsRange = exactFixedFps(fps),
      jpegQuality = manualJpegQuality(100)
    )
  }

  fun provideCamera(cameraView: CameraView) {
    if (camera != null) {
      return
    }

    val configuration = createConfiguration(NORMAL_FPS)

    camera = Fotoapparat(
      context = context,
      view = cameraView,
      lensPosition = back(),
      cameraConfiguration = configuration
    )
  }

  fun startCamera() {
    if (!isStarted.getAndSet(true)) {
      if (camera == null) {
        throw RuntimeException("Camera is null")
      }

      camera!!.start()
    }
  }

  fun stopCamera() {
    if (isStarted.getAndSet(false)) {
      if (camera == null) {
        throw RuntimeException("Camera is null")
      }

      camera!!.stop()
    }
  }

  fun isStarted(): Boolean = isStarted.get()
  fun isAvailable(): Boolean = camera?.isAvailable(back()) ?: false

  suspend fun takePhoto(): TakenPhoto? {
    Timber.tag(TAG).d("before cleanup")
    takenPhotosRepository.cleanup()
    Timber.tag(TAG).d("after cleanup")

    val tempFile = takenPhotosRepository.createTempFile()

    try {
      takePhotoInternal(tempFile)
    } catch (error: Throwable) {
      Timber.tag(TAG).d("takePhotoInternal returned false")

      takenPhotosRepository.markDeletedById(tempFile)
      return null
    }

    Timber.tag(TAG).d("before saveTakenPhoto")
    val takenPhoto = takenPhotosRepository.saveTakenPhoto(tempFile)
    Timber.tag(TAG).d("after saveTakenPhoto")

    if (takenPhoto == null) {
      Timber.tag(TAG).d("saveTakenPhoto returned empty photo")

      takenPhotosRepository.markDeletedById(tempFile) //TODO: should this be here?
      takenPhotosRepository.deleteMyPhoto(takenPhoto)
      return null
    }

    return takenPhoto
  }

  private suspend fun takePhotoInternal(tempFile: TempFileEntity) {
    if (!isAvailable()) {
      throw CameraIsNotAvailable("Camera is not supported by this device")
    }

    if (!isStarted()) {
      throw CameraIsNotStartedException("Camera is not started")
    }

    Timber.tag(tag).d("Taking a photo...")

    return suspendCoroutine { continuation ->
      try {
        val file = tempFile.asFile()

        camera!!.takePicture()
          .saveToFile(file)
          .whenAvailable {
            Timber.tag(tag).d("Photo has been taken")
            continuation.resume(Unit)
          }
      } catch (error: Throwable) {
        Timber.e(error)
        continuation.resumeWithException(error)
      }
    }
  }

  class CameraIsNotStartedException(msg: String) : Exception(msg)
  class CameraIsNotAvailable(msg: String) : Exception(msg)
}