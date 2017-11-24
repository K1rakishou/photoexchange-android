package com.kirakishou.photoexchange.ui.activity

import android.arch.lifecycle.ViewModelProviders
import android.content.Intent
import android.os.Bundle
import android.support.design.widget.FloatingActionButton
import android.widget.ImageView
import butterknife.BindView
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.jakewharton.rxbinding2.view.RxView
import com.kirakishou.fixmypc.photoexchange.R
import com.kirakishou.photoexchange.PhotoExchangeApplication
import com.kirakishou.photoexchange.base.BaseActivity
import com.kirakishou.photoexchange.di.component.DaggerViewTakenPhotoActivityComponent
import com.kirakishou.photoexchange.di.module.ViewTakenPhotoActivityModule
import com.kirakishou.photoexchange.helper.service.UploadPhotoService
import com.kirakishou.photoexchange.mwvm.model.other.LonLat
import com.kirakishou.photoexchange.mwvm.model.other.ServiceCommand
import com.kirakishou.photoexchange.mwvm.model.other.TakenPhoto
import com.kirakishou.photoexchange.mwvm.viewmodel.ViewTakenPhotoActivityViewModel
import com.kirakishou.photoexchange.mwvm.viewmodel.factory.ViewTakenPhotoActivityViewModelFactory
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.rxkotlin.plusAssign
import java.io.File
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
                .doOnNext {
                    getViewModel().inputs.deleteTakenPhoto(takenPhoto.id)
                }
                .subscribe({ finish() }, this::onUnknownError)

        compositeDisposable += RxView.clicks(closeActivityButtonFab)
                .subscribeOn(AndroidSchedulers.mainThread())
                .observeOn(AndroidSchedulers.mainThread())
                .doOnNext {
                    getViewModel().inputs.deleteTakenPhoto(takenPhoto.id)
                }
                .subscribe({ finish() }, this::onUnknownError)

        compositeDisposable += RxView.clicks(sendPhotoButton)
                .subscribeOn(AndroidSchedulers.mainThread())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({
                    startServiceToUploadPhoto()
                    switchToAllPhotosViewActivity()
                })
    }

    private fun deletePhoto() {
        val photoFile = File(takenPhoto.photoFilePath)
        if (photoFile.exists()) {
            photoFile.delete()
        }
    }

    private fun switchToAllPhotosViewActivity() {
        val intent = Intent(this, AllPhotosViewActivity::class.java)
        intent.putExtra("is_photo_uploading", true)
        startActivity(intent)
        finish()
    }

    private fun startServiceToUploadPhoto() {
        val intent = Intent(this, UploadPhotoService::class.java)
        intent.putExtra("id", takenPhoto.id)
        intent.putExtra("command", ServiceCommand.SEND_PHOTO.value)
        intent.putExtra("lon", takenPhoto.location.lon)
        intent.putExtra("lat", takenPhoto.location.lat)
        intent.putExtra("user_id", takenPhoto.userId)
        intent.putExtra("photo_file_path", takenPhoto.photoFilePath)

        startService(intent)
    }

    private fun setPhotoPreview() {
        Glide.with(this)
                .load(File(takenPhoto.photoFilePath))
                .apply(RequestOptions().centerCrop())
                .into(photoView)
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

        takenPhoto = TakenPhoto(id, LonLat(lon, lat), photoFilePath, userId)
    }

    override fun resolveDaggerDependency() {
        DaggerViewTakenPhotoActivityComponent.builder()
                .applicationComponent(PhotoExchangeApplication.applicationComponent)
                .viewTakenPhotoActivityModule(ViewTakenPhotoActivityModule(this))
                .build()
                .inject(this)
    }
}
