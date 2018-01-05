package com.kirakishou.photoexchange.ui.activity

import android.Manifest
import android.arch.lifecycle.ViewModelProviders
import android.content.Intent
import android.os.Bundle
import android.support.design.widget.FloatingActionButton
import android.support.v7.widget.CardView
import android.view.View
import android.view.animation.AnticipateInterpolator
import android.view.animation.OvershootInterpolator
import android.widget.ImageView
import android.widget.TextView
import butterknife.BindView
import com.afollestad.materialdialogs.MaterialDialog
import com.jakewharton.rxbinding2.view.RxView
import com.karumi.dexter.Dexter
import com.karumi.dexter.MultiplePermissionsReport
import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.PermissionRequest
import com.karumi.dexter.listener.multi.MultiplePermissionsListener
import com.kirakishou.fixmypc.photoexchange.R
import com.kirakishou.photoexchange.PhotoExchangeApplication
import com.kirakishou.photoexchange.base.BaseActivity
import com.kirakishou.photoexchange.di.component.DaggerTakePhotoActivityComponent
import com.kirakishou.photoexchange.helper.CameraProvider
import com.kirakishou.photoexchange.helper.extension.mySetListener
import com.kirakishou.photoexchange.helper.location.MyLocationManager
import com.kirakishou.photoexchange.helper.location.RxLocationManager
import com.kirakishou.photoexchange.helper.preference.AppSharedPreference
import com.kirakishou.photoexchange.helper.preference.UserInfoPreference
import com.kirakishou.photoexchange.helper.util.Utils
import com.kirakishou.photoexchange.mwvm.model.other.LonLat
import com.kirakishou.photoexchange.mwvm.model.other.TakenPhoto
import com.kirakishou.photoexchange.mwvm.viewmodel.TakePhotoActivityViewModel
import com.kirakishou.photoexchange.mwvm.viewmodel.factory.TakePhotoActivityViewModelFactory
import io.fotoapparat.view.CameraView
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.rxkotlin.Observables
import io.reactivex.rxkotlin.plusAssign
import io.reactivex.subjects.BehaviorSubject
import timber.log.Timber
import java.util.concurrent.TimeUnit
import javax.inject.Inject

class TakePhotoActivity : BaseActivity<TakePhotoActivityViewModel>() {

    @BindView(R.id.iv_show_all_photos)
    lateinit var ivShowAllPhotos: ImageView

    @BindView(R.id.camera_view)
    lateinit var cameraView: CameraView

    @BindView(R.id.take_photo_button)
    lateinit var takePhotoButton: FloatingActionButton

    @Inject
    lateinit var viewModelFactory: TakePhotoActivityViewModelFactory

    @Inject
    lateinit var appSharedPreference: AppSharedPreference

    private val tag = "[${this::class.java.simpleName}]: "
    private val ON_RESUME = 0
    private val ON_PAUSE = 1

    private val cameraProvider = CameraProvider()

    private val userInfoPreference by lazy { appSharedPreference.prepare<UserInfoPreference>() }

    private val permissionsGrantedSubject = BehaviorSubject.create<Boolean>()
    private val lifecycleSubject = BehaviorSubject.create<Int>()

    override fun initViewModel(): TakePhotoActivityViewModel {
        return ViewModelProviders.of(this, viewModelFactory).get(TakePhotoActivityViewModel::class.java)
    }

    override fun getContentView(): Int = R.layout.activity_take_photo

    override fun onInitRx() {
        initRx()
    }

    override fun onActivityCreate(savedInstanceState: Bundle?, intent: Intent) {
        userInfoPreference.load()

        getPermissions()
        generateOrReadUserId()

        getViewModel().inputs.cleanTakenPhotosDB()
        getViewModel().showDatabaseDebugInfo()
    }

    override fun onActivityDestroy() {
    }

    override fun onResume() {
        super.onResume()

        lifecycleSubject.onNext(ON_RESUME)
    }

    override fun onPause() {
        super.onPause()

        //we have to stop camera here because startOrStopCamera won't be executed
        if (cameraProvider.isStarted()) {
            Timber.tag(tag).d("startOrStopCamera()")
            cameraProvider.stopCamera()
            hideControls()
        }

        lifecycleSubject.onNext(ON_PAUSE)
        userInfoPreference.save()
    }

    private fun getPermissions() {
        Dexter.withActivity(this)
                .withPermissions(
                        Manifest.permission.CAMERA,
                        Manifest.permission.ACCESS_FINE_LOCATION)
                .withListener(object : MultiplePermissionsListener {
                    override fun onPermissionsChecked(report: MultiplePermissionsReport) {
                        val cameraPermissionDenied = report.deniedPermissionResponses.any { it.permissionName == Manifest.permission.CAMERA }
                        if (cameraPermissionDenied) {
                            Timber.tag(tag).d("getPermissions() Could not obtain camera permission")
                            showAppCannotWorkWithoutCameraPermissionDialog()
                            return
                        }

                        Timber.tag(tag).d("getPermissions() Got permissions")
                        permissionsGrantedSubject.onNext(true)
                    }

                    override fun onPermissionRationaleShouldBeShown(permissions: MutableList<PermissionRequest>, token: PermissionToken) {
                        showCameraRationale(token)
                    }

                }).check()
    }

    private fun showAppCannotWorkWithoutCameraPermissionDialog() {
        //TODO: change this to homemade dialog and get rid of the MaterialDialogs dependency
        MaterialDialog.Builder(this)
                .title("Error")
                .content("This app cannon work without a camera permission")
                .positiveText("OK")
                .onPositive { _, _ ->
                    finish()
                }
                .show()
    }

    private fun showCameraRationale(token: PermissionToken) {
        //TODO: change this to homemade dialog and get rid of the MaterialDialogs dependency
        MaterialDialog.Builder(this)
                .title("Why do we need permissions?")
                .content("We need camera permission so you can take a picture that will be sent to someone. " +
                        "We don't necessarily need gps permission so you can disable it but the person " +
                        "who receives your photo won't be able to see where it was taken.")
                .positiveText("Allow")
                .negativeText("Close app")
                .onPositive { _, _ ->
                    token.continuePermissionRequest()
                }
                .onNegative { _, _ ->
                    token.cancelPermissionRequest()
                }
                .show()
    }

    private fun showCameraIsNotAvailableDialog() {
        //TODO: change this to homemade dialog and get rid of the MaterialDialogs dependency
        MaterialDialog.Builder(this)
                .title("Camera is not available")
                .content("It looks like your device does not support camera. This app cannot work without a camera.")
                .positiveText("OK")
                .onPositive { _, _ ->
                    finish()
                }
                .show()
    }

    private fun initCamera(): Observable<Boolean> {
        return Observable.fromCallable {
            Timber.tag(tag).d("initCamera()")
            cameraProvider.provideCamera(this, cameraView)

            return@fromCallable true
        }
    }

    private fun initRx() {
        compositeDisposable += RxView.clicks(ivShowAllPhotos)
                .subscribeOn(AndroidSchedulers.mainThread())
                .subscribe({ switchToAllPhotosViewActivity() })

        val fotoapparatObservable = permissionsGrantedSubject
                .subscribeOn(AndroidSchedulers.mainThread())
                .flatMap { initCamera() }
                //FIXME:
                //WTF: I don't know why, but this works
                //
                //I've tried to use operator share(), but it didn't work -
                //the rx chain from "RxView.clicks(takePhotoButton)" would hang after "flatMap { fotoapparatObservable }"
                //
                //I've also tried to use publish() + autoconnect(2), but it also didn't work -
                //both "Observables.combineLatest(fotoapparatObservable, lifecycleSubject)" and "RxView.clicks(takePhotoButton)"
                //would hang until I click takePhotoButton button
                //
                //But cache works for some reason (why???)
                .cache()

        compositeDisposable += Observables.combineLatest(fotoapparatObservable, lifecycleSubject)
                .observeOn(AndroidSchedulers.mainThread())
                .doOnNext { (_, lifecycle) -> startOrStopCamera(lifecycle) }
                .subscribe()

        compositeDisposable += RxView.clicks(takePhotoButton)
                .observeOn(AndroidSchedulers.mainThread())
                .flatMap { fotoapparatObservable }
                .doOnNext { hideControls() }
                .flatMap { _ -> takePhoto() }
                .doOnNext(this::saveTakenPhoto)
                .doOnError(this::onUnknownError)
                .subscribe()

        compositeDisposable += getViewModel().outputs.onTakenPhotoSavedObservable()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({ savedTakenPhoto ->
                    switchToViewTakenPhotoActivity(savedTakenPhoto.id, savedTakenPhoto.location,
                            savedTakenPhoto.photoFilePath, savedTakenPhoto.userId)
                })

        compositeDisposable += getViewModel().errors.onUnknownErrorObservable()
                .subscribeOn(AndroidSchedulers.mainThread())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(this::onUnknownError)
    }

    private fun startOrStopCamera(lifecycle: Int) {
        if (!cameraProvider.isAvailable()) {
            if (lifecycle == ON_RESUME) {
                Timber.tag(tag).d("startOrStopCamera() Camera IS NOT available!!!")
                hideTakePhotoButton()
                hideShowAllPhotosButton()
                showCameraIsNotAvailableDialog()
            }
        } else {
            when (lifecycle) {
                ON_RESUME -> {
                    if (!cameraProvider.isStarted()) {
                        Timber.tag(tag).d("startOrStopCamera()")
                        cameraProvider.startCamera()
                        showControls()
                    }
                }
            }
        }
    }

    private fun saveTakenPhoto(photoFilePath: String) {
        val userId = userInfoPreference.getUserId()

        val photo = TakenPhoto.create(photoFilePath, userId)
        getViewModel().inputs.saveTakenPhoto(photo)
    }

    private fun hideShowAllPhotosButton() {
        ivShowAllPhotos.visibility = View.GONE
    }

    private fun hideTakePhotoButton() {
        takePhotoButton.visibility = View.GONE
    }

    private fun switchToAllPhotosViewActivity() {
        val intent = Intent(this, AllPhotosViewActivity::class.java)
        startActivity(intent)
    }

    private fun switchToViewTakenPhotoActivity(id: Long, location: LonLat, photoFilePath: String, userId: String) {
        val intent = Intent(this, ViewTakenPhotoActivity::class.java)
        intent.putExtra("id", id)
        intent.putExtra("lon", location.lon)
        intent.putExtra("lat", location.lat)
        intent.putExtra("photo_file_path", photoFilePath)
        intent.putExtra("user_id", userId)
        startActivity(intent)
    }

    private fun generateOrReadUserId() {
        if (!userInfoPreference.exists()) {
            Timber.tag(tag).d("generateOrReadUserId() App first run. Generating userId")
            userInfoPreference.setUserId(Utils.generateUserId())
        } else {
            Timber.tag(tag).d("generateOrReadUserId() UserId already exists")
        }
    }

    private fun takePhoto(): Observable<String> {
        return Observable.create { emitter ->
            Timber.tag(tag).d("takePhoto() Taking a photo...")
            return@create cameraProvider.takePicture(emitter)
        }
    }

    private fun showControls() {
        takePhotoButton.animate()
                .scaleX(1f)
                .scaleY(1f)
                .setDuration(500)
                .setInterpolator(OvershootInterpolator())
                .mySetListener {
                    onAnimationStart {
                        takePhotoButton.visibility = View.VISIBLE
                    }
                }
                .start()

        ivShowAllPhotos.animate()
                .scaleX(1f)
                .scaleY(1f)
                .setDuration(500)
                .setInterpolator(OvershootInterpolator())
                .mySetListener {
                    onAnimationStart {
                        ivShowAllPhotos.visibility = View.VISIBLE
                    }
                }
                .start()
    }

    private fun hideControls() {
        if (this::takePhotoButton.isInitialized) {
            takePhotoButton.animate()
                    .scaleX(0f)
                    .scaleY(0f)
                    .setDuration(500)
                    .setInterpolator(AnticipateInterpolator())
                    .start()
        }

        if (this::ivShowAllPhotos.isInitialized) {
            ivShowAllPhotos.animate()
                    .scaleX(0f)
                    .scaleY(0f)
                    .setDuration(500)
                    .setInterpolator(AnticipateInterpolator())
                    .start()
        }
    }

    override fun resolveDaggerDependency() {
        DaggerTakePhotoActivityComponent.builder()
                .applicationComponent(PhotoExchangeApplication.applicationComponent)
                .build()
                .inject(this)
    }
}




















