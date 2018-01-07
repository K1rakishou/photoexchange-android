package com.kirakishou.photoexchange.ui.activity

import android.arch.lifecycle.ViewModelProviders
import android.content.Intent
import android.os.Bundle
import android.support.design.widget.FloatingActionButton
import android.support.v7.widget.CardView
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import butterknife.BindView
import com.jakewharton.rxbinding2.view.RxView
import com.kirakishou.fixmypc.photoexchange.R
import com.kirakishou.photoexchange.PhotoExchangeApplication
import com.kirakishou.photoexchange.base.BaseActivity
import com.kirakishou.photoexchange.di.component.DaggerViewTakenPhotoActivityComponent
import com.kirakishou.photoexchange.di.module.ViewTakenPhotoActivityModule
import com.kirakishou.photoexchange.helper.ImageLoader
import com.kirakishou.photoexchange.helper.location.MyLocationManager
import com.kirakishou.photoexchange.helper.location.RxLocationManager
import com.kirakishou.photoexchange.mwvm.model.other.LonLat
import com.kirakishou.photoexchange.mwvm.model.state.PhotoState
import com.kirakishou.photoexchange.mwvm.model.other.TakenPhoto
import com.kirakishou.photoexchange.mwvm.viewmodel.ViewTakenPhotoActivityViewModel
import com.kirakishou.photoexchange.mwvm.viewmodel.factory.ViewTakenPhotoActivityViewModelFactory
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.rxkotlin.Observables
import io.reactivex.rxkotlin.plusAssign
import io.reactivex.schedulers.Schedulers
import timber.log.Timber
import java.io.File
import java.util.concurrent.TimeUnit
import javax.inject.Inject


class ViewTakenPhotoActivity : BaseActivity<ViewTakenPhotoActivityViewModel>() {

    @BindView(R.id.iv_close_activity)
    lateinit var closeActivityButtonIv: ImageView

    @BindView(R.id.iv_photo_view)
    lateinit var photoView: ImageView

    @BindView(R.id.fab_close_activity)
    lateinit var closeActivityButtonFab: FloatingActionButton

    @BindView(R.id.fab_send_photo)
    lateinit var sendPhotoButton: FloatingActionButton

    @Inject
    lateinit var viewModelFactory: ViewTakenPhotoActivityViewModelFactory

    @Inject
    lateinit var imageLoader: ImageLoader

    private val tag = "[${this::class.java.simpleName}]: "
    private var takenPhoto = TakenPhoto.empty()

    override fun initViewModel(): ViewTakenPhotoActivityViewModel {
        return ViewModelProviders.of(this, viewModelFactory).get(ViewTakenPhotoActivityViewModel::class.java)
    }

    override fun getContentView() = R.layout.activity_view_taken_photo

    override fun onInitRx() {
        initRx()
    }

    override fun onActivityCreate(savedInstanceState: Bundle?, intent: Intent) {
        getTakenPhoto(intent)
        setPhotoPreview()
    }

    override fun onActivityDestroy() {
    }

    private fun initRx() {
        compositeDisposable += RxView.clicks(closeActivityButtonIv)
                .subscribeOn(AndroidSchedulers.mainThread())
                .observeOn(AndroidSchedulers.mainThread())
                .doOnNext { getViewModel().inputs.deleteTakenPhoto(takenPhoto.id) }
                .doOnError(this::onUnknownError)
                .subscribe({ finish() })

        compositeDisposable += RxView.clicks(closeActivityButtonFab)
                .subscribeOn(AndroidSchedulers.mainThread())
                .observeOn(AndroidSchedulers.mainThread())
                .doOnNext { getViewModel().inputs.deleteTakenPhoto(takenPhoto.id) }
                .doOnError(this::onUnknownError)
                .subscribe({ finish() })

        compositeDisposable += RxView.clicks(sendPhotoButton)
                .subscribeOn(AndroidSchedulers.mainThread())
                .observeOn(Schedulers.io())
                .doOnNext { getViewModel().inputs.updateTakenPhotoAsQueuedUp(takenPhoto.id) }
                .delay(1, TimeUnit.SECONDS)
                .observeOn(AndroidSchedulers.mainThread())
                .doOnError(this::onUnknownError)
                .subscribe({ switchToAllPhotosViewActivity() })
    }

    private fun switchToAllPhotosViewActivity() {
        val intent = Intent(this, AllPhotosViewActivity::class.java)
        startActivity(intent)
        finish()
    }

    private fun setPhotoPreview() {
        imageLoader.loadImageFromDiskInto(File(takenPhoto.photoFilePath), photoView)
    }

    private fun getTakenPhoto(intent: Intent) {
        val id = intent.getLongExtra("id", -1L)
        val lon = intent.getDoubleExtra("lon", 0.0)
        val lat = intent.getDoubleExtra("lat", 0.0)
        val photoFilePath = intent.getStringExtra("photo_file_path")
        val userId = intent.getStringExtra("user_id")

        checkNotNull(photoFilePath)
        checkNotNull(userId)
        check(lon != 0.0)
        check(lat != 0.0)
        check(photoFilePath.isNotEmpty())
        check(userId.isNotEmpty())

        takenPhoto = TakenPhoto.create(id, LonLat(lon, lat), photoFilePath, userId, "", PhotoState.TAKEN)
    }

    override fun resolveDaggerDependency() {
        DaggerViewTakenPhotoActivityComponent.builder()
                .applicationComponent(PhotoExchangeApplication.applicationComponent)
                .viewTakenPhotoActivityModule(ViewTakenPhotoActivityModule(this))
                .build()
                .inject(this)
    }
}
