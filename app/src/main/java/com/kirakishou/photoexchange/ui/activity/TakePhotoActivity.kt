package com.kirakishou.photoexchange.ui.activity

import android.arch.lifecycle.ViewModelProviders
import android.content.Intent
import android.os.Bundle
import android.support.design.widget.FloatingActionButton
import android.support.v7.widget.CardView
import android.view.View
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
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.rxkotlin.Observables
import io.reactivex.rxkotlin.plusAssign
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.PublishSubject
import timber.log.Timber
import java.io.File
import javax.inject.Inject


class TakePhotoActivity : BaseActivity<TakePhotoActivityViewModel>() {

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

    override fun initViewModel() =
            ViewModelProviders.of(this, viewModelFactory).get(TakePhotoActivityViewModel::class.java)

    override fun getContentView(): Int = R.layout.activity_take_photo

    override fun onActivityCreate(savedInstanceState: Bundle?, intent: Intent) {
        userInfoPreference.load()

        initUserInfo()
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
                .subscribe({
                    getLocation()
                    takePhoto()
                })

        compositeDisposable += Observables.zip(locationSubject, photoAvailabilitySubject)
                .subscribeOn(Schedulers.io())
                .observeOn(Schedulers.io())
                .doOnError { unknownErrorsSubject.onNext(it) }
                .flatMap {
                    val id = saveTakenPhotoToDb(it)
                    return@flatMap Observables.zip(Observable.just(id), Observable.just(it.second))
                }
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(this::switchToViewTakenPhotoActivity)
    }

    private fun saveTakenPhotoToDb(locationAndPhotoPath: Pair<LonLat, String>): Long {
        val userId = userInfoPreference.getUserId()
        val location = locationAndPhotoPath.first
        val photoFilePath = locationAndPhotoPath.second

        return getViewModel().saveTakenPhotoToDb(location, userId, photoFilePath)
    }

    private fun switchToViewTakenPhotoActivity(idAndPhotoFilePath: Pair<Long, String>) {
        hideNotification()

        val intent = Intent(this, ViewTakenPhotoActivity::class.java)
        intent.putExtra("photo_id", idAndPhotoFilePath.first)
        intent.putExtra("photo_file_path", idAndPhotoFilePath.second)
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
        showNotification()

        SmartLocation.with(this)
                .location()
                .oneFix()
                .start {
                    Timber.d("getLocation() Done")
                    locationSubject.onNext(LonLat(it.longitude, it.latitude))
                }
    }

    fun showNotification() {
        notification.visibility = View.VISIBLE
    }

    fun hideNotification() {
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






















