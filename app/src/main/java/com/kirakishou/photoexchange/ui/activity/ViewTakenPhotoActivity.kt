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

    private var photoFilePath: String = ""
    private var photoId: Long = -1L

    override fun getContentView() = R.layout.activity_view_taken_photo

    override fun onActivityCreate(savedInstanceState: Bundle?, intent: Intent) {
        setImageViewPhoto(intent)

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
                .subscribe({ startAllPhotosViewActivity() })
    }

    private fun startAllPhotosViewActivity() {
        val intent = Intent(this, AllPhotosViewActivity::class.java)
        startActivity(intent)
        finish()
    }

    private fun setImageViewPhoto(intent: Intent) {
        photoFilePath = intent.getStringExtra("photo_file_path")
        photoId = intent.getLongExtra("photo_id", -1L)

        check(photoFilePath.isNotEmpty())
        check(photoId != -1L)

        Glide.with(this)
                .load(File(photoFilePath))
                .apply(RequestOptions().centerCrop())
                .into(photoView)
    }

    private fun closeActivity() {
        //TODO: delete photo from the DB and from the disk

        finish()
    }

    override fun resolveDaggerDependency() {
    }

}
