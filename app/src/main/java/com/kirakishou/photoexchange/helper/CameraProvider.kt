package com.kirakishou.photoexchange.helper

import android.content.Context
import com.kirakishou.photoexchange.helper.concurrency.coroutines.DispatchersProvider
import com.kirakishou.photoexchange.helper.database.entity.TempFileEntity
import com.kirakishou.photoexchange.helper.database.repository.TakenPhotosRepository
import com.kirakishou.photoexchange.mvp.model.photo.TakenPhoto
import io.fotoapparat.Fotoapparat
import io.fotoapparat.configuration.CameraConfiguration
import io.fotoapparat.selector.*
import io.fotoapparat.view.CameraView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.lang.RuntimeException
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

/**
 * Created by kirakishou on 1/4/2018.
 */
open class CameraProvider(
  private val context: Context,
  private val takenPhotosRepository: TakenPhotosRepository,
  private val dispatchersProvider: DispatchersProvider
) : CoroutineScope {
  private val TAG = "CameraProvider"
  private val isStarted = AtomicBoolean(false)
  private var camera: Fotoapparat? = null

  private val job = Job()

  override val coroutineContext: CoroutineContext
    get() = job + dispatchersProvider.GENERAL()

  private fun createConfiguration(): CameraConfiguration {
    return CameraConfiguration(
      previewResolution = highestResolution(),
      previewFpsRange = highestFps(),
      jpegQuality = manualJpegQuality(100)
    )
  }

  fun initCamera(cameraView: CameraView) {
    if (camera != null) {
      return
    }

    camera = Fotoapparat(
      context = context,
      view = cameraView,
      lensPosition = back(),
      cameraConfiguration = createConfiguration()
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

  fun onDestroy() {
    job.cancel()
  }

  fun isAvailable(): Boolean = camera?.isAvailable(back()) ?: false

  suspend fun takePhoto(): TakenPhoto? {
    return withContext(coroutineContext) {
      Timber.tag(TAG).d("before cleanup")
      takenPhotosRepository.cleanup()
      Timber.tag(TAG).d("after cleanup")

      val tempFile = takenPhotosRepository.createTempFile()

      try {
        takePhotoInternal(tempFile)
      } catch (error: Throwable) {
        Timber.tag(TAG).d("takePhotoInternal returned false")

        takenPhotosRepository.markDeletedById(tempFile)
        return@withContext null
      }

      Timber.tag(TAG).d("before saveTakenPhoto")
      //TODO: move to usecase?
      val takenPhoto = takenPhotosRepository.saveTakenPhoto(tempFile)
      Timber.tag(TAG).d("after saveTakenPhoto")

      if (takenPhoto == null) {
        Timber.tag(TAG).d("saveTakenPhoto returned empty photo")

        //TODO: transaction
        takenPhotosRepository.markDeletedById(tempFile)
        takenPhotosRepository.deleteMyPhoto(takenPhoto)
        return@withContext null
      }

      return@withContext takenPhoto
    }
  }

  private suspend fun takePhotoInternal(tempFile: TempFileEntity) {
    if (!isAvailable()) {
      throw CameraIsNotAvailable("Camera is not supported by this device")
    }

    if (!isStarted.get()) {
      throw CameraIsNotStartedException("Camera is not started")
    }

    Timber.tag(TAG).d("Taking a photo...")

    return suspendCoroutine { continuation ->
      try {
        val file = tempFile.asFile()

        camera!!.takePicture()
          .saveToFile(file)
          .whenAvailable {
            Timber.tag(TAG).d("Photo has been taken")
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