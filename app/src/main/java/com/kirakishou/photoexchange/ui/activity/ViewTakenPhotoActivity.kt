package com.kirakishou.photoexchange.ui.activity

import android.content.Intent
import android.os.Bundle
import android.support.design.widget.FloatingActionButton
import android.view.View
import android.widget.ImageView
import butterknife.BindView
import com.jakewharton.rxbinding2.view.RxView
import com.kirakishou.fixmypc.photoexchange.R
import com.kirakishou.photoexchange.PhotoExchangeApplication
import com.kirakishou.photoexchange.di.module.ViewTakenPhotoActivityModule
import com.kirakishou.photoexchange.helper.ImageLoader
import com.kirakishou.photoexchange.helper.extension.debounceClicks
import com.kirakishou.photoexchange.mvp.model.MyPhoto
import com.kirakishou.photoexchange.mvp.view.ViewTakenPhotoActivityView
import com.kirakishou.photoexchange.mvp.viewmodel.ViewTakenPhotoActivityViewModel
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.rxkotlin.plusAssign
import javax.inject.Inject

class ViewTakenPhotoActivity : BaseActivity(), ViewTakenPhotoActivityView {

    @BindView(R.id.iv_photo_view)
    lateinit var ivPhotoView: ImageView

    @BindView(R.id.fab_close_activity)
    lateinit var fabCloseActivity: FloatingActionButton

    @BindView(R.id.fab_send_photo)
    lateinit var fabSendPhoto: FloatingActionButton

    @Inject
    lateinit var viewModel: ViewTakenPhotoActivityViewModel

    @Inject
    lateinit var imageLoader: ImageLoader

    lateinit var takenPhoto: MyPhoto

    override fun getContentView(): Int = R.layout.activity_view_taken_photo

    override fun onActivityCreate(savedInstanceState: Bundle?, intent: Intent) {
        getTakenPhotoFromIntent(intent)
        initViews()
    }

    private fun initViews() {
        compositeDisposable += RxView.clicks(fabCloseActivity)
            .subscribeOn(AndroidSchedulers.mainThread())
            .debounceClicks()
            .doOnNext { finish() }
            .subscribe()

        compositeDisposable += RxView.clicks(fabSendPhoto)
            .subscribeOn(AndroidSchedulers.mainThread())
            .debounceClicks()
            .doOnNext { viewModel.updatePhotoState(takenPhoto.id) }
            .subscribe()

        imageLoader.loadImageFromDiskInto(takenPhoto.getFile(), ivPhotoView)
    }

    private fun getTakenPhotoFromIntent(intent: Intent) {
        require(intent.extras != null) { "Intent does not contain photos bundle" }

        takenPhoto = MyPhoto.fromBundle(intent.extras!!)
    }

    override fun onActivityDestroy() {
    }

    override fun hideControls() {
        runOnUiThread {
            fabCloseActivity.visibility = View.GONE
            fabSendPhoto.visibility = View.GONE
        }
    }

    override fun showControls() {
        runOnUiThread {
            fabCloseActivity.visibility = View.VISIBLE
            fabSendPhoto.visibility = View.VISIBLE
        }
    }

    override fun onPhotoUpdated() {
        runActivity(AllPhotosActivity::class.java, true)
    }

    override fun showToast(message: String, duration: Int) {
        onShowToast(message, duration)
    }

    override fun resolveDaggerDependency() {
        (application as PhotoExchangeApplication).applicationComponent
            .plus(ViewTakenPhotoActivityModule(this))
            .inject(this)
    }
}
