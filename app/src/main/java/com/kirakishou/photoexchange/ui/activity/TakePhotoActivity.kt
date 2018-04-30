package com.kirakishou.photoexchange.ui.activity

import android.Manifest
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.support.design.widget.FloatingActionButton
import android.support.v4.app.ActivityCompat
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.AccelerateInterpolator
import android.view.animation.OvershootInterpolator
import android.widget.ImageView
import androidx.core.animation.addListener
import butterknife.BindView
import com.jakewharton.rxbinding2.view.RxView
import com.kirakishou.fixmypc.photoexchange.R
import com.kirakishou.photoexchange.PhotoExchangeApplication
import com.kirakishou.photoexchange.di.module.TakePhotoActivityModule
import com.kirakishou.photoexchange.helper.CameraProvider
import com.kirakishou.photoexchange.helper.extension.debounceClicks
import com.kirakishou.photoexchange.helper.permission.PermissionManager
import com.kirakishou.photoexchange.helper.util.AndroidUtils
import com.kirakishou.photoexchange.mvp.model.MyPhoto
import com.kirakishou.photoexchange.mvp.view.TakePhotoActivityView
import com.kirakishou.photoexchange.mvp.viewmodel.TakePhotoActivityViewModel
import com.kirakishou.photoexchange.ui.dialog.AppCannotWorkWithoutCameraPermissionDialog
import com.kirakishou.photoexchange.ui.dialog.CameraIsNotAvailableDialog
import com.kirakishou.photoexchange.ui.dialog.CameraRationaleDialog
import io.fotoapparat.view.CameraView
import io.reactivex.Completable
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.rxkotlin.plusAssign
import timber.log.Timber
import java.io.File
import javax.inject.Inject

class TakePhotoActivity : BaseActivity(), TakePhotoActivityView {

    @BindView(R.id.iv_show_all_photos)
    lateinit var ivShowAllPhotos: ImageView

    @BindView(R.id.camera_view)
    lateinit var cameraView: CameraView

    @BindView(R.id.take_photo_button)
    lateinit var takePhotoButton: FloatingActionButton

    @Inject
    lateinit var viewModel: TakePhotoActivityViewModel

    @Inject
    lateinit var permissionManager: PermissionManager

    @Inject
    lateinit var cameraProvider: CameraProvider

    private val tag = "[${this::class.java.simpleName}]: "
    private var translationDelta: Float = 0f

    override fun getContentView(): Int = R.layout.activity_take_photo

    override fun onActivityCreate(savedInstanceState: Bundle?, intent: Intent) {
        initViews()
    }

    private fun initViews() {
        translationDelta = AndroidUtils.dpToPx(96f, this)

        takePhotoButton.translationY = takePhotoButton.translationY + translationDelta
        ivShowAllPhotos.translationY = ivShowAllPhotos.translationY + translationDelta
    }

    override fun onInitRx() {
        compositeDisposable += RxView.clicks(takePhotoButton)
            .subscribeOn(AndroidSchedulers.mainThread())
            .debounceClicks()
            .doOnNext { viewModel.takePhoto() }
            .subscribe()

        compositeDisposable += RxView.clicks(ivShowAllPhotos)
            .subscribeOn(AndroidSchedulers.mainThread())
            .debounceClicks()
            .doOnNext { runActivity(AllPhotosActivity::class.java) }
            .subscribe()
    }

    override fun onActivityStart() {
        viewModel.setView(this)
    }

    override fun onResume() {
        super.onResume()

        checkPermissions()
    }

    override fun onPause() {
        super.onPause()
        cameraProvider.stopCamera()
    }

    override fun onActivityStop() {
        viewModel.clearView()
    }

    private fun checkPermissions() {
        val requestedPermissions = arrayOf(Manifest.permission.CAMERA)
        permissionManager.askForPermission(this, requestedPermissions) { permissions, grantResults ->
            val index = permissions.indexOf(Manifest.permission.CAMERA)
            if (index == -1) {
                throw RuntimeException("Couldn't find Manifest.permission.CAMERA in result permissions")
            }

            if (grantResults[index] == PackageManager.PERMISSION_DENIED) {
                if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.CAMERA)) {
                    showCameraRationaleDialog()
                } else {
                    Timber.tag(tag).d("getPermissions() Could not obtain camera permission")
                    showAppCannotWorkWithoutCameraPermissionDialog()
                }

                return@askForPermission
            }

            animateAppear()
            startCamera()
        }
    }

    private fun startCamera() {
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

    private fun showAppCannotWorkWithoutCameraPermissionDialog() {
        AppCannotWorkWithoutCameraPermissionDialog().show(this) {
            finish()
        }
    }

    private fun showCameraRationaleDialog() {
        CameraRationaleDialog().show(this, {
            checkPermissions()
        }, {
            finish()
        })
    }

    private fun showCameraIsNotAvailableDialog() {
        CameraIsNotAvailableDialog().show(this) {
            finish()
        }
    }

    override fun takePhoto(file: File): Single<Boolean> = cameraProvider.takePhoto(file)

    override fun onPhotoTaken(myPhoto: MyPhoto) {
        runActivityWithArgs(ViewTakenPhotoActivity::class.java,
            myPhoto.toBundle(), false)
    }

    private fun animateAppear() {
        runOnUiThread {
            val set = AnimatorSet()

            val animation1 = ObjectAnimator.ofFloat(takePhotoButton, View.TRANSLATION_Y, translationDelta, 0f)
            animation1.setInterpolator(AccelerateDecelerateInterpolator())

            val animation2 = ObjectAnimator.ofFloat(ivShowAllPhotos, View.TRANSLATION_Y, translationDelta, 0f)
            animation2.setInterpolator(AccelerateDecelerateInterpolator())

            set.playTogether(animation1, animation2)
            set.setStartDelay(50)
            set.setDuration(200)
            set.start()
        }
    }

    private fun animateDisappear(): Completable {
        return Completable.create { emitter ->
            val set = AnimatorSet()

            val animation1 = ObjectAnimator.ofFloat(takePhotoButton, View.TRANSLATION_Y, 0f, translationDelta)
            animation1.setInterpolator(AccelerateInterpolator())

            val animation2 = ObjectAnimator.ofFloat(ivShowAllPhotos, View.TRANSLATION_Y, 0f, translationDelta)
            animation2.setInterpolator(AccelerateInterpolator())

            set.playTogether(animation1, animation2)
            set.setDuration(250)
            set.addListener(onEnd = {
                emitter.onComplete()
            })
            set.start()
        }
    }

    override fun showToast(message: String, duration: Int) {
        onShowToast(message, duration)
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
