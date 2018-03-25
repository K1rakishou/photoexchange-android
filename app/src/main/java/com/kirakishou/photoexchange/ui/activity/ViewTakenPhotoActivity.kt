package com.kirakishou.photoexchange.ui.activity

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
import kotlinx.coroutines.experimental.async
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
    lateinit var coroutinesPool: CoroutineThreadPoolProvider

    @Inject
    lateinit var imageLoader: ImageLoader

    lateinit var takenPhoto: MyPhoto

    override fun getContentView(): Int = R.layout.activity_view_taken_photo

    override fun onActivityCreate(savedInstanceState: Bundle?, intent: Intent) {
        getTakenPhotoFromIntent(intent)
        initViews()
    }

    private fun initViews() {
//        hideView(true)

        fabCloseActivity.setOnClickListener {
            finish()
        }

        fabSendPhoto.setOnClickListener {
            viewModel.updatePhotoState(takenPhoto.id)
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
    }

    override fun hideControls() {
        async(coroutinesPool.UI()) {
            fabCloseActivity.visibility = View.GONE
            fabSendPhoto.visibility = View.GONE
        }
    }

    override fun showControls() {
        async(coroutinesPool.UI()) {
            fabCloseActivity.visibility = View.VISIBLE
            fabSendPhoto.visibility = View.VISIBLE
        }
    }

    override fun onPhotoUpdated() {
        runActivity(AllPhotosActivity::class.java, true)
//        showView()
    }

//    fun hideView(immediately: Boolean) {
//        async(coroutinesPool.UI()) {
//            if (immediately) {
//                makePhotoPublicView.animate()
//                    .translationYBy(-makePhotoPublicView.height.toFloat())
//                    .setDuration(2000)
//                    .start()
//            } else {
//                makePhotoPublicView.animate()
//                    .translationYBy(-makePhotoPublicView.height.toFloat())
//                    .setDuration(2000)
//                    .setInterpolator(AccelerateInterpolator())
//                    .mySetListener {
//                        onAnimationEnd {
//                            makePhotoPublicView.visibility = View.GONE
//                        }
//                    }
//                    .start()
//            }
//        }
//    }
//
//    fun showView() {
//        async(coroutinesPool.UI()) {
//            makePhotoPublicView.animate()
//                .translationYBy(makePhotoPublicView.height.toFloat())
//                .setDuration(2000)
//                .setInterpolator(DecelerateInterpolator())
//                .mySetListener {
//                    onAnimationStart {
//                        makePhotoPublicView.visibility = View.VISIBLE
//                    }
//                }
//                .start()
//
//        }
//    }

    override fun showToast(message: String, duration: Int) {
        onShowToast(message, duration)
    }

    override fun resolveDaggerDependency() {
        (application as PhotoExchangeApplication).applicationComponent
            .plus(ViewTakenPhotoActivityModule(this))
            .inject(this)
    }
}
