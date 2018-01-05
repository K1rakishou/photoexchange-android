package com.kirakishou.photoexchange.helper

import android.content.Context
import com.kirakishou.photoexchange.helper.util.BitmapUtils
import com.kirakishou.photoexchange.mwvm.model.exception.CouldNotTakePhotoException
import io.fotoapparat.Fotoapparat
import io.fotoapparat.parameter.ScaleType
import io.fotoapparat.parameter.selector.LensPositionSelectors
import io.fotoapparat.parameter.update.UpdateRequest
import io.fotoapparat.view.CameraView
import io.reactivex.ObservableEmitter
import timber.log.Timber

/**
 * Created by kirakishou on 1/4/2018.
 */
class CameraProvider {

    @Volatile
    private var isStarted = false
    private lateinit var camera: Fotoapparat

    fun provideCamera(context: Context, cameraView: CameraView) {
        camera = Fotoapparat
                .with(context)
                .into(cameraView)
                .previewScaleType(ScaleType.CENTER_CROP)
                .photoSize(PhotoSizeSelector())
                .lensPosition(LensPositionSelectors.back())
                .build()
    }

    fun startCamera() {
        if (!isStarted) {
            camera.start()
            isStarted = true
        }
    }

    fun stopCamera() {
        if (isStarted) {
            camera.stop()
            isStarted = false
        }
    }

    fun isStarted(): Boolean = isStarted
    fun isAvailable(): Boolean = camera.isAvailable

    fun takePicture(emitter: ObservableEmitter<String>) {
        if (isStarted) {
            camera.takePicture()
                    .toBitmap()
                    .transform { bitmapPhoto ->
                        Timber.d("Applying rotation ${bitmapPhoto.rotationDegrees}")
                        return@transform BitmapUtils.rotateBitmap(bitmapPhoto.bitmap, bitmapPhoto.rotationDegrees)
                    }
                    .whenAvailable { rotatedPhotoFickle ->
                        Timber.d("takePhoto() Done")

                        if (!rotatedPhotoFickle.isPresent()) {
                            emitter.onError(CouldNotTakePhotoException())
                        } else {
                            emitter.onNext(rotatedPhotoFickle.get())
                        }
                    }
        } else {
            emitter.onError(IllegalStateException("Camera is not started"))
        }
    }
}