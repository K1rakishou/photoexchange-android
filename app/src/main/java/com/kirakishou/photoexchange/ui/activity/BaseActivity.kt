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

    open fun runActivityWithArgs(clazz: Class<*>, args: Bundle, finishCurrentActivity: Boolean) {
        val intent = Intent(this, clazz)
        intent.putExtras(args)
        startActivity(intent)

        if (finishCurrentActivity) {
            finish()
        }
    }

    fun showErrorCodeToast(errorCode: ErrorCode) {
        val errorMessage = when (errorCode) {
            is ErrorCode.UploadPhotoErrors.Remote.Ok,
            is ErrorCode.ReceivePhotosErrors.Remote.Ok,
            is ErrorCode.GetGalleryPhotosErrors.Remote.Ok,
            is ErrorCode.FavouritePhotoErrors.Remote.Ok,
            is ErrorCode.TakePhotoErrors.Ok,
            is ErrorCode.GetUserIdError.Remote.Ok,
            is ErrorCode.GetGalleryPhotosInfoError.Remote.Ok,
            is ErrorCode.ReportPhotoErrors.Remote.Ok -> null

            is ErrorCode.UploadPhotoErrors.Remote.UnknownError,
            is ErrorCode.ReceivePhotosErrors.Remote.UnknownError,
            is ErrorCode.GetGalleryPhotosErrors.Remote.UnknownError,
            is ErrorCode.FavouritePhotoErrors.Remote.UnknownError,
            is ErrorCode.GetUserIdError.Remote.UnknownError,
            is ErrorCode.GetGalleryPhotosInfoError.Remote.UnknownError,
            is ErrorCode.ReportPhotoErrors.Remote.UnknownError -> "Unknown error"

            is ErrorCode.UploadPhotoErrors.Remote.BadRequest,
            is ErrorCode.ReceivePhotosErrors.Remote.BadRequest,
            is ErrorCode.GetGalleryPhotosErrors.Remote.BadRequest,
            is ErrorCode.FavouritePhotoErrors.Remote.BadRequest,
            is ErrorCode.GetGalleryPhotosInfoError.Remote.BadRequest,
            is ErrorCode.ReportPhotoErrors.Remote.BadRequest -> "Bad request error"

            is ErrorCode.UploadPhotoErrors.Local.Timeout,
            is ErrorCode.ReceivePhotosErrors.Local.Timeout,
            is ErrorCode.GetGalleryPhotosErrors.Local.Timeout,
            is ErrorCode.FavouritePhotoErrors.Local.Timeout,
            is ErrorCode.GetUserIdError.Local.Timeout,
            is ErrorCode.GetGalleryPhotosInfoError.Local.Timeout,
            is ErrorCode.ReportPhotoErrors.Local.Timeout -> "Operation timeout error"

            is ErrorCode.UploadPhotoErrors.Remote.DatabaseError,
            is ErrorCode.GetUserIdError.Remote.DatabaseError,
            is ErrorCode.GetUserIdError.Local.DatabaseError,
            is ErrorCode.ReceivePhotosErrors.Remote.DatabaseError -> "Server database error"

            is ErrorCode.UploadPhotoErrors.Local.BadServerResponse,
            is ErrorCode.ReceivePhotosErrors.Local.BadServerResponse,
            is ErrorCode.GetGalleryPhotosErrors.Local.BadServerResponse,
            is ErrorCode.ReportPhotoErrors.Local.BadServerResponse,
            is ErrorCode.GetUserIdError.Local.BadServerResponse,
            is ErrorCode.GetGalleryPhotosInfoError.Local.BadServerResponse,
            is ErrorCode.FavouritePhotoErrors.Local.BadServerResponse -> "Bad server response error"

            is ErrorCode.GetGalleryPhotosErrors.Remote.NoPhotosInRequest,
            is ErrorCode.GetGalleryPhotosInfoError.Remote.NoPhotosInRequest,
            is ErrorCode.ReceivePhotosErrors.Remote.NoPhotosInRequest -> "No photos were selected error"

            is ErrorCode.UploadPhotoErrors.Local.NoPhotoFileOnDisk -> "No photo file on disk error"
            is ErrorCode.ReceivePhotosErrors.Remote.TooManyPhotosRequested -> "Too many photos requested error"
            is ErrorCode.ReceivePhotosErrors.Remote.NoPhotosToSendBack -> "No photos to send back"
            is ErrorCode.ReceivePhotosErrors.Remote.NotEnoughPhotosUploaded -> "Upload more photos first"
            is ErrorCode.FavouritePhotoErrors.Remote.AlreadyFavourited -> "You have already added this photo to favourites"
            is ErrorCode.ReportPhotoErrors.Remote.AlreadyReported -> "You have already reported this photo"
            is ErrorCode.UploadPhotoErrors.Local.Interrupted -> "The process was interrupted by user"
            is ErrorCode.TakePhotoErrors.UnknownError -> "Could not take photo (unknown error)"
            is ErrorCode.TakePhotoErrors.CameraIsNotAvailable -> "Could not take photo (camera is not available)"
            is ErrorCode.TakePhotoErrors.CameraIsNotStartedException -> "Could not take photo (camera is not started)"
            is ErrorCode.TakePhotoErrors.TimeoutException -> "Could not take photo (exceeded maximum camera wait time)"
            is ErrorCode.TakePhotoErrors.DatabaseError -> "Could not take photo (database error)"
            is ErrorCode.TakePhotoErrors.CouldNotTakePhoto -> "Could not take photo (probably the view was disconnected)"
            is ErrorCode.GetGalleryPhotosErrors.Local.DatabaseError -> "Could not cache gallery photos in the database"
            is ErrorCode.UploadPhotoErrors.Remote.CouldNotGetUserId -> "Could not retrieve user id from the server"
            is ErrorCode.UploadPhotoErrors.Local.DatabaseError -> "Could not save user id in the database"
            is ErrorCode.GetGalleryPhotosInfoError.Local.DatabaseError -> "Could not save gallery photo info in the database"
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