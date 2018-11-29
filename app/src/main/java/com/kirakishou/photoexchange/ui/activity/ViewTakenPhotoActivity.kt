package com.kirakishou.photoexchange.ui.activity

import android.content.Intent
import android.os.Bundle
import com.kirakishou.fixmypc.photoexchange.R
import com.kirakishou.photoexchange.PhotoExchangeApplication
import com.kirakishou.photoexchange.di.module.ViewTakenPhotoActivityModule
import com.kirakishou.photoexchange.mvp.model.photo.TakenPhoto
import com.kirakishou.photoexchange.mvp.viewmodel.ViewTakenPhotoActivityViewModel
import com.kirakishou.photoexchange.ui.fragment.AddToGalleryDialogFragment
import com.kirakishou.photoexchange.ui.fragment.ViewTakenPhotoFragment
import io.reactivex.Completable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.rxkotlin.plusAssign
import kotlinx.coroutines.launch
import kotlinx.coroutines.rx2.await
import kotlinx.coroutines.rx2.consumeEach
import timber.log.Timber
import javax.inject.Inject

class ViewTakenPhotoActivity : BaseActivity() {

  @Inject
  lateinit var viewModel: ViewTakenPhotoActivityViewModel

  private val TAG = "ViewTakenPhotoActivity"

  lateinit var takenPhoto: TakenPhoto

  val activityComponent by lazy {
    (application as PhotoExchangeApplication).applicationComponent
      .plus(ViewTakenPhotoActivityModule(this))
  }

  override fun getContentView(): Int = R.layout.activity_view_taken_photo

  override fun onActivityCreate(savedInstanceState: Bundle?, intent: Intent) {
    takenPhoto = TakenPhoto.fromBundle(intent.extras)

    showViewTakenPhotoFragment(intent)
  }

  override fun onActivityStart() {
    launch { initRx() }
  }

  override fun onActivityStop() {
  }

  private suspend fun initRx() {
    launch {
      //TODO: remove
      viewModel.addToGalleryFragmentResult.consumeEach { fragmentResult ->
        onBackPressedInternal().await()
        onAddToGalleryFragmentResult(fragmentResult)
        onPhotoUpdated()
      }
    }
  }

  private fun showViewTakenPhotoFragment(intent: Intent) {
    supportFragmentManager.beginTransaction()
      .replace(R.id.fragment_container, ViewTakenPhotoFragment.newInstance(intent), ViewTakenPhotoFragment.BACKSTACK_TAG)
      .addToBackStack(null)
      .commit()
  }

  fun showDialogFragment() {
    supportFragmentManager.beginTransaction()
      .add(R.id.fragment_container, AddToGalleryDialogFragment(), AddToGalleryDialogFragment.BACKSTACK_TAG)
      .addToBackStack(null)
      .commit()
  }

  private fun onPhotoUpdated() {
    runActivity(PhotosActivity::class.java, true)
  }

  private suspend fun onAddToGalleryFragmentResult(fragmentResult: AddToGalleryDialogFragment.FragmentResult) {
    viewModel.saveMakePublicFlag(fragmentResult.rememberChoice, fragmentResult.makePublic)
    viewModel.updateSetIsPhotoPublic(takenPhoto.id)
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
      .doOnError { Timber.tag(TAG).e(it) }
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
