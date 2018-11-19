package com.kirakishou.photoexchange.helper

import android.content.Context
import com.kirakishou.photoexchange.helper.database.entity.TempFileEntity
import com.kirakishou.photoexchange.helper.database.repository.TakenPhotosRepository
import com.kirakishou.photoexchange.helper.database.repository.TempFileRepository
import com.kirakishou.photoexchange.mvp.model.PhotoState
import com.kirakishou.photoexchange.mvp.model.TakenPhoto
import io.fotoapparat.Fotoapparat
import io.fotoapparat.configuration.CameraConfiguration
import io.fotoapparat.selector.back
import io.fotoapparat.selector.exactFixedFps
import io.fotoapparat.selector.highestResolution
import io.fotoapparat.selector.manualJpegQuality
import io.fotoapparat.view.CameraView
import timber.log.Timber
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

/**
 * Created by kirakishou on 1/4/2018.
 */
open class CameraProvider(
  val context: Context,
  private val takenPhotosRepository: TakenPhotosRepository,
  private val tempFilesRepository: TempFileRepository
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
    if (!isStarted()) {
      if (camera != null) {
        camera!!.start()
        isStarted.set(true)
      }
    }
  }

  fun stopCamera() {
    if (isStarted()) {
      isStarted.set(false)
      if (camera != null) {
        camera!!.stop()
      }
    }
  }

  fun isStarted(): Boolean = isStarted.get()
  fun isAvailable(): Boolean = camera?.isAvailable(back()) ?: false

  suspend fun takePhoto(): TakenPhoto {
    takenPhotosRepository.cleanup()

    val tempFile = tempFilesRepository.create()
    val takePhotoStatus = takePhotoInternal(tempFile)

    if (!takePhotoStatus) {
      deletePhotoFile(tempFile)
      return TakenPhoto.empty()
    }

    val takenPhoto = takenPhotosRepository.saveTakenPhoto(tempFile)
    if (takenPhoto.isEmpty()) {
      deletePhotoFile(tempFile) //TODO: should this be here?
      cleanUp(takenPhoto)
      return TakenPhoto.empty()
    }

    return takenPhoto
  }

  private suspend fun takePhotoInternal(tempFile: TempFileEntity): Boolean {
    if (!isAvailable()) {
      throw CameraIsNotAvailable("Camera is not supported by this device")
    }

    if (!isStarted()) {
      throw CameraIsNotStartedException("Camera is not started")
    }

    Timber.tag(tag).d("Taking photo...")

    return suspendCoroutine { continuation ->
      try {
        val file = tempFile.asFile()

        camera!!.takePicture()
          .saveToFile(file)
          .whenAvailable {
            Timber.tag(tag).d("Photo has been taken")
            continuation.resume(true)
          }
      } catch (error: Throwable) {
        Timber.e(error)
        continuation.resume(false)
      }
    }
  }

  private suspend fun deletePhotoFile(tempFile: TempFileEntity) {
    tempFilesRepository.markDeletedById(tempFile)
  }

  private suspend fun cleanUp(photo: TakenPhoto?) {
    takenPhotosRepository.deleteMyPhoto(photo)
  }

  class CameraIsNotStartedException(msg: String) : Exception(msg)
  class CameraIsNotAvailable(msg: String) : Exception(msg)
}