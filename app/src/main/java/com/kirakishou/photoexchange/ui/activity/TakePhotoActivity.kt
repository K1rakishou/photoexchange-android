package com.kirakishou.photoexchange.ui.activity

import android.Manifest
import android.arch.lifecycle.ViewModelProviders
import android.content.Intent
import android.location.Location
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
import com.kirakishou.photoexchange.helper.database.entity.TakenPhotoEntity
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
import io.nlopez.smartlocation.SmartLocation
import io.nlopez.smartlocation.location.config.LocationParams
import io.nlopez.smartlocation.rx.ObservableFactory
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
import java.util.concurrent.TimeUnit
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

    private val ON_START = 0
    private val ON_STOP = 1

    private val userInfoPreference by lazy { appSharedPreference.prepare<UserInfoPreference>() }
    private val initCameraSubject = BehaviorSubject.create<Boolean>()
    private val locationPermissionSubject = BehaviorSubject.create<Boolean>()
    private val photoAvailabilitySubject = PublishSubject.create<String>()
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

    private fun getPermissions() {
        Dexter.withActivity(this)
                .withPermissions(
                        Manifest.permission.CAMERA,
                        Manifest.permission.ACCESS_FINE_LOCATION)
                .withListener(object : MultiplePermissionsListener {
                    override fun onPermissionsChecked(report: MultiplePermissionsReport) {
                        val cameraPermissionDenied = report.deniedPermissionResponses.any { it.permissionName == Manifest.permission.CAMERA }
                        if (cameraPermissionDenied) {
                            Timber.d("Could not obtain camera permission")
                            finish()
                            return
                        }

                        initCameraSubject.onNext(true)
                        locationPermissionSubject.onNext(true)
                    }

                    override fun onPermissionRationaleShouldBeShown(permissions: MutableList<PermissionRequest>, token: PermissionToken) {
                        showCameraRationale(token)
                    }

                }).check()
    }

    private fun showCameraRationale(token: PermissionToken) {
        MaterialDialog.Builder(this)
                .title("Why do we need camera?")
                .content("How would you take pictures without camera?")
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
        super.onStop()

        lifecycleSubject.onNext(ON_STOP)
    }

    private fun initCamera(): Observable<Fotoapparat> {
        val foto = Fotoapparat
                .with(applicationContext)
                .into(cameraView)
                .previewScaleType(ScaleType.CENTER_CROP)
                .photoSize(biggestSize())
                .lensPosition(back())
                .build()

        return Observable.just(foto)
    }

    private fun initRx() {
        compositeDisposable += RxView.clicks(ivShowAllPhotos)
                .subscribeOn(AndroidSchedulers.mainThread())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({ switchToAllPhotosViewActivity() })

        val fotoapparatObservable = initCameraSubject
                .flatMap {
                    initCamera()
                }
                .share()

        compositeDisposable += Observables.combineLatest(fotoapparatObservable, lifecycleSubject)
                .doOnNext { (fotoapparat, lifecycle) ->
                    when (lifecycle) {
                        ON_START -> {
                            Timber.d("ON_START")
                            fotoapparat.start()
                        }
                        ON_STOP -> {
                            Timber.d("ON_STOP")
                            fotoapparat.stop()
                        }
                    }
                }
                .subscribe()

        compositeDisposable += RxView.clicks(takePhotoButton)
                .subscribeOn(AndroidSchedulers.mainThread())
                .observeOn(AndroidSchedulers.mainThread())
                .map { lifecycleSubject.value }
                .filter { lifecycle -> lifecycle == ON_START }
                .combineLatest(fotoapparatObservable)
                .map { it.second }
                .subscribe({ fotoapparat ->
                    showNotification()
                    takePhoto(fotoapparat)
                })

        val locationObservableGranted = locationPermissionSubject
                .filter { granted -> granted }
                .combineLatest(getLocationObservable())
                .map { it.second }

        val locationObservableDenied = locationPermissionSubject
                .filter { granted -> !granted }
                .map { LonLat.empty() }

        val locationObservable = Observable.merge(locationObservableDenied, locationObservableGranted)

        compositeDisposable += photoAvailabilitySubject
                .subscribeOn(Schedulers.io())
                .observeOn(Schedulers.io())
                .combineLatest(locationObservable)
                .doOnError { unknownErrorsSubject.onNext(it) }
                .map {
                    val photoFilePath = it.first
                    val location = it.second
                    val userId = userInfoPreference.getUserId()

                    return@map TakenPhoto(-1L, location, photoFilePath, userId, "")
                }
                .subscribe({ getViewModel().inputs.saveTakenPhoto(it) }, this::onUnknownError)

        compositeDisposable += getViewModel().outputs.onTakenPhotoSavedObservable()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({ savedTakenPhoto ->
                    hideNotification()
                    switchToViewTakenPhotoActivity(savedTakenPhoto.id, savedTakenPhoto.location, savedTakenPhoto.photoFilePath, savedTakenPhoto.userId)
                })

        compositeDisposable += getViewModel().errors.onUnknownErrorObservable()
                .subscribeOn(AndroidSchedulers.mainThread())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(this::onUnknownError)
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
            Timber.d("App first run. Generating userId")

            val newUserId = Utils.generateUserId()
            userInfoPreference.setUserId(newUserId)
        } else {
            Timber.d("UserId already exists")
        }
    }

    private fun takePhoto(fotoapparat: Fotoapparat) {
        Timber.d("takePhoto() Taking a photo...")
        val tempFile = File.createTempFile("photo", ".tmp")

        fotoapparat.takePicture()
                .saveToFile(tempFile)
                .whenAvailable {
                    Timber.d("takePhoto() Done")
                    photoAvailabilitySubject.onNext(tempFile.absolutePath)
                }
    }

    private fun getLocationObservable(): Observable<LonLat> {
        Timber.d("getLocation() Getting current location...")

        return ObservableFactory.from(SmartLocation.with(applicationContext)
                .location()
                .config(LocationParams.NAVIGATION)
                .oneFix())
                .timeout(5, TimeUnit.SECONDS)
                .map { location -> getTruncatedLonLat(location) }
                .onErrorReturn {
                    Timber.d("Could not get current location. Returning empty location")
                    LonLat.empty()
                }
    }

    private fun getTruncatedLonLat(location: Location): LonLat {
        val lon = Math.floor(location.longitude * 100) / 100
        val lat = Math.floor(location.latitude * 100) / 100

        return LonLat(lon, lat)
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






















