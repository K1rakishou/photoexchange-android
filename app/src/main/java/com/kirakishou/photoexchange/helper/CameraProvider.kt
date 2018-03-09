package com.kirakishou.photoexchange.helper

import android.content.Context
import io.fotoapparat.Fotoapparat
import io.fotoapparat.characteristic.toCameraId
import io.fotoapparat.configuration.CameraConfiguration
import io.fotoapparat.parameter.ScaleType
import io.fotoapparat.selector.back
import io.fotoapparat.selector.front
import io.fotoapparat.view.CameraView
import io.reactivex.ObservableEmitter
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import timber.log.Timber
import java.io.File

/**
 * Created by kirakishou on 1/4/2018.
 */
open class CameraProvider(
    val context: Context
) {

    @Volatile
    private var isStarted = false
    private var camera: Fotoapparat? = null
    private val tag = "[${this::class.java.simpleName}]: "
    private val cameraLensPosition = back()

    fun provideCamera(cameraView: CameraView) {
        val configuration = CameraConfiguration(
            pictureResolution = { PhotoResolutionSelector(this).select() }
        )

        camera = Fotoapparat(
            context = context,
            view = cameraView,
            lensPosition = cameraLensPosition,
            cameraConfiguration = configuration
        )
    }

    fun startCamera(): Boolean {
        if (!isStarted() && isAvailable()) {
            camera?.apply {
                this.start()
                isStarted = true

                return true
            }
        }

        return false
    }

    fun stopCamera() {
        if (isStarted() && isAvailable()) {
            camera?.apply {
                this.stop()
                isStarted = false
            }
        }
    }

    fun isStarted(): Boolean = isStarted
    fun isAvailable(): Boolean = camera?.isAvailable(cameraLensPosition) ?: false

    fun takePhoto(file: File): Single<Boolean> {
        val single = Single.create<Boolean> { emitter ->
            if (!isAvailable()) {
                emitter.onError(CameraInNotAvailable("Camera is not supported by this device"))
                return@create
            }

            if (!isStarted()) {
                emitter.onError(CameraIsNotStartedException("Camera is not started"))
                return@create
            }

            Timber.tag(tag).d("Taking photo...")

            try {
                camera!!.takePicture()
                    .saveToFile(file)
                    .whenAvailable {
                        emitter.onSuccess(true)
                    }
            } catch (error: Throwable) {
                Timber.e(error)
                emitter.onError(error)
            }

            Timber.tag(tag).d("Photo has been taken")
        }

        return single
            .subscribeOn(AndroidSchedulers.mainThread())
            .observeOn(AndroidSchedulers.mainThread())
    }

    class CameraIsNotStartedException(msg: String) : Exception(msg)
    class CameraInNotAvailable(msg: String) : Exception(msg)
}