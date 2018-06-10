package com.kirakishou.photoexchange.helper

import android.content.Context
import com.kirakishou.photoexchange.helper.database.entity.TempFileEntity
import com.kirakishou.photoexchange.helper.database.repository.TakenPhotosRepository
import com.kirakishou.photoexchange.helper.database.repository.TempFileRepository
import com.kirakishou.photoexchange.mvp.model.PhotoState
import com.kirakishou.photoexchange.mvp.model.TakenPhoto
import com.kirakishou.photoexchange.mvp.model.other.ErrorCode
import io.fotoapparat.Fotoapparat
import io.fotoapparat.configuration.CameraConfiguration
import io.fotoapparat.selector.back
import io.fotoapparat.selector.highestResolution
import io.fotoapparat.selector.manualJpegQuality
import io.fotoapparat.view.CameraView
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.schedulers.Schedulers
import kotlinx.coroutines.experimental.CommonPool
import kotlinx.coroutines.experimental.async
import kotlinx.coroutines.experimental.rx2.asSingle
import kotlinx.coroutines.experimental.rx2.await
import kotlinx.coroutines.experimental.rx2.rxObservable
import kotlinx.coroutines.experimental.rx2.rxSingle
import timber.log.Timber
import java.io.File
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException
import java.util.concurrent.atomic.AtomicBoolean

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

    fun provideCamera(cameraView: CameraView) {
        if (camera != null) {
            return
        }

        val configuration = CameraConfiguration(
            previewResolution = highestResolution(),
            jpegQuality = manualJpegQuality(100)
        )

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

    private fun doTakePhoto(tempFile: TempFileEntity): Single<Boolean> {
        val single = Single.create<Boolean> { emitter ->
            if (!isAvailable()) {
                emitter.onError(CameraIsNotAvailable("Camera is not supported by this device"))
                return@create
            }

            if (!isStarted()) {
                emitter.onError(CameraIsNotStartedException("Camera is not started"))
                return@create
            }

            Timber.tag(tag).d("Taking photo...")

            try {
                val file = tempFile.asFile()

                camera!!.takePicture()
                    .saveToFile(file)
                    .whenAvailable {
                        Timber.tag(tag).d("Photo has been taken")
                        emitter.onSuccess(true)
                    }
            } catch (error: Throwable) {
                Timber.e(error)
                emitter.onError(error)
            }
        }

        return single
            .subscribeOn(Schedulers.io())
            .observeOn(Schedulers.io())
    }

    fun takePhoto(): Single<ErrorCode.TakePhotoErrors> {
        return rxSingle {
            var takenPhoto: TakenPhoto = TakenPhoto.empty()

            try {
                takenPhotosRepository.deleteAllWithState(PhotoState.PHOTO_TAKEN)
                takenPhotosRepository.deleteOldPhotoFiles()

                val tempFile = tempFilesRepository.create()
                val takePhotoStatus = doTakePhoto(tempFile).await()

                if (!takePhotoStatus) {
                    deletePhotoFile(tempFile)
                    return@rxSingle ErrorCode.TakePhotoErrors.CouldNotTakePhoto()
                }

                takenPhoto = takenPhotosRepository.saveTakenPhoto(tempFile)
                if (takenPhoto.isEmpty()) {
                    cleanUp(takenPhoto)
                    return@rxSingle ErrorCode.TakePhotoErrors.DatabaseError()
                }

                return@rxSingle ErrorCode.TakePhotoErrors.Ok(takenPhoto)
            } catch (error: Exception) {
                Timber.tag(TAG).e(error)

                cleanUp(takenPhoto)
                return@rxSingle handleException(error)
            }
        }
    }

    private fun deletePhotoFile(tempFile: TempFileEntity) {
        tempFilesRepository.markDeletedById(tempFile)
    }

    private fun cleanUp(photo: TakenPhoto?) {
        takenPhotosRepository.deleteMyPhoto(photo)
    }

    private fun handleException(error: Exception): ErrorCode.TakePhotoErrors {
        return when (error.cause) {
            null -> ErrorCode.TakePhotoErrors.DatabaseError()

            else -> when (error.cause!!) {
                is CameraProvider.CameraIsNotAvailable -> ErrorCode.TakePhotoErrors.CameraIsNotAvailable()
                is CameraProvider.CameraIsNotStartedException -> ErrorCode.TakePhotoErrors.CameraIsNotStartedException()
                is TimeoutException -> ErrorCode.TakePhotoErrors.TimeoutException()
                else -> ErrorCode.TakePhotoErrors.UnknownError()
            }
        }
    }

    class CameraIsNotStartedException(msg: String) : Exception(msg)
    class CameraIsNotAvailable(msg: String) : Exception(msg)
}