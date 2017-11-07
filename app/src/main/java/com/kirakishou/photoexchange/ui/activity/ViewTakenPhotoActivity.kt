package com.kirakishou.photoexchange.ui.activity

import android.content.Intent
import android.os.Bundle
import android.support.design.widget.FloatingActionButton
import android.widget.ImageView
import butterknife.BindView
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.jakewharton.rxbinding2.view.RxView
import com.kirakishou.fixmypc.photoexchange.R
import com.kirakishou.photoexchange.base.BaseActivityWithoutViewModel
import com.kirakishou.photoexchange.mvvm.model.LonLat
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.rxkotlin.plusAssign
import java.io.File


class ViewTakenPhotoActivity : BaseActivityWithoutViewModel() {

    @BindView(R.id.iv_close_activity)
    lateinit var closeActivityButtonIv: ImageView

    @BindView(R.id.iv_photo_view)
    lateinit var photoView: ImageView

    @BindView(R.id.fab_close_activity)
    lateinit var closeActivityButtonFab: FloatingActionButton

    @BindView(R.id.fab_send_photo)
    lateinit var sendPhotoButton: FloatingActionButton

    private var location: LonLat = LonLat.empty()
    private var userId: String = ""
    private var photoFilePath: String = ""

    override fun getContentView() = R.layout.activity_view_taken_photo

    override fun onActivityCreate(savedInstanceState: Bundle?, intent: Intent) {
        getPhotoInfoFromIntent(intent)
        setImageViewPhoto()

        initRx()
    }

    override fun onActivityDestroy() {

    }

    private fun initRx() {
        compositeDisposable += RxView.clicks(closeActivityButtonIv)
                .subscribeOn(AndroidSchedulers.mainThread())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({ closeActivity() })

        compositeDisposable += RxView.clicks(closeActivityButtonFab)
                .subscribeOn(AndroidSchedulers.mainThread())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({ closeActivity() })

        compositeDisposable += RxView.clicks(sendPhotoButton)
                .subscribeOn(AndroidSchedulers.mainThread())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({
                    startAllPhotosViewActivity(location, photoFilePath, userId)
                })
    }

    private fun startAllPhotosViewActivity(location: LonLat, photoFilePath: String, userId: String) {
        val intent = Intent(this, AllPhotosViewActivity::class.java)
        intent.putExtra("lon", location.lon)
        intent.putExtra("lat", location.lat)
        intent.putExtra("user_id", userId)
        intent.putExtra("photo_file_path", photoFilePath)
        startActivity(intent)
        finish()
    }

    private fun setImageViewPhoto() {
        Glide.with(this)
                .load(File(photoFilePath))
                .apply(RequestOptions().centerCrop())
                .into(photoView)
    }

    private fun getPhotoInfoFromIntent(intent: Intent) {
        val lon = intent.getDoubleExtra("lon", 0.0)
        val lat = intent.getDoubleExtra("lat", 0.0)
        userId = intent.getStringExtra("user_id")
        photoFilePath = intent.getStringExtra("photo_file_path")

        check(lon != 0.0)
        check(lat != 0.0)
        check(userId.isNotEmpty())
        check(photoFilePath.isNotEmpty())

        location = LonLat(lon, lat)
    }

    private fun closeActivity() {
        finish()
    }

    override fun resolveDaggerDependency() {
    }

}
