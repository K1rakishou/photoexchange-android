package com.kirakishou.photoexchange.ui.fragment


import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.animation.AnticipateInterpolator
import android.view.animation.OvershootInterpolator
import android.widget.ImageView
import androidx.core.animation.addListener
import butterknife.BindView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.jakewharton.rxbinding2.view.RxView
import com.kirakishou.fixmypc.photoexchange.R
import com.kirakishou.photoexchange.helper.ImageLoader
import com.kirakishou.photoexchange.helper.PhotosVisibility
import com.kirakishou.photoexchange.helper.extension.debounceClicks
import com.kirakishou.photoexchange.mvp.model.photo.TakenPhoto
import com.kirakishou.photoexchange.mvp.viewmodel.ViewTakenPhotoActivityViewModel
import com.kirakishou.photoexchange.ui.activity.ViewTakenPhotoActivity
import io.reactivex.Completable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.rxkotlin.plusAssign
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject


class ViewTakenPhotoFragment : BaseFragment(), ViewTakenPhotoActivity.BackPressAwareFragment {

  @BindView(R.id.iv_photo_view)
  lateinit var ivPhotoView: ImageView

  @BindView(R.id.fab_close_activity)
  lateinit var fabCloseActivity: FloatingActionButton

  @BindView(R.id.fab_queue_up_photo)
  lateinit var fabQueueUpPhoto: FloatingActionButton

  @Inject
  lateinit var imageLoader: ImageLoader

  @Inject
  lateinit var viewModel: ViewTakenPhotoActivityViewModel

  private val TAG = "ViewTakenPhotoFragment"
  lateinit var takenPhoto: TakenPhoto

  override fun getContentView(): Int = R.layout.fragment_view_taken_photo

  override fun onFragmentViewCreated(savedInstanceState: Bundle?) {
    takenPhoto = TakenPhoto.fromBundle(arguments)

    initRx()
    initViews()

    animateAppear()
  }

  override fun onFragmentViewDestroy() {
  }

  private fun initViews() {
    imageLoader.loadPhotoFromDiskInto(takenPhoto.getFile(), ivPhotoView)

    fabCloseActivity.scaleX = 0f
    fabCloseActivity.scaleY = 0f

    fabQueueUpPhoto.scaleX = 0f
    fabQueueUpPhoto.scaleY = 0f
  }

  private fun initRx() {
    compositeDisposable += RxView.clicks(fabCloseActivity)
      .subscribeOn(AndroidSchedulers.mainThread())
      .debounceClicks()
      .doOnNext { (requireActivity() as ViewTakenPhotoActivity).onBackPressed() }
      .doOnError { Timber.tag(TAG).e(it) }
      .subscribe()

    fabQueueUpPhoto.setOnClickListener {
      launch { queueUpPhoto() }
    }
  }

  private suspend fun queueUpPhoto() {
    viewModel.queueUpTakenPhoto(takenPhoto.id)
    val makePublicFlag = viewModel.getMakePublicFlag()

    when (makePublicFlag) {
      PhotosVisibility.AlwaysPublic,
      PhotosVisibility.AlwaysPrivate -> {
        val makePublic = makePublicFlag == PhotosVisibility.AlwaysPublic
        viewModel.addToGalleryFragmentResult.onNext(AddToGalleryDialogFragment.FragmentResult(false, makePublic))
      }
      PhotosVisibility.Neither -> {
        (requireActivity() as ViewTakenPhotoActivity).showDialogFragment()
      }
    }
  }

  fun animateAppear() {
    requireActivity().runOnUiThread {
      val set = AnimatorSet()

      val animation1 = ObjectAnimator.ofFloat(fabCloseActivity, View.SCALE_X, 0f, 1f)
      animation1.setInterpolator(OvershootInterpolator())

      val animation2 = ObjectAnimator.ofFloat(fabCloseActivity, View.SCALE_Y, 0f, 1f)
      animation2.setInterpolator(OvershootInterpolator())

      val animation3 = ObjectAnimator.ofFloat(fabQueueUpPhoto, View.SCALE_X, 0f, 1f)
      animation3.setInterpolator(OvershootInterpolator())

      val animation4 = ObjectAnimator.ofFloat(fabQueueUpPhoto, View.SCALE_Y, 0f, 1f)
      animation4.setInterpolator(OvershootInterpolator())

      set.playTogether(animation1, animation2, animation3, animation4)
      set.setStartDelay(100)
      set.setDuration(250)
      set.start()
    }
  }

  fun animateDisappear(): Completable {
    return Completable.create { emitter ->
      val set = AnimatorSet()

      val animation1 = ObjectAnimator.ofFloat(fabCloseActivity, View.SCALE_X, 1f, 0f)
      animation1.setInterpolator(AnticipateInterpolator())

      val animation2 = ObjectAnimator.ofFloat(fabCloseActivity, View.SCALE_Y, 1f, 0f)
      animation2.setInterpolator(AnticipateInterpolator())

      val animation3 = ObjectAnimator.ofFloat(fabQueueUpPhoto, View.SCALE_X, 1f, 0f)
      animation3.setInterpolator(AnticipateInterpolator())

      val animation4 = ObjectAnimator.ofFloat(fabQueueUpPhoto, View.SCALE_Y, 1f, 0f)
      animation4.setInterpolator(AnticipateInterpolator())

      set.playTogether(animation1, animation2, animation3, animation4)
      set.setDuration(250)
      set.addListener(onEnd = {
        emitter.onComplete()
      })
      set.start()
    }.subscribeOn(AndroidSchedulers.mainThread())
  }

  override fun onBackPressed(): Completable {
    return animateDisappear()
  }

  override fun resolveDaggerDependency() {
    (requireActivity() as ViewTakenPhotoActivity).activityComponent
      .inject(this)
  }

  companion object {
    const val BACKSTACK_TAG = "VIEW_TAKEN_PHOTO_FRAGMENT"

    fun newInstance(intent: Intent): ViewTakenPhotoFragment {
      val fragment = ViewTakenPhotoFragment()
      fragment.arguments = intent.extras

      return fragment
    }
  }
}
