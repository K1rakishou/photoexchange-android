package com.kirakishou.photoexchange.ui.activity

import android.Manifest
import android.arch.lifecycle.ViewModelProviders
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.support.design.widget.FloatingActionButton
import android.view.View
import butterknife.BindView
import com.kirakishou.fixmypc.photoexchange.R
import com.kirakishou.photoexchange.PhotoExchangeApplication
import com.kirakishou.photoexchange.base.BaseActivity
import com.kirakishou.photoexchange.di.module.TakePhotoActivityModule
import com.kirakishou.photoexchange.helper.CameraProvider
import com.kirakishou.photoexchange.helper.concurrency.coroutine.CoroutineThreadPoolProvider
import com.kirakishou.photoexchange.helper.permission.PermissionManager
import com.kirakishou.photoexchange.mvp.model.MyPhoto
import com.kirakishou.photoexchange.mvp.view.TakePhotoActivityView
import com.kirakishou.photoexchange.mvp.viewmodel.TakePhotoActivityViewModel
import com.kirakishou.photoexchange.mvp.viewmodel.factory.TakePhotoActivityViewModelFactory
import com.kirakishou.photoexchange.ui.dialog.AppCannotWorkWithoutCameraPermissionDialog
import com.kirakishou.photoexchange.ui.dialog.CameraIsNotAvailableDialog
import io.fotoapparat.view.CameraView
import io.reactivex.Single
import kotlinx.coroutines.experimental.async
import timber.log.Timber
import java.io.File
import javax.inject.Inject

class TakePhotoActivity : BaseActivity<TakePhotoActivityViewModel>(), TakePhotoActivityView {

    @BindView(R.id.camera_view)
    lateinit var cameraView: CameraView

    @BindView(R.id.take_photo_button)
    lateinit var takePhotoButton: FloatingActionButton

    @Inject
    lateinit var viewModelFactory: TakePhotoActivityViewModelFactory

    @Inject
    lateinit var coroutinesPool: CoroutineThreadPoolProvider

    @Inject
    lateinit var permissionManager: PermissionManager

    @Inject
    lateinit var cameraProvider: CameraProvider

    private val tag = "[${this::class.java.simpleName}]: "

    override fun initViewModel(): TakePhotoActivityViewModel? {
        return ViewModelProviders.of(this, viewModelFactory).get(TakePhotoActivityViewModel::class.java)
    }

    override fun getContentView(): Int = R.layout.activity_take_photo

    override fun onActivityCreate(savedInstanceState: Bundle?, intent: Intent) {
        initViews()
        checkPermissions()
    }

    private fun checkPermissions() {
        val requestedPermissions = arrayOf(Manifest.permission.CAMERA, Manifest.permission.ACCESS_FINE_LOCATION)
        permissionManager.askForPermission(this, requestedPermissions) { permissions, grantResults ->
            val index = permissions.indexOf(Manifest.permission.CAMERA)
            if (index == -1) {
                throw RuntimeException("Couldn't find Manifest.permission.CAMERA in result permissions")
            }

            if (grantResults[index] == PackageManager.PERMISSION_DENIED) {
                Timber.tag(tag).d("getPermissions() Could not obtain camera permission")
                showAppCannotWorkWithoutCameraPermissionDialog()
                return@askForPermission
            }

            initCamera()
        }
    }

    private fun initViews() {
        takePhotoButton.setOnClickListener {
            async(coroutinesPool.provideCommon()) {
                getViewModel().takePhoto()
            }
        }
    }

    private fun initCamera() {
        cameraProvider.provideCamera(cameraView)
    }

    override fun onActivityDestroy() {
    }

    override fun onResume() {
        super.onResume()
        cameraProvider.startCamera()
    }

    override fun onPause() {
        super.onPause()
        cameraProvider.stopCamera()
    }

    private fun showAppCannotWorkWithoutCameraPermissionDialog() {
        AppCannotWorkWithoutCameraPermissionDialog().show(this, {
            finish()
        })
    }

//    private fun showCameraRationaleDialog(token: PermissionToken) {
//        CameraRationaleDialog().show(this, {
//            token.continuePermissionRequest()
//        }, {
//            token.cancelPermissionRequest()
//        })
//    }

    private fun showCameraIsNotAvailableDialog() {
        CameraIsNotAvailableDialog().show(this, {
            finish()
        })
    }

    override fun takePhoto(file: File): Single<Boolean> = cameraProvider.takePhoto(file)

    override fun onPhotoTaken(myPhoto: MyPhoto) {
    }

    override fun showTakePhotoButton() {
        async(coroutinesPool.provideMain()) {
            takePhotoButton.visibility = View.VISIBLE
        }
    }

    override fun hideTakePhotoButton() {
        async(coroutinesPool.provideMain()) {
            takePhotoButton.visibility = View.GONE
        }
    }

    override fun showToast(message: String, duration: Int) {
        onShowToast(message, duration)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        permissionManager.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

    override fun resolveDaggerDependency() {
        (application as PhotoExchangeApplication).applicationComponent
            .plus(TakePhotoActivityModule(this))
            .inject(this)
    }
}
