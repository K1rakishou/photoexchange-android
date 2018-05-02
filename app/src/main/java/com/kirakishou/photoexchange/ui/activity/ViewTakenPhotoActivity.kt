package com.kirakishou.photoexchange.ui.activity

import android.content.Intent
import android.os.Bundle
import com.kirakishou.fixmypc.photoexchange.R
import com.kirakishou.photoexchange.PhotoExchangeApplication
import com.kirakishou.photoexchange.di.module.ViewTakenPhotoActivityModule
import com.kirakishou.photoexchange.mvp.model.MyPhoto
import com.kirakishou.photoexchange.mvp.view.ViewTakenPhotoActivityView
import com.kirakishou.photoexchange.mvp.viewmodel.ViewTakenPhotoActivityViewModel
import com.kirakishou.photoexchange.ui.fragment.AddToGalleryDialogFragment
import com.kirakishou.photoexchange.ui.fragment.ViewTakenPhotoFragment
import io.reactivex.Completable
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.rxkotlin.plusAssign
import io.reactivex.schedulers.Schedulers
import javax.inject.Inject

class ViewTakenPhotoActivity : BaseActivity(), ViewTakenPhotoActivityView {

    @Inject
    lateinit var viewModel: ViewTakenPhotoActivityViewModel

    lateinit var takenPhoto: MyPhoto

    val activityComponent by lazy {
        (application as PhotoExchangeApplication).applicationComponent
            .plus(ViewTakenPhotoActivityModule(this))
    }

    override fun getContentView(): Int = R.layout.activity_view_taken_photo

    override fun onActivityCreate(savedInstanceState: Bundle?, intent: Intent) {
        takenPhoto = MyPhoto.fromBundle(intent.extras)

        showViewTakenPhotoFragment(intent)
    }

    override fun onActivityStart() {
        viewModel.setView(this)
        initRx()
    }

    override fun onActivityStop() {
        viewModel.clearView()
    }

    private fun initRx() {
        compositeDisposable += viewModel.addToGalleryFragmentResult
            .subscribeOn(AndroidSchedulers.mainThread())
            .flatMap { fragmentResult ->
                onBackPressedInternal()
                    .andThen(Observable.just(fragmentResult))
            }
            .concatMap { fragmentResult -> onAddToGalleryFragmentResult(fragmentResult) }
            .doOnNext { onPhotoUpdated() }
            .subscribe()
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

    override fun onPhotoUpdated() {
        runActivity(AllPhotosActivity::class.java, true)
    }

    override fun showToast(message: String, duration: Int) {
        onShowToast(message, duration)
    }

    private fun onAddToGalleryFragmentResult(fragmentResult: AddToGalleryDialogFragment.FragmentResult): Observable<Boolean> {
        return Observable.fromCallable {
            if (fragmentResult.rememberChoice) {
                viewModel.saveMakePublicFlag(fragmentResult.makePublic)
            }

            return@fromCallable fragmentResult.makePublic
        }
            .subscribeOn(Schedulers.io())
            .observeOn(Schedulers.io())
            .flatMap { isPublic ->
                if (isPublic) {
                    return@flatMap viewModel.updateSetIsPhotoPublic(takenPhoto.id)
                }

                return@flatMap Observable.just(isPublic)
            }
    }

    override fun onBackPressed() {
        compositeDisposable += onBackPressedInternal()
            .subscribeOn(AndroidSchedulers.mainThread())
            .doOnComplete {
                supportFragmentManager.popBackStackImmediate()

                if (supportFragmentManager.fragments.size <= 0) {
                    super.onBackPressed()
                }
            }
            .subscribe()
    }

    private fun onBackPressedInternal(): Completable {
        val fragment = (supportFragmentManager.fragments.last() as BackPressAwareFragment)
        return fragment.onBackPressed()
    }

    interface BackPressAwareFragment {
        fun onBackPressed(): Completable
    }

    override fun resolveDaggerDependency() {
        activityComponent.inject(this)
    }
}
