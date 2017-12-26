package com.kirakishou.photoexchange.ui.activity

import android.Manifest
import android.arch.lifecycle.ViewModelProviders
import android.content.Intent
import android.os.Bundle
import android.support.design.widget.FloatingActionButton
import android.support.v7.widget.CardView
import android.view.View
import android.widget.ImageView
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
import com.kirakishou.photoexchange.helper.location.MyLocationManager
import com.kirakishou.photoexchange.helper.location.RxLocationManager
import com.kirakishou.photoexchange.helper.preference.AppSharedPreference
import com.kirakishou.photoexchange.helper.preference.UserInfoPreference
import com.kirakishou.photoexchange.helper.util.Utils
import com.kirakishou.photoexchange.mwvm.model.other.LonLat
import com.kirakishou.photoexchange.mwvm.model.other.TakenPhoto
import com.kirakishou.photoexchange.mwvm.viewmodel.TakePhotoActivityViewModel
import com.kirakishou.photoexchange.mwvm.viewmodel.factory.TakePhotoActivityViewModelFactory
import io.fotoapparat.Fotoapparat
import io.fotoapparat.parameter.ScaleType
import io.fotoapparat.parameter.selector.LensPositionSelectors.back
import io.fotoapparat.parameter.selector.SizeSelectors.biggestSize
import io.fotoapparat.view.CameraView
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.rxkotlin.Observables
import io.reactivex.rxkotlin.combineLatest
import io.reactivex.rxkotlin.plusAssign
import io.reactivex.rxkotlin.zipWith
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.BehaviorSubject
import io.reactivex.subjects.PublishSubject
import timber.log.Timber
import java.io.File
import javax.inject.Inject

class TakePhotoActivity : BaseActivity<TakePhotoActivityViewModel>() {

    @BindView(R.id.iv_show_all_photos)
    lateinit var ivShowAllPhotos: ImageView

    @BindView(R.id.notification)
    lateinit var notification: CardView

    @BindView(R.id.camera_view)
    lateinit var cameraView: CameraView

    @BindView(R.id.take_photo_button)
    lateinit var takePhotoButton: FloatingActionButton

    @Inject
    lateinit var viewModelFactory: TakePhotoActivityViewModelFactory

    @Inject
    lateinit var appSharedPreference: AppSharedPreference

    private val tag = "[${this::class.java.simpleName}]: "
    private val ON_START = 0
    private val ON_STOP = 1

    private val userInfoPreference by lazy { appSharedPreference.prepare<UserInfoPreference>() }
    private val locationManager by lazy { MyLocationManager(applicationContext) }

    private val permissionsGrantedSubject = PublishSubject.create<Boolean>()
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

        getViewModel().showDatabaseDebugInfo()
    }

    override fun onActivityDestroy() {
    }

    override fun onPause() {
        super.onPause()
        userInfoPreference.save()
    }

    override fun onStart() {
        super.onStart()
        lifecycleSubject.onNext(ON_START)
    }

    override fun onStop() {
        lifecycleSubject.onNext(ON_STOP)
        super.onStop()
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
                .title("Why do we need these permissions?")
                .content("We need camera so you can take a picture to send it to someone. " +
                        "We don't necessarily need gps permission so you can disable it and the person " +
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
        MaterialDialog.Builder(this)
                .title("Camera is not available")
                .content("It look like your device does not support a camera. This app cannot work without a camera.")
                .positiveText("OK")
                .onPositive { _, _ ->
                    finish()
                }
                .show()
    }

    private fun initCamera(): Observable<Fotoapparat> {
        return Observable.fromCallable {
            Timber.tag(tag).d("initCamera() initCamera")

            return@fromCallable Fotoapparat
                    .with(applicationContext)
                    .into(cameraView)
                    .previewScaleType(ScaleType.CENTER_CROP)
                    .photoSize(biggestSize())
                    .lensPosition(back())
                    .build()
        }
    }

    private fun initRx() {
        val fotoapparatObservable = permissionsGrantedSubject
                .subscribeOn(AndroidSchedulers.mainThread())
                .flatMap { initCamera() }
                .share()

        compositeDisposable += Observables.combineLatest(fotoapparatObservable, lifecycleSubject)
                .doOnNext { (fotoapparat, lifecycle) -> startOrStopCamera(fotoapparat, lifecycle) }
                .subscribe()

        compositeDisposable += RxView.clicks(ivShowAllPhotos)
                .subscribeOn(AndroidSchedulers.mainThread())
                .subscribe({ switchToAllPhotosViewActivity() })

        compositeDisposable += RxView.clicks(takePhotoButton)
                .observeOn(Schedulers.io())
                .map { lifecycleSubject.value }
                .filter { lifecycle -> lifecycle == ON_START }
                .combineLatest(fotoapparatObservable)
                .map { it.second }
                .observeOn(AndroidSchedulers.mainThread())
                .doOnNext { showNotification() }
                //FIXME: for some reason fotoapparat doesn't want to work on the io schedulers,
                //so we have to take photo in a blocking way
                .flatMap { fotoapparat -> takePhoto(fotoapparat) }
                .observeOn(Schedulers.io())
                .zipWith(getLocationObservable())
                .doOnNext(this::saveTakenPhoto)
                .doOnError(this::onUnknownError)
                .subscribe()

        compositeDisposable += getViewModel().outputs.onTakenPhotoSavedObservable()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({ savedTakenPhoto ->
                    hideNotification()
                    switchToViewTakenPhotoActivity(savedTakenPhoto.id, savedTakenPhoto.location,
                            savedTakenPhoto.photoFilePath, savedTakenPhoto.userId)
                })

        compositeDisposable += getViewModel().errors.onUnknownErrorObservable()
                .subscribeOn(AndroidSchedulers.mainThread())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(this::onUnknownError)
    }

    private fun TakePhotoActivity.startOrStopCamera(fotoapparat: Fotoapparat, lifecycle: Int?) {
        if (!fotoapparat.isAvailable) {
            if (lifecycle == ON_START) {
                Timber.tag(tag).d("initRx() Camera IS NOT available!!!")
                hideTakePhotoButton()
                hideShowAllPhotosButton()
                showCameraIsNotAvailableDialog()
            }
        } else {
            when (lifecycle) {
                ON_START -> {
                    Timber.tag(tag).d("initRx() ON_START")
                    fotoapparat.start()
                }
                ON_STOP -> {
                    Timber.tag(tag).d("initRx() ON_STOP")
                    fotoapparat.stop()
                }
            }
        }
    }

    private fun saveTakenPhoto(it: Pair<String, LonLat>) {
        val photoFilePath = it.first
        val location = it.second
        val userId = userInfoPreference.getUserId()
        
        val photo = TakenPhoto.create(location, photoFilePath, userId)
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

    private fun takePhoto(fotoapparat: Fotoapparat): Observable<String> {
        return Observable.create<String> { emitter ->
            Timber.tag(tag).d("takePhoto() Taking a photo...")
            val tempFile = File.createTempFile("photo", ".tmp")

            fotoapparat.takePicture()
                    .saveToFile(tempFile)
                    .whenAvailable {
                        Timber.tag(tag).d("takePhoto() Done")
                        emitter.onNext(tempFile.absolutePath)
                    }
        }
    }

    private fun getLocationObservable(): Observable<LonLat> {
        val gpsStateObservable = Observable.fromCallable { locationManager.isGpsEnabled() }
                .share()

        val gpsEnabledObservable = gpsStateObservable
                .filter { isEnabled -> isEnabled }
                .doOnNext { Timber.tag(tag).d("getLocationObservable() Gps is enabled. Trying to obtain current location") }
                .flatMap {
                    return@flatMap RxLocationManager.start(locationManager)
                    /*.first(LonLat.empty())
                    .timeout(5, TimeUnit.SECONDS)
                    .onErrorReturnItem(LonLat.empty())
                    .toObservable()*/
                }

        val gpsDisabledObservable = gpsStateObservable
                .filter { isEnabled -> !isEnabled }
                .doOnNext { Timber.tag(tag).d("getLocationObservable() Gps is disabled. Returning empty location") }
                .map { LonLat.empty() }

        return Observable.merge(gpsEnabledObservable, gpsDisabledObservable)
                .doOnNext { location -> Timber.tag(tag).d("getLocationObservable() Current location is [lon: ${location.lon}, lat: ${location.lat}]") }
    }

    private fun showNotification() {
        notification.visibility = View.VISIBLE
    }

    private fun hideNotification() {
        notification.visibility = View.GONE
    }

    override fun resolveDaggerDependency() {
        DaggerTakePhotoActivityComponent.builder()
                .applicationComponent(PhotoExchangeApplication.applicationComponent)
                .build()
                .inject(this)
    }
}






















