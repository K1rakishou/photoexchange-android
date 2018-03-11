package com.kirakishou.photoexchange.ui.activity

import android.arch.lifecycle.ViewModelProviders
import android.content.Intent
import android.os.Bundle
import android.support.design.widget.FloatingActionButton
import android.view.View
import android.widget.ImageView
import butterknife.BindView
import com.kirakishou.fixmypc.photoexchange.R
import com.kirakishou.photoexchange.PhotoExchangeApplication
import com.kirakishou.photoexchange.base.BaseActivity
import com.kirakishou.photoexchange.di.module.ViewTakenPhotoActivityModule
import com.kirakishou.photoexchange.helper.ImageLoader
import com.kirakishou.photoexchange.helper.concurrency.coroutine.CoroutineThreadPoolProvider
import com.kirakishou.photoexchange.mvp.model.MyPhoto
import com.kirakishou.photoexchange.mvp.view.ViewTakenPhotoActivityView
import com.kirakishou.photoexchange.mvp.viewmodel.ViewTakenPhotoActivityViewModel
import com.kirakishou.photoexchange.mvp.viewmodel.factory.ViewTakenPhotoActivityViewModelFactory
import kotlinx.coroutines.experimental.async
import timber.log.Timber
import javax.inject.Inject

class ViewTakenPhotoActivity : BaseActivity<ViewTakenPhotoActivityViewModel>(), ViewTakenPhotoActivityView {

    @BindView(R.id.iv_photo_view)
    lateinit var ivPhotoView: ImageView

    @BindView(R.id.iv_close_activity)
    lateinit var ivCloseAcitivity: ImageView

    @BindView(R.id.fab_close_activity)
    lateinit var fabCloseActivity: FloatingActionButton

    @BindView(R.id.fab_send_photo)
    lateinit var fabSendPhoto: FloatingActionButton

    @Inject
    lateinit var viewModelFactory: ViewTakenPhotoActivityViewModelFactory

    @Inject
    lateinit var coroutinesPool: CoroutineThreadPoolProvider

    @Inject
    lateinit var imageLoader: ImageLoader

    lateinit var takenPhoto: MyPhoto

    override fun initViewModel(): ViewTakenPhotoActivityViewModel? {
        return ViewModelProviders.of(this, viewModelFactory).get(ViewTakenPhotoActivityViewModel::class.java)
    }

    override fun getContentView(): Int = R.layout.activity_view_taken_photo

    override fun onActivityCreate(savedInstanceState: Bundle?, intent: Intent) {
        getTakenPhotoFromIntent(intent)
        initViews()
    }

    private fun initViews() {
        ivCloseAcitivity.setOnClickListener {
            finish()
        }

        fabCloseActivity.setOnClickListener {
            finish()
        }

        fabSendPhoto.setOnClickListener {
            getViewModel().updatePhotoState(takenPhoto)
        }

        imageLoader.loadImageFromDiskInto(takenPhoto.getFile(), ivPhotoView)
    }

    private fun getTakenPhotoFromIntent(intent: Intent) {
        if (intent.extras == null) {
            throw RuntimeException("Intent does not have photos bundle")
        }

        takenPhoto = MyPhoto.fromBundle(intent.extras!!)
    }

    override fun onActivityDestroy() {
        getViewModel().detach()
    }

    override fun hideControls() {
        async(coroutinesPool.UI()) {
            ivCloseAcitivity.visibility = View.GONE
            fabCloseActivity.visibility = View.GONE
            fabSendPhoto.visibility = View.GONE
        }
    }

    override fun showControls() {
        async(coroutinesPool.UI()) {
            ivCloseAcitivity.visibility = View.VISIBLE
            fabCloseActivity.visibility = View.VISIBLE
            fabSendPhoto.visibility = View.VISIBLE
        }
    }

    override fun onPhotoUpdated(takenPhoto: MyPhoto) {
        Timber.e("onPhotoUpdated")
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
