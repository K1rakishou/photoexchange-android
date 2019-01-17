package com.kirakishou.photoexchange.ui.activity

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.ViewAnimationUtils
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.AccelerateInterpolator
import android.widget.ImageView
import androidx.core.animation.addListener
import butterknife.BindView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.kirakishou.fixmypc.photoexchange.R
import com.kirakishou.photoexchange.PhotoExchangeApplication
import com.kirakishou.photoexchange.di.module.activity.TakePhotoActivityModule
import com.kirakishou.photoexchange.helper.CameraProvider
import com.kirakishou.photoexchange.helper.Vibrator
import com.kirakishou.photoexchange.helper.permission.PermissionManager
import com.kirakishou.photoexchange.helper.util.AndroidUtils
import com.kirakishou.photoexchange.mvrx.model.photo.TakenPhoto
import com.kirakishou.photoexchange.mvrx.viewmodel.TakePhotoActivityViewModel
import com.kirakishou.photoexchange.ui.dialog.CameraIsNotAvailableDialog
import io.fotoapparat.view.CameraView
import io.reactivex.Completable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.rxkotlin.plusAssign
import kotlinx.coroutines.launch
import javax.inject.Inject

class TakePhotoActivity : BaseActivity() {

  @BindView(R.id.camera_view)
  lateinit var cameraView: CameraView

  @BindView(R.id.take_photo_button)
  lateinit var takePhotoButton: FloatingActionButton

  @BindView(R.id.circular_reveal_view)
  lateinit var circularRevealView: View

  @Inject
  lateinit var viewModel: TakePhotoActivityViewModel

  @Inject
  lateinit var cameraProvider: CameraProvider

  @Inject
  lateinit var vibrator: Vibrator

  @Inject
  lateinit var permissionManager: PermissionManager

  private val TAG = "TakePhotoActivity"
  private val VIBRATION_TIME_MS = 25L
  private val BUTTONS_TRANSITION_DELTA = 96f
  private var translationDelta: Float = 0f

  override fun getContentView(): Int = R.layout.activity_take_photo

  override fun onActivityCreate(savedInstanceState: Bundle?, intent: Intent) {
    initViews()
    initRx()

    cameraProvider.initCamera(cameraView)
  }

  override fun onActivityStart() {
    startCamera()
    animateAppear()
  }

  override fun onActivityStop() {
    cameraProvider.stopCamera()
    permissionManager.clear()
  }

  private fun startCamera() {
    if (!cameraProvider.isAvailable()) {
      showCameraIsNotAvailableDialog()
      return
    }

    cameraProvider.startCamera()
  }

  private fun showCameraIsNotAvailableDialog() {
    CameraIsNotAvailableDialog().show(this@TakePhotoActivity) {
      setResult(Activity.RESULT_CANCELED)
      finish()
    }
  }

  private fun initViews() {
    translationDelta = AndroidUtils.dpToPx(BUTTONS_TRANSITION_DELTA, this)
    takePhotoButton.translationY = takePhotoButton.translationY + translationDelta
    circularRevealView.visibility = View.GONE
  }

  private fun initRx() {
    takePhotoButton.setOnClickListener {
      vibrator.vibrate(VIBRATION_TIME_MS)

      launch {
        animateCameraViewHide()

        try {
          val takenPhoto = takePhoto()
          if (takenPhoto == null) {
            onShowToast(getString(R.string.take_photo_activity_could_not_take_photo))
            return@launch
          }

          onPhotoTaken(takenPhoto)
        } catch (error: Exception) {
          onShowToast(error.message)
        }
      }
    }
  }

  private suspend fun takePhoto(): TakenPhoto? {
    return cameraProvider.takePhoto()
  }

  private fun onPhotoTaken(takenPhoto: TakenPhoto) {
    val intent = Intent()
    intent.putExtra(TAKEN_PHOTO_BUNDLE_KEY, takenPhoto.toBundle())

    setResult(Activity.RESULT_OK, intent)
    finish()
  }

  private fun animateCameraViewHide() {
    val cx = (takePhotoButton.x + (takePhotoButton.measuredWidth / 2)).toInt()
    val cy = (takePhotoButton.y + (takePhotoButton.measuredHeight / 2)).toInt()

    val finalRadius = Math.max(cameraView.width, cameraView.height) / 2
    val anim = ViewAnimationUtils.createCircularReveal(circularRevealView, cx, cy, 0f, finalRadius.toFloat())

    (takePhotoButton as ImageView).visibility = View.GONE
    circularRevealView.visibility = View.VISIBLE

    anim.duration = 100
    anim.start()
  }

  private fun animateAppear() {
    runOnUiThread {
      val set = AnimatorSet()

      val animation = ObjectAnimator.ofFloat(takePhotoButton, View.TRANSLATION_Y, translationDelta, 0f)
      animation.setInterpolator(AccelerateDecelerateInterpolator())

      set.play(animation)
      set.setStartDelay(50)
      set.setDuration(200)
      set.addListener(onEnd = {
        takePhotoButton.isClickable = true
      }, onStart = {
        takePhotoButton.show()
      })
      set.start()
    }
  }

  private fun animateDisappear(): Completable {
    return Completable.create { emitter ->
      val set = AnimatorSet()

      val animation = ObjectAnimator.ofFloat(takePhotoButton, View.TRANSLATION_Y, 0f, translationDelta)
      animation.setInterpolator(AccelerateInterpolator())

      set.play(animation)
      set.setDuration(250)
      set.addListener(onStart = {
        takePhotoButton.isClickable = false
      }, onEnd = {
        takePhotoButton.hide()
        emitter.onComplete()
      })
      set.start()
    }
  }

  override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
    super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    permissionManager.onRequestPermissionsResult(requestCode, permissions, grantResults)
  }

  override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
    super.onActivityResult(requestCode, resultCode, data)

    if (requestCode == ViewTakenPhotoActivity.VIEW_TAKEN_PHOTO_REQUEST_CODE) {
      if (resultCode == Activity.RESULT_OK) {
        setResult(Activity.RESULT_OK)
        finish()
      }
    }
  }

  override fun onBackPressed() {
    compositeDisposable += animateDisappear()
      .observeOn(AndroidSchedulers.mainThread())
      .doOnComplete { super.onBackPressed() }
      .subscribe()
  }

  override fun resolveDaggerDependency() {
    (application as PhotoExchangeApplication).applicationComponent
      .plus(TakePhotoActivityModule(this))
      .inject(this)
  }

  companion object {
    const val TAKE_PHOTO_REQUEST_CODE = 0x1
    const val TAKEN_PHOTO_BUNDLE_KEY = "taken_photo"
  }
}
