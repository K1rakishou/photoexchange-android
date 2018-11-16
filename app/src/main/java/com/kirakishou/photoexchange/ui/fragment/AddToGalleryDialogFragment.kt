package com.kirakishou.photoexchange.ui.fragment


import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.os.Bundle
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.appcompat.widget.AppCompatButton
import androidx.appcompat.widget.SwitchCompat
import android.view.View
import android.view.animation.*
import androidx.core.animation.addListener
import butterknife.BindView
import com.jakewharton.rxbinding2.view.RxView
import com.jakewharton.rxbinding2.widget.RxCompoundButton
import com.kirakishou.fixmypc.photoexchange.R
import com.kirakishou.photoexchange.helper.extension.debounceClicks
import com.kirakishou.photoexchange.helper.util.AndroidUtils
import com.kirakishou.photoexchange.mvp.viewmodel.ViewTakenPhotoActivityViewModel
import com.kirakishou.photoexchange.ui.activity.ViewTakenPhotoActivity
import io.reactivex.Completable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.rxkotlin.plusAssign
import timber.log.Timber
import javax.inject.Inject

class AddToGalleryDialogFragment : BaseFragment(), ViewTakenPhotoActivity.BackPressAwareFragment {

  @BindView(R.id.make_photo_public_container)
  lateinit var viewContainer: ConstraintLayout

  @BindView(R.id.remember_choice_switch)
  lateinit var rememberChoiceSwitch: SwitchCompat

  @BindView(R.id.do_not_make_public_button)
  lateinit var doNotMakePublicButton: AppCompatButton

  @BindView(R.id.make_public_button)
  lateinit var makePublicButton: AppCompatButton

  @Inject
  lateinit var viewModel: ViewTakenPhotoActivityViewModel

  override fun getContentView(): Int = R.layout.fragment_add_to_gallery_dialog

  private val TAG = "AddToGalleryDialogFragment"
  private var viewHeight: Float = 0f
  private val fragmentResult = FragmentResult(false, false)

  override fun onFragmentViewCreated(savedInstanceState: Bundle?) {
    initRx()

    val context = requireContext()
    viewHeight = AndroidUtils.dpToPx(context.resources.getDimension(R.dimen.make_photo_public_dialog_container_height), context)

    viewContainer.translationY = viewContainer.translationY + viewHeight
    viewContainer.alpha = 0f
  }

  private fun initRx() {
    compositeDisposable += RxCompoundButton.checkedChanges(rememberChoiceSwitch)
      .subscribeOn(AndroidSchedulers.mainThread())
      .debounceClicks()
      .doOnNext { rememberChoice -> fragmentResult.rememberChoice = rememberChoice }
      .doOnError { Timber.tag(TAG).e(it) }
      .subscribe()

    compositeDisposable += RxView.clicks(doNotMakePublicButton)
      .subscribeOn(AndroidSchedulers.mainThread())
      .debounceClicks()
      .doOnNext { fragmentResult.makePublic = false }
      .doOnNext { viewModel.addToGalleryFragmentResult.onNext(fragmentResult) }
      .doOnError { Timber.tag(TAG).e(it) }
      .subscribe()

    compositeDisposable += RxView.clicks(makePublicButton)
      .subscribeOn(AndroidSchedulers.mainThread())
      .debounceClicks()
      .doOnNext { fragmentResult.makePublic = true }
      .doOnNext { viewModel.addToGalleryFragmentResult.onNext(fragmentResult) }
      .doOnError { Timber.tag(TAG).e(it) }
      .subscribe()
  }

  override fun onResume() {
    super.onResume()

    animateAppear()
  }

  private fun animateAppear() {
    val set = AnimatorSet()

    val animation1 = ObjectAnimator.ofFloat(viewContainer, View.TRANSLATION_Y, viewHeight, 0f)
    animation1.setInterpolator(AccelerateDecelerateInterpolator())

    val animation2 = ObjectAnimator.ofFloat(viewContainer, View.ALPHA, 0f, 1f)
    animation2.setInterpolator(LinearInterpolator())

    set.playTogether(animation1, animation2)
    set.setDuration(350)
    set.start()
  }

  private fun animateDisappear(): Completable {
    return Completable.create { emitter ->
      val set = AnimatorSet()

      val animation1 = ObjectAnimator.ofFloat(viewContainer, View.TRANSLATION_Y, 0f, viewHeight)
      animation1.setInterpolator(AccelerateInterpolator())

      val animation2 = ObjectAnimator.ofFloat(viewContainer, View.ALPHA, 1f, 0f)
      animation2.setInterpolator(LinearInterpolator())

      set.playTogether(animation1, animation2)
      set.setDuration(350)
      set.addListener(onEnd = {
        emitter.onComplete()
      })
      set.start()
    }.subscribeOn(AndroidSchedulers.mainThread())
  }

  override fun onFragmentViewDestroy() {
  }

  override fun onBackPressed(): Completable {
    return animateDisappear()
  }

  override fun resolveDaggerDependency() {
    (requireActivity() as ViewTakenPhotoActivity).activityComponent
      .inject(this)
  }

  data class FragmentResult(
    var rememberChoice: Boolean,
    var makePublic: Boolean)

  companion object {
    const val BACKSTACK_TAG = "ADD_TO_GALLERY_DIALOG_FRAGMENT"
  }
}
