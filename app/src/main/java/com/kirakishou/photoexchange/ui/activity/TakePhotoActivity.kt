package com.kirakishou.photoexchange.ui.activity

import android.arch.lifecycle.ViewModelProviders
import android.content.Intent
import android.os.Bundle
import android.support.design.widget.FloatingActionButton
import android.support.v7.widget.CardView
import android.view.View
import android.widget.ImageView
import butterknife.BindView
import com.jakewharton.rxbinding2.view.RxView
import com.kirakishou.fixmypc.photoexchange.R
import com.kirakishou.photoexchange.PhotoExchangeApplication
import com.kirakishou.photoexchange.base.BaseActivity
import com.kirakishou.photoexchange.di.component.DaggerTakePhotoActivityComponent
import com.kirakishou.photoexchange.helper.preference.AppSharedPreference
import com.kirakishou.photoexchange.helper.preference.UserInfoPreference
import com.kirakishou.photoexchange.helper.util.Utils
import com.kirakishou.photoexchange.mvvm.model.ServerErrorCode
import com.kirakishou.photoexchange.mvvm.model.LonLat
import com.kirakishou.photoexchange.mvvm.viewmodel.TakePhotoActivityViewModel
import com.kirakishou.photoexchange.mvvm.viewmodel.factory.TakePhotoActivityViewModelFactory
import io.fotoapparat.Fotoapparat
import io.fotoapparat.parameter.ScaleType
import io.fotoapparat.parameter.selector.LensPositionSelectors.back
import io.fotoapparat.parameter.selector.SizeSelectors.biggestSize
import io.fotoapparat.view.CameraView
import io.nlopez.smartlocation.SmartLocation
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.rxkotlin.Observables
import io.reactivex.rxkotlin.plusAssign
import io.reactivex.schedulers.Schedulers
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

    lateinit var fotoapparat: Fotoapparat

    private val userInfoPreference by lazy { appSharedPreference.prepare<UserInfoPreference>() }
    private val photoAvailabilitySubject = PublishSubject.create<String>()
    private val locationSubject = PublishSubject.create<LonLat>()

    override fun initViewModel(): TakePhotoActivityViewModel {
        return ViewModelProviders.of(this, viewModelFactory).get(TakePhotoActivityViewModel::class.java)
    }

    override fun getContentView(): Int = R.layout.activity_take_photo

    override fun onActivityCreate(savedInstanceState: Bundle?, intent: Intent) {
        userInfoPreference.load()

        initUserInfo()
        initRx()
        initCamera()

        getViewModel().inputs.cleanDb()
    }

    override fun onActivityDestroy() {
        SmartLocation.with(this)
                .location()
                .stop()

        PhotoExchangeApplication.refWatcher.watch(this, this::class.simpleName)
    }

    override fun onStart() {
        super.onStart()
        fotoapparat.start()
    }

    override fun onStop() {
        super.onStop()
        fotoapparat.stop()
    }

    override fun onPause() {
        super.onPause()
        userInfoPreference.save()
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
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({
                    showNotification()
                    getLocation()
                    takePhoto()
                })

        compositeDisposable += RxView.clicks(ivShowAllPhotos)
                .subscribeOn(AndroidSchedulers.mainThread())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({ switchToAllPhotosViewActivity() })

        compositeDisposable += Observables.zip(locationSubject, photoAvailabilitySubject)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .doOnError { unknownErrorsSubject.onNext(it) }
                .subscribe({
                    val location = it.first
                    val photoFilePath = it.second
                    val userId = userInfoPreference.getUserId()

                    hideNotification()
                    switchToViewTakenPhotoActivity(location, photoFilePath, userId)
                })
    }

    private fun switchToAllPhotosViewActivity() {
        val intent = Intent(this, AllPhotosViewActivity::class.java)
        startActivity(intent)
    }

    private fun switchToViewTakenPhotoActivity(location: LonLat, photoFilePath: String, userId: String) {
        val intent = Intent(this, ViewTakenPhotoActivity::class.java)
        intent.putExtra("lon", location.lon)
        intent.putExtra("lat", location.lat)
        intent.putExtra("photo_file_path", photoFilePath)
        intent.putExtra("user_id", userId)
        startActivity(intent)
    }

    private fun initUserInfo() {
        if (!userInfoPreference.exists()) {
            Timber.d("App first run. Generating userId")

            val newUserId = Utils.generateUserId()
            userInfoPreference.setUserId(newUserId)
        } else {
            Timber.d("UserId already exists")
        }
    }

    private fun takePhoto() {
        Timber.d("takePhoto() Taking a photo...")
        val tempFile = File.createTempFile("photo", ".tmp")

        fotoapparat.takePicture()
                .saveToFile(tempFile)
                .whenAvailable {
                    Timber.d("takePhoto() Done")
                    photoAvailabilitySubject.onNext(tempFile.absolutePath)
                }
    }

    private fun getLocation() {
        Timber.d("getLocation() Getting current location...")

        SmartLocation.with(this)
                .location()
                .oneFix()
                .start {
                    Timber.d("getLocation() Done")
                    locationSubject.onNext(LonLat(it.longitude, it.latitude))
                }
    }

    private fun showNotification() {
        notification.visibility = View.VISIBLE
    }

    private fun hideNotification() {
        notification.visibility = View.GONE
    }

    override fun onBadResponse(serverErrorCode: ServerErrorCode) {
        super.onBadResponse(serverErrorCode)

        /*val message = ErrorMessage.getRemoteErrorMessage(activity, serverErrorCode)
        showToast(message, Toast.LENGTH_LONG)*/
    }

    override fun onUnknownError(error: Throwable) {
        super.onUnknownError(error)
    }

    override fun resolveDaggerDependency() {
        DaggerTakePhotoActivityComponent.builder()
                .applicationComponent(PhotoExchangeApplication.applicationComponent)
                .build()
                .inject(this)
    }
}






















