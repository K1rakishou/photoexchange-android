package com.kirakishou.photoexchange.ui.activity

import android.content.Intent
import android.os.Bundle
import com.kirakishou.fixmypc.photoexchange.R
import com.kirakishou.photoexchange.PhotoExchangeApplication
import com.kirakishou.photoexchange.di.module.ViewTakenPhotoActivityModule
import com.kirakishou.photoexchange.mvp.view.ViewTakenPhotoActivityView
import com.kirakishou.photoexchange.mvp.viewmodel.ViewTakenPhotoActivityViewModel
import com.kirakishou.photoexchange.ui.fragment.AddToGalleryDialogFragment
import com.kirakishou.photoexchange.ui.fragment.ViewTakenPhotoFragment
import io.reactivex.Completable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.rxkotlin.plusAssign
import javax.inject.Inject

class ViewTakenPhotoActivity : BaseActivity(), ViewTakenPhotoActivityView {

    @Inject
    lateinit var viewModel: ViewTakenPhotoActivityViewModel

    val activityComponent by lazy {
        (application as PhotoExchangeApplication).applicationComponent
            .plus(ViewTakenPhotoActivityModule(this))
    }

    override fun getContentView(): Int = R.layout.activity_view_taken_photo

    override fun onActivityCreate(savedInstanceState: Bundle?, intent: Intent) {
        showViewTakenPhotoFragment(intent)
    }

    override fun onInitRx() {

    }

    private fun showViewTakenPhotoFragment(intent: Intent) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, ViewTakenPhotoFragment.newInstance(intent), ViewTakenPhotoFragment.TAG)
            .addToBackStack(null)
            .commit()
    }

    fun showDialogFragment() {
        supportFragmentManager.beginTransaction()
            .add(R.id.fragment_container, AddToGalleryDialogFragment(), AddToGalleryDialogFragment.TAG)
            .addToBackStack(null)
            .commit()
    }

    override fun onActivityStart() {
        viewModel.setView(this)
    }

    override fun onActivityStop() {
        viewModel.clearView()
    }

    override fun onPhotoUpdated() {
        runActivity(AllPhotosActivity::class.java, true)
    }

    override fun showToast(message: String, duration: Int) {
        onShowToast(message, duration)
    }

    override fun onBackPressed() {
        val fragment = (supportFragmentManager.fragments.last() as BackPressAwareFragment)

        compositeDisposable += fragment.onBackPressed()
            .observeOn(AndroidSchedulers.mainThread())
            .doOnComplete {
                supportFragmentManager.popBackStackImmediate()

                if (supportFragmentManager.fragments.size <= 0) {
                    super.onBackPressed()
                }
            }
            .subscribe()
    }

    interface BackPressAwareFragment {
        fun onBackPressed(): Completable
    }

    override fun resolveDaggerDependency() {
        activityComponent.inject(this)
    }
}
