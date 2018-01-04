package com.kirakishou.photoexchange.helper

import android.content.Context
import android.graphics.Bitmap
import io.fotoapparat.Fotoapparat
import io.fotoapparat.parameter.ScaleType
import io.fotoapparat.parameter.selector.LensPositionSelectors
import io.fotoapparat.photo.BitmapPhoto
import io.fotoapparat.view.CameraView

/**
 * Created by kirakishou on 1/4/2018.
 */
class CameraProvider {

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
        }
    }

    fun stopCamera() {
        if (isStarted) {
            camera.stop()
        }
    }

    fun isAvailable(): Boolean {
        return camera.isAvailable
    }

    fun takePicture(): BitmapPhoto {
        if (isStarted) {
            return camera.takePicture()
                    .toBitmap()
                    .await()
        }

        throw IllegalStateException("Camera is not started")
    }
}