package com.kirakishou.photoexchange.ui.activity

import android.arch.lifecycle.ViewModelProviders
import android.content.Intent
import android.os.Bundle
import android.support.design.widget.FloatingActionButton
import android.view.View
import butterknife.BindView
import com.kirakishou.fixmypc.photoexchange.R
import com.kirakishou.photoexchange.PhotoExchangeApplication
import com.kirakishou.photoexchange.base.BaseActivity
import com.kirakishou.photoexchange.di.component.DaggerTakePhotoActivityComponent
import com.kirakishou.photoexchange.di.module.TakePhotoActivityModule
import com.kirakishou.photoexchange.helper.PhotoResolutionSelector
import com.kirakishou.photoexchange.helper.concurrency.coroutine.CoroutineThreadPoolProvider
import com.kirakishou.photoexchange.mvp.view.TakePhotoActivityView
import com.kirakishou.photoexchange.mvp.viewmodel.TakePhotoActivityViewModel
import com.kirakishou.photoexchange.mvp.viewmodel.factory.TakePhotoActivityViewModelFactory
import io.fotoapparat.Fotoapparat
import io.fotoapparat.configuration.CameraConfiguration
import io.fotoapparat.selector.back
import io.fotoapparat.view.CameraView
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import kotlinx.coroutines.experimental.async
import timber.log.Timber
import java.io.File
import java.lang.ref.WeakReference
import javax.inject.Inject

class TakePhotoActivity : BaseActivity<TakePhotoActivityViewModel>(), TakePhotoActivityView {

    @BindView(R.id.camera_view)
    lateinit var cameraView: CameraView

    @BindView(R.id.take_photo_button)
    lateinit var takePhotoButton: FloatingActionButton

    @Inject
    lateinit var viewModelFactory: TakePhotoActivityViewModelFactory

    @Inject
    lateinit var coroutinesPool: CoroutineThreadPoolProvider

    lateinit var fotoapparat: Fotoapparat

    override fun initViewModel(): TakePhotoActivityViewModel? {
        return ViewModelProviders.of(this, viewModelFactory).get(TakePhotoActivityViewModel::class.java)
    }

    override fun getContentView(): Int = R.layout.activity_take_photo

    override fun onActivityCreate(savedInstanceState: Bundle?, intent: Intent) {
        takePhotoButton.setOnClickListener { getViewModel().takePhoto() }

        initCamera()
    }

    private fun initCamera() {
        val configuration = CameraConfiguration(
            pictureResolution = { PhotoResolutionSelector(this).select() }
        )

        fotoapparat = Fotoapparat(
            context = this,
            view = cameraView,
            lensPosition = back(),
            cameraConfiguration = configuration
        )
    }

    override fun onActivityDestroy() {
    }

    override fun onResume() {
        super.onResume()

        fotoapparat.start()
    }

    override fun onPause() {
        super.onPause()

        fotoapparat.stop()
    }

    override fun takePhoto(file: File): Single<Boolean> {
        val single = Single.create<Boolean> { emitter ->
            println("Taking photo...")

            try {
                fotoapparat.takePicture()
                    .saveToFile(file)
                    .whenAvailable {
                        emitter.onSuccess(true)
                    }
            } catch (error: Throwable) {
                Timber.e(error)
                emitter.onError(error)
            }

            println("Photo has been taken")
        }

        return single
            .subscribeOn(AndroidSchedulers.mainThread())
            .observeOn(AndroidSchedulers.mainThread())
    }

    override fun showTakePhotoButton() {
        async(coroutinesPool.provideMain()) {
            takePhotoButton.visibility = View.VISIBLE
        }
    }

    override fun hideTakePhotoButton() {
        async(coroutinesPool.provideMain()) {
            takePhotoButton.visibility = View.GONE
        }
    }

    override fun resolveDaggerDependency() {
        DaggerTakePhotoActivityComponent.builder()
            .applicationComponent(PhotoExchangeApplication.applicationComponent)
            .takePhotoActivityModule(TakePhotoActivityModule(WeakReference(this)))
            .build()
            .inject(this)
    }
}
