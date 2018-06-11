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
import io.fotoapparat.selector.exactFixedFps
import io.fotoapparat.selector.highestResolution
import io.fotoapparat.selector.manualJpegQuality
import io.fotoapparat.view.CameraView
import io.reactivex.Single
import io.reactivex.schedulers.Schedulers
import timber.log.Timber
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
            .subscribeOn(Schedulers.computation())
    }

    fun takePhoto(): Single<ErrorCode.TakePhotoErrors> {
        return Single.just(Unit)
            .subscribeOn(Schedulers.computation())
            .doOnSuccess {
                //we need to delete all photos with state PHOTO_TAKEN because at this step they are being considered corrupted
                takenPhotosRepository.deleteAllWithState(PhotoState.PHOTO_TAKEN)

                //delete photo files that were marked as deleted earlier than (CURRENT_TIME - OLD_PHOTO_TIME_THRESHOLD)
                tempFilesRepository.deleteOld()

                //delete photo files that have no takenPhotoId
                tempFilesRepository.deleteEmptyTempFiles()

                //in case the user takes photos way too often and they weight a lot (like 3-4 mb per photo)
                //we need to consider this as well so we delete them when total files size exceeds MAX_CACHE_SIZE
                tempFilesRepository.deleteOldIfCacheSizeIsTooBig()
            }
            .flatMap {
                val tempFile = tempFilesRepository.create()

                return@flatMap doTakePhoto(tempFile)
                    .flatMap { takePhotoStatus ->
                        if (!takePhotoStatus) {
                            deletePhotoFile(tempFile)
                            return@flatMap Single.just(ErrorCode.TakePhotoErrors.CouldNotTakePhoto())
                        }

                        return@flatMap Single.fromCallable { takenPhotosRepository.saveTakenPhoto(tempFile) }
                            .subscribeOn(Schedulers.computation())
                            .map { takenPhoto ->
                                if (takenPhoto.isEmpty()) {
                                    cleanUp(takenPhoto)
                                    return@map ErrorCode.TakePhotoErrors.DatabaseError()
                                }

                                return@map ErrorCode.TakePhotoErrors.Ok(takenPhoto)
                            }
                    }
                    .onErrorReturn { error ->
                        Timber.tag(TAG).e(error)
                        return@onErrorReturn handleException(error)
                    }
            }

    }

    private fun deletePhotoFile(tempFile: TempFileEntity) {
        tempFilesRepository.markDeletedById(tempFile)
    }

    private fun cleanUp(photo: TakenPhoto?) {
        takenPhotosRepository.deleteMyPhoto(photo)
    }

    private fun handleException(error: Throwable): ErrorCode.TakePhotoErrors {
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