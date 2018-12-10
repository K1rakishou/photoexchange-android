package com.kirakishou.photoexchange.ui.activity

import android.Manifest
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.View
import android.view.ViewAnimationUtils
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.AccelerateInterpolator
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.core.animation.addListener
import androidx.core.app.ActivityCompat
import butterknife.BindView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.jakewharton.rxbinding2.view.RxView
import com.kirakishou.fixmypc.photoexchange.R
import com.kirakishou.photoexchange.PhotoExchangeApplication
import com.kirakishou.photoexchange.di.module.activity.TakePhotoActivityModule
import com.kirakishou.photoexchange.helper.CameraProvider
import com.kirakishou.photoexchange.helper.Vibrator
import com.kirakishou.photoexchange.helper.extension.debounceClicks
import com.kirakishou.photoexchange.helper.permission.PermissionManager
import com.kirakishou.photoexchange.helper.util.AndroidUtils
import com.kirakishou.photoexchange.mvp.model.photo.TakenPhoto
import com.kirakishou.photoexchange.mvp.viewmodel.TakePhotoActivityViewModel
import com.kirakishou.photoexchange.ui.dialog.AppCannotWorkWithoutCameraPermissionDialog
import com.kirakishou.photoexchange.ui.dialog.CameraIsNotAvailableDialog
import com.kirakishou.photoexchange.ui.dialog.CameraRationaleDialog
import com.kirakishou.photoexchange.ui.dialog.GpsRationaleDialog
import io.fotoapparat.view.CameraView
import io.reactivex.Completable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.rxkotlin.plusAssign
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

class TakePhotoActivity : BaseActivity() {

  @BindView(R.id.show_all_photos_btn)
  lateinit var showAllPhotosButton: LinearLayout

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

  private val permissionManager = PermissionManager()

  private val TAG = "TakePhotoActivity"
  private val VIBRATION_TIME_MS = 25L
  private val BUTTONS_TRANSITION_DELTA = 96f
  private var translationDelta: Float = 0f

  override fun getContentView(): Int = R.layout.activity_take_photo

  override fun onActivityCreate(savedInstanceState: Bundle?, intent: Intent) {
  }

  override fun onActivityStart() {
    initViews()
    initRx()
    checkPermissions()
  }

  override fun onPause() {
    super.onPause()
    cameraProvider.stopCamera()
  }

  override fun onActivityStop() {
  }

  private fun initViews() {
    translationDelta = AndroidUtils.dpToPx(BUTTONS_TRANSITION_DELTA, this)

    takePhotoButton.translationY = takePhotoButton.translationY + translationDelta
    showAllPhotosButton.translationX = showAllPhotosButton.translationX + translationDelta

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
            onShowToast("Could not take photo")
            return@launch
          }

          onPhotoTaken(takenPhoto)
        } catch (error: Exception) {
          onShowToast(error.message)
        }
      }
    }

    compositeDisposable += RxView.clicks(showAllPhotosButton)
      .subscribeOn(AndroidSchedulers.mainThread())
      .debounceClicks()
      .doOnNext { runActivity(PhotosActivity::class.java) }
      .doOnError { Timber.tag(TAG).e(it) }
      .subscribe()
  }

  private fun animateCameraViewHide() {
    val cx = (takePhotoButton.x + (takePhotoButton.measuredWidth / 2)).toInt()
    val cy = (takePhotoButton.y + (takePhotoButton.measuredHeight / 2)).toInt()

    val finalRadius = Math.max(cameraView.width, cameraView.height) / 2
    val anim = ViewAnimationUtils.createCircularReveal(circularRevealView, cx, cy, 0f, finalRadius.toFloat())

    (takePhotoButton as ImageView).visibility = View.GONE
    circularRevealView.visibility = View.VISIBLE
    showAllPhotosButton.visibility = View.GONE

    anim.duration = 100
    anim.start()
  }

  private fun checkPermissions() {
    val requestedPermissions = arrayOf(Manifest.permission.CAMERA, Manifest.permission.ACCESS_FINE_LOCATION)

    permissionManager.askForPermission(this, requestedPermissions) { permissions, grantResults ->
      val cameraIndex = permissions.indexOf(Manifest.permission.CAMERA)
      if (cameraIndex == -1) {
        throw RuntimeException("Couldn't find Manifest.permission.CAMERA in result permissions")
      }

      val gpsIndex = permissions.indexOf(Manifest.permission.ACCESS_FINE_LOCATION)
      if (gpsIndex == -1) {
        throw RuntimeException("Couldn't find Manifest.permission.ACCESS_FINE_LOCATION in result permissions")
      }

      if (grantResults[cameraIndex] == PackageManager.PERMISSION_DENIED) {
        if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.CAMERA)) {
          launch { showCameraRationaleDialog() }
        } else {
          Timber.tag(TAG).d("getPermissions() Could not obtain camera permission")
          launch { showAppCannotWorkWithoutCameraPermissionDialog() }
        }

        return@askForPermission
      }

      var granted = true

      if (grantResults[gpsIndex] == PackageManager.PERMISSION_DENIED) {
        granted = false

        if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.ACCESS_FINE_LOCATION)) {
          launch { showGpsRationaleDialog() }
          return@askForPermission
        }
      }

      launch {
        viewModel.updateGpsPermissionGranted(granted)
        onPermissionCallback()
      }
    }
  }

  private suspend fun onPermissionCallback() {
    startCamera()
    animateAppear()
  }

  private suspend fun startCamera() {
    if (cameraProvider.isStarted()) {
      return
    }

    cameraProvider.provideCamera(cameraView)

    if (!cameraProvider.isAvailable()) {
      showCameraIsNotAvailableDialog()
      return
    }

    cameraProvider.startCamera()
  }

  private suspend fun showGpsRationaleDialog() {
    GpsRationaleDialog(this).show(this, {
      checkPermissions()
    }, {
      onPermissionCallback()
    })
  }

  private suspend fun showAppCannotWorkWithoutCameraPermissionDialog() {
    AppCannotWorkWithoutCameraPermissionDialog(this).show(this) {
      finish()
    }
  }

  private suspend fun showCameraRationaleDialog() {
    CameraRationaleDialog(this).show(this, {
      checkPermissions()
    }, {
      finish()
    })
  }

  private suspend fun showCameraIsNotAvailableDialog() {
    CameraIsNotAvailableDialog(this).show(this) {
      finish()
    }
  }

  private suspend fun takePhoto(): TakenPhoto? {
    return cameraProvider.takePhoto()
  }

  private fun onPhotoTaken(takenPhoto: TakenPhoto) {
    runActivityWithArgs(ViewTakenPhotoActivity::class.java, takenPhoto.toBundle())
  }

  private fun animateAppear() {
    runOnUiThread {
      val set = AnimatorSet()

      val animation1 = ObjectAnimator.ofFloat(takePhotoButton, View.TRANSLATION_Y, translationDelta, 0f)
      animation1.setInterpolator(AccelerateDecelerateInterpolator())

      val animation2 = ObjectAnimator.ofFloat(showAllPhotosButton, View.TRANSLATION_X, translationDelta, 0f)
      animation2.setInterpolator(AccelerateDecelerateInterpolator())

      set.playTogether(animation1, animation2)
      set.setStartDelay(50)
      set.setDuration(200)
      set.addListener(onEnd = {
        if (::takePhotoButton.isInitialized) {
          takePhotoButton.isClickable = true
        }

        if (::showAllPhotosButton.isInitialized) {
          showAllPhotosButton.isClickable = true
        }
      }, onStart = {
        if (::takePhotoButton.isInitialized) {
          takePhotoButton.show()
        }

        if (::showAllPhotosButton.isInitialized) {
          showAllPhotosButton.visibility = View.VISIBLE
        }
      })
      set.start()
    }
  }

  private fun animateDisappear(): Completable {
    return Completable.create { emitter ->
      val set = AnimatorSet()

      val animation1 = ObjectAnimator.ofFloat(takePhotoButton, View.TRANSLATION_Y, 0f, translationDelta)
      animation1.setInterpolator(AccelerateInterpolator())

      val animation2 = ObjectAnimator.ofFloat(showAllPhotosButton, View.TRANSLATION_X, 0f, translationDelta)
      animation2.setInterpolator(AccelerateInterpolator())

      set.playTogether(animation1, animation2)
      set.setDuration(250)
      set.addListener(onStart = {
        if (::takePhotoButton.isInitialized) {
          takePhotoButton.isClickable = false
        }

        if (::showAllPhotosButton.isInitialized) {
          showAllPhotosButton.isClickable = false
        }
      }, onEnd = {
        if (::takePhotoButton.isInitialized) {
          takePhotoButton.hide()
        }

        if (::showAllPhotosButton.isInitialized) {
          showAllPhotosButton.visibility = View.GONE
        }

        emitter.onComplete()
      })
      set.start()
    }
  }

  override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
    super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    permissionManager.onRequestPermissionsResult(requestCode, permissions, grantResults)
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
}
