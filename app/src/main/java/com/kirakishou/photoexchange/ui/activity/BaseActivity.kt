package com.kirakishou.photoexchange.ui.activity

import android.arch.lifecycle.LifecycleRegistry
import android.content.Intent
import android.os.Bundle
import android.support.annotation.CallSuper
import android.support.v7.app.AppCompatActivity
import android.widget.Toast
import butterknife.ButterKnife
import butterknife.Unbinder
import com.kirakishou.photoexchange.PhotoExchangeApplication
import com.kirakishou.photoexchange.mvp.model.other.ErrorCode
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.plusAssign
import io.reactivex.subjects.PublishSubject
import timber.log.Timber


/**
 * Created by kirakishou on 7/20/2017.
 */
abstract class BaseActivity : AppCompatActivity() {

    private val TAG = "${this::class.java}"

    private val registry by lazy {
        LifecycleRegistry(this)
    }

    override fun getLifecycle(): LifecycleRegistry = registry

    protected val unknownErrorsSubject = PublishSubject.create<Throwable>()

    protected val compositeDisposable = CompositeDisposable()
    private var unBinder: Unbinder? = null

    @Suppress("UNCHECKED_CAST")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(getContentView())
        resolveDaggerDependency()

        unBinder = ButterKnife.bind(this)
        onActivityCreate(savedInstanceState, intent)
    }

    override fun onStart() {
        super.onStart()

        compositeDisposable += unknownErrorsSubject
            .observeOn(AndroidSchedulers.mainThread())
            .doOnError(this::onUnknownError)
            .subscribe(this::onUnknownError)

        onActivityStart()
    }

    override fun onResume() {
        super.onResume()
    }

    override fun onPause() {
        super.onPause()
    }

    override fun onStop() {
        onActivityStop()

        compositeDisposable.clear()
        super.onStop()
    }

    override fun onDestroy() {
        unBinder?.unbind()
        PhotoExchangeApplication.watch(this, this::class.simpleName)
        super.onDestroy()
    }

    open fun onShowToast(message: String, duration: Int = Toast.LENGTH_LONG) {
        runOnUiThread {
            Toast.makeText(this, message, duration).show()
        }
    }

    @CallSuper
    open fun onUnknownError(error: Throwable) {
        Timber.e(error)

        if (error.message != null) {
            onShowToast(error.message!!)
        } else {
            onShowToast("Unknown error")
        }

        finish()
    }

    open fun runActivity(clazz: Class<*>, finishCurrentActivity: Boolean = false) {
        val intent = Intent(this, clazz)
        startActivity(intent)

        if (finishCurrentActivity) {
            finish()
        }
    }

    open fun runActivityWithArgs(clazz: Class<*>, args: Bundle, finishCurrentActivity: Boolean = false) {
        val intent = Intent(this, clazz)
        intent.putExtras(args)
        startActivity(intent)

        if (finishCurrentActivity) {
            finish()
        }
    }

    fun showErrorCodeToast(errorCode: ErrorCode) {
        val errorMessage = when (errorCode) {
            is ErrorCode.ReceivePhotosErrors.NotEnoughPhotosOnServer,
            is ErrorCode.TakePhotoErrors.Ok,
            is ErrorCode.UploadPhotoErrors.Ok,
            is ErrorCode.ReceivePhotosErrors.Ok,
            is ErrorCode.GetGalleryPhotosErrors.Ok,
            is ErrorCode.FavouritePhotoErrors.Ok,
            is ErrorCode.ReportPhotoErrors.Ok,
            is ErrorCode.GetUserIdError.Ok,
            is ErrorCode.GetUploadedPhotosErrors.Ok,
            is ErrorCode.GetReceivedPhotosErrors.Ok -> null

            is ErrorCode.TakePhotoErrors.UnknownError,
            is ErrorCode.UploadPhotoErrors.UnknownError,
            is ErrorCode.ReceivePhotosErrors.UnknownError,
            is ErrorCode.GetGalleryPhotosErrors.UnknownError,
            is ErrorCode.FavouritePhotoErrors.UnknownError,
            is ErrorCode.ReportPhotoErrors.UnknownError,
            is ErrorCode.GetUserIdError.UnknownError,
            is ErrorCode.GetUploadedPhotosErrors.UnknownError,
            is ErrorCode.GetReceivedPhotosErrors.UnknownError -> "Unknown error"

            is ErrorCode.UploadPhotoErrors.BadRequest,
            is ErrorCode.ReceivePhotosErrors.BadRequest,
            is ErrorCode.GetGalleryPhotosErrors.BadRequest,
            is ErrorCode.FavouritePhotoErrors.BadRequest,
            is ErrorCode.ReportPhotoErrors.BadRequest,
            is ErrorCode.GetUploadedPhotosErrors.BadRequest,
            is ErrorCode.GetReceivedPhotosErrors.BadRequest -> "Bad request error"

            is ErrorCode.ReceivePhotosErrors.NoPhotosInRequest,
            is ErrorCode.GetGalleryPhotosErrors.NoPhotosInRequest,
            is ErrorCode.GetUploadedPhotosErrors.NoPhotosInRequest,
            is ErrorCode.GetReceivedPhotosErrors.NoPhotosInRequest -> "Bad request error (no photos in request)"

            is ErrorCode.TakePhotoErrors.DatabaseError,
            is ErrorCode.UploadPhotoErrors.DatabaseError,
            is ErrorCode.GetUserIdError.DatabaseError,
            is ErrorCode.GetUploadedPhotosErrors.DatabaseError,
            is ErrorCode.GetReceivedPhotosErrors.DatabaseError -> "Server database error"

            is ErrorCode.GetGalleryPhotosErrors.LocalBadServerResponse,
            is ErrorCode.UploadPhotoErrors.LocalBadServerResponse,
            is ErrorCode.ReceivePhotosErrors.LocalBadServerResponse,
            is ErrorCode.FavouritePhotoErrors.LocalBadServerResponse,
            is ErrorCode.ReportPhotoErrors.LocalBadServerResponse,
            is ErrorCode.GetUserIdError.LocalBadServerResponse,
            is ErrorCode.GetUploadedPhotosErrors.LocalBadServerResponse,
            is ErrorCode.GetReceivedPhotosErrors.LocalBadServerResponse -> "Bad server response error"

            is ErrorCode.UploadPhotoErrors.LocalTimeout,
            is ErrorCode.ReceivePhotosErrors.LocalTimeout,
            is ErrorCode.GetGalleryPhotosErrors.LocalTimeout,
            is ErrorCode.FavouritePhotoErrors.LocalTimeout,
            is ErrorCode.ReportPhotoErrors.LocalTimeout,
            is ErrorCode.GetUserIdError.LocalTimeout,
            is ErrorCode.GetUploadedPhotosErrors.LocalTimeout,
            is ErrorCode.GetReceivedPhotosErrors.LocalTimeout,
            is ErrorCode.TakePhotoErrors.TimeoutException -> "Operation timeout error"

            is ErrorCode.ReceivePhotosErrors.LocalDatabaseError,
            is ErrorCode.GetGalleryPhotosErrors.LocalDatabaseError,
            is ErrorCode.UploadPhotoErrors.LocalDatabaseError,
            is ErrorCode.GetUserIdError.LocalDatabaseError -> "Couldn't store data from the server on the disk"

            is ErrorCode.GetReceivedPhotosErrors.LocalUserIdIsEmpty,
            is ErrorCode.ReceivePhotosErrors.LocalCouldNotGetUserId,
            is ErrorCode.GetUploadedPhotosErrors.LocalUserIdIsEmpty -> "This operation cannot be done without user photoId"

            is ErrorCode.UploadPhotoErrors.LocalNoPhotoFileOnDisk -> "No photo on disk"
            is ErrorCode.UploadPhotoErrors.LocalInterrupted -> "Interrupted by user"
            is ErrorCode.ReceivePhotosErrors.LocalTooManyPhotosRequested -> "Too many photos in one request"
            is ErrorCode.ReceivePhotosErrors.LocalNotEnoughPhotosUploaded -> "Upload more photos first"
            is ErrorCode.TakePhotoErrors.CameraIsNotAvailable -> "Camera is not available on this phone"
            is ErrorCode.TakePhotoErrors.CameraIsNotStartedException -> "Camera is not started"
            is ErrorCode.TakePhotoErrors.CouldNotTakePhoto -> "Could not take a photo"
            is ErrorCode.UploadPhotoErrors.CouldNotRotatePhoto -> "Could not rotate a photo"
        }

        if (errorMessage != null) {
            Timber.tag(TAG).e(errorMessage)
            onShowToast(errorMessage, Toast.LENGTH_SHORT)
        }
    }

    protected abstract fun getContentView(): Int
    protected abstract fun onActivityCreate(savedInstanceState: Bundle?, intent: Intent)
    protected abstract fun onActivityStart()
    protected abstract fun onActivityStop()
    protected abstract fun resolveDaggerDependency()
}