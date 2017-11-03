package com.kirakishou.photoexchange.ui.activity

import android.arch.lifecycle.ViewModelProviders
import android.content.Intent
import android.os.Bundle
import android.support.design.widget.FloatingActionButton
import android.support.v7.app.AppCompatActivity
import butterknife.BindView
import butterknife.ButterKnife
import com.jakewharton.rxbinding2.view.RxView
import com.kirakishou.fixmypc.photoexchange.R
import com.kirakishou.photoexchange.PhotoExchangeApplication
import com.kirakishou.photoexchange.base.BaseActivity
import com.kirakishou.photoexchange.di.component.DaggerMainActivityComponent
import com.kirakishou.photoexchange.di.module.NetworkModule
import com.kirakishou.photoexchange.helper.api.ApiService
import com.kirakishou.photoexchange.mvvm.model.LonLat
import com.kirakishou.photoexchange.mvvm.viewmodel.MainActivityViewModel
import com.kirakishou.photoexchange.mvvm.viewmodel.factory.MainActivityViewModelFactory
import io.fotoapparat.Fotoapparat
import io.fotoapparat.parameter.ScaleType
import io.fotoapparat.parameter.selector.LensPositionSelectors.back
import io.fotoapparat.parameter.selector.SizeSelectors.biggestSize
import io.fotoapparat.view.CameraView
import io.nlopez.smartlocation.SmartLocation
import io.reactivex.FlowableEmitter
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.Observables
import io.reactivex.rxkotlin.plusAssign
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.PublishSubject
import java.io.File
import javax.inject.Inject


class MainActivity : BaseActivity<MainActivityViewModel>() {

    @BindView(R.id.camera_view)
    lateinit var cameraView: CameraView

    @BindView(R.id.take_photo_button)
    lateinit var takePhotoButton: FloatingActionButton

    @Inject
    lateinit var viewModelFactory: MainActivityViewModelFactory

    lateinit var fotoapparat: Fotoapparat

    private val photoAvailabilitySubject = PublishSubject.create<File>()
    private val locationSubject = PublishSubject.create<LonLat>()

    override fun initViewModel() =
            ViewModelProviders.of(this, viewModelFactory).get(MainActivityViewModel::class.java)

    override fun getContentView(): Int = R.layout.activity_main

    override fun onActivityCreate(savedInstanceState: Bundle?, intent: Intent) {
        initRx()
        initCamera()
    }

    override fun onActivityDestroy() {
        SmartLocation.with(this)
                .location()
                .stop()
    }

    override fun onStart() {
        super.onStart()
        fotoapparat.start()
    }

    override fun onStop() {
        super.onStop()
        fotoapparat.stop()
    }

    private fun initCamera() {
        fotoapparat = Fotoapparat
                .with(this)
                .into(cameraView)
                .previewScaleType(ScaleType.CENTER_CROP)
                .photoSize(biggestSize())
                .lensPosition(back())
                .build()
    }

    private fun initRx() {
        compositeDisposable += RxView.clicks(takePhotoButton)
                .subscribeOn(AndroidSchedulers.mainThread())
                .subscribe({ takePhoto() })

        compositeDisposable += Observables.zip(locationSubject, photoAvailabilitySubject)
                .subscribeOn(Schedulers.io())
                .doOnError { unknownErrorsSubject.onNext(it) }
                .subscribe({ (location, photoFile) ->
                    getViewModel().inputs.sendPhoto(photoFile, location)
                })
    }

    fun takePhoto() {
        val tempFile = File.createTempFile("temp", "file")

        fotoapparat.takePicture()
                .saveToFile(tempFile)
                .whenAvailable {
                    photoAvailabilitySubject.onNext(tempFile)
                }
    }

    fun getLocation() {
        SmartLocation.with(this)
                .location()
                .oneFix()
                .start {
                    val lonlat = LonLat(it.longitude, it.latitude)
                    locationSubject.onNext(lonlat)
                }
    }

    override fun resolveDaggerDependency() {
        DaggerMainActivityComponent.builder()
                .applicationComponent(PhotoExchangeApplication.applicationComponent)
                .build()
                .inject(this)
    }
}






















