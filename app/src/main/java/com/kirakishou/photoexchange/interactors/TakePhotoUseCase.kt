package com.kirakishou.photoexchange.interactors

import com.kirakishou.photoexchange.helper.CameraProvider
import com.kirakishou.photoexchange.helper.concurrency.coroutines.DispatchersProvider
import com.kirakishou.photoexchange.helper.database.MyDatabase
import com.kirakishou.photoexchange.helper.database.entity.TempFileEntity
import com.kirakishou.photoexchange.helper.database.repository.TakenPhotosRepository
import com.kirakishou.photoexchange.mvp.model.photo.TakenPhoto
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.lang.ref.WeakReference
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class TakePhotoUseCase(
  private val database: MyDatabase,
  private val takenPhotosRepository: TakenPhotosRepository,
  dispatchersProvider: DispatchersProvider
) : BaseUseCase(dispatchersProvider) {
  private val TAG = "TakePhotoUseCase"

  suspend fun takePhoto(cameraProvider: WeakReference<CameraProvider>): TakenPhoto? {
    return withContext(coroutineContext) {
      takenPhotosRepository.cleanup()

      val tempFile = takenPhotosRepository.createTempFile()

      try {
        if (!takePhotoInternal(tempFile, cameraProvider)) {
          //canceled
          takenPhotosRepository.markDeletedById(tempFile)
          return@withContext null
        }
      } catch (error: Throwable) {
        Timber.tag(TAG).d("takePhotoInternal returned false")

        takenPhotosRepository.markDeletedById(tempFile)
        return@withContext null
      }

      val takenPhoto = takenPhotosRepository.saveTakenPhoto(tempFile)
      if (takenPhoto == null) {
        Timber.tag(TAG).d("saveTakenPhoto returned empty photo")

        database.transactional {
          takenPhotosRepository.markDeletedById(tempFile)
          takenPhotosRepository.deleteMyPhoto(takenPhoto)
        }

        return@withContext null
      }

      return@withContext takenPhoto
    }
  }

  private suspend fun takePhotoInternal(
    tempFile: TempFileEntity,
    cameraProvider: WeakReference<CameraProvider>
  ): Boolean {
    val camera = cameraProvider.get()

    if (camera == null) {
      //cancel since camera is no longer available
      return false
    }

    if (!camera.isStarted()) {
      throw CameraIsNotStartedException("Camera is not started")
    }

    if (!camera.isAvailable()) {
      throw CameraIsNotAvailable("Camera is not supported by this device")
    }

    Timber.tag(TAG).d("Taking a photo...")

    return suspendCancellableCoroutine { continuation ->
      try {
        val file = tempFile.asFile()

        cameraProvider.get()?.takePicture()
          ?.saveToFile(file)
          ?.whenAvailable {
            if (cameraProvider.get() == null) {
              Timber.tag(TAG).d("Take photo canceled")
              continuation.cancel()
              return@whenAvailable
            }

            Timber.tag(TAG).d("Photo has been taken")
            continuation.resume(true)
          }
          ?: continuation.resume(false)
      } catch (error: Throwable) {
        Timber.e(error)
        continuation.resumeWithException(error)
      }
    }
  }

  class CameraIsNotStartedException(msg: String) : Exception(msg)
  class CameraIsNotAvailable(msg: String) : Exception(msg)

}