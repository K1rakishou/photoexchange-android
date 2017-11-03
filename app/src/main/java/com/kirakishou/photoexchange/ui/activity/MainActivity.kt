package com.kirakishou.photoexchange.ui.activity

import android.os.Bundle
import android.support.design.widget.FloatingActionButton
import android.support.v7.app.AppCompatActivity
import butterknife.BindView
import butterknife.ButterKnife
import com.jakewharton.rxbinding2.view.RxView
import com.kirakishou.fixmypc.photoexchange.R
import io.fotoapparat.Fotoapparat
import io.fotoapparat.parameter.ScaleType
import io.fotoapparat.parameter.selector.LensPositionSelectors.back
import io.fotoapparat.parameter.selector.SizeSelectors.biggestSize
import io.fotoapparat.view.CameraView
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.plusAssign
import java.io.File


class MainActivity : AppCompatActivity() {

    @BindView(R.id.camera_view)
    lateinit var cameraView: CameraView

    @BindView(R.id.take_photo_button)
    lateinit var takePhotoButton: FloatingActionButton

    private val mCompositeDisposable = CompositeDisposable()

    lateinit var fotoapparat: Fotoapparat

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        ButterKnife.bind(this)

        fotoapparat = Fotoapparat
                .with(this)
                .into(cameraView)
                .previewScaleType(ScaleType.CENTER_CROP)
                .photoSize(biggestSize())
                .lensPosition(back())
                .build()

        mCompositeDisposable += RxView.clicks(takePhotoButton)
                .subscribeOn(AndroidSchedulers.mainThread())
                .subscribe({ takePhoto() })
    }

    override fun onStart() {
        super.onStart()
        fotoapparat.start()
    }

    override fun onStop() {
        super.onStop()
        fotoapparat.stop()
    }

    override fun onDestroy() {
        super.onDestroy()
        mCompositeDisposable.clear()
    }

    fun takePhoto() {
        val tempFile = File.createTempFile("temp", "file")

        fotoapparat.takePicture()
                .saveToFile(tempFile)
                .whenAvailable {

                }
    }
}






















