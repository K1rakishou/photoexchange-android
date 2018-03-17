package com.kirakishou.photoexchange.ui.activity

import android.arch.lifecycle.ViewModelProviders
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.IBinder
import com.kirakishou.fixmypc.photoexchange.R
import com.kirakishou.photoexchange.PhotoExchangeApplication
import com.kirakishou.photoexchange.base.BaseActivity
import com.kirakishou.photoexchange.di.module.AllPhotosActivityModule
import com.kirakishou.photoexchange.helper.concurrency.coroutine.CoroutineThreadPoolProvider
import com.kirakishou.photoexchange.mvp.model.PhotoUploadingEvent
import com.kirakishou.photoexchange.mvp.model.other.LonLat
import com.kirakishou.photoexchange.service.UploadPhotoService
import com.kirakishou.photoexchange.mvp.view.AllPhotosActivityView
import com.kirakishou.photoexchange.mvp.viewmodel.AllPhotosActivityViewModel
import com.kirakishou.photoexchange.mvp.viewmodel.factory.AllPhotosActivityViewModelFactory
import com.kirakishou.photoexchange.ui.callback.ActivityCallback
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.rxkotlin.plusAssign
import io.reactivex.subjects.PublishSubject
import timber.log.Timber
import javax.inject.Inject


class AllPhotosActivity : BaseActivity<AllPhotosActivityViewModel>(), AllPhotosActivityView, ActivityCallback {

    @Inject
    lateinit var viewModelFactory: AllPhotosActivityViewModelFactory

    @Inject
    lateinit var coroutinesPool: CoroutineThreadPoolProvider

    private val tag = "[${this::class.java.simpleName}] "
    private var service: UploadPhotoService? = null

    private val onServiceConnectedSubject = PublishSubject.create<Unit>()

    override fun initViewModel(): AllPhotosActivityViewModel? {
        return ViewModelProviders.of(this, viewModelFactory).get(AllPhotosActivityViewModel::class.java)
    }

    override fun getContentView(): Int = R.layout.activity_all_photos

    override fun onActivityCreate(savedInstanceState: Bundle?, intent: Intent) {
        val serviceIntent = Intent(this, UploadPhotoService::class.java)

        startService(serviceIntent)
        bindService(serviceIntent, connection, Context.BIND_AUTO_CREATE)

        compositeDisposable += onServiceConnectedSubject
            .subscribeOn(AndroidSchedulers.mainThread())
            .observeOn(AndroidSchedulers.mainThread())
            .doOnNext { service?.attachCallback(this@AllPhotosActivity) }
            .doOnNext {
                val testUserId = "123"
                val testLocation = LonLat(55.411666, 44.225236)

                service?.startPhotosUploading(testUserId, testLocation)
            }
            .subscribe()
    }

    override fun onUploadingEvent(event: PhotoUploadingEvent) {
        when (event) {
            is PhotoUploadingEvent.onPrepare -> {
                Timber.e("onPrepare")
            }
            is PhotoUploadingEvent.onPhotoUploadingStart -> {
                Timber.e("onPhotoUploadingStart")
            }
            is PhotoUploadingEvent.onProgress -> {
                Timber.e("onProgress ${event.progress}")
            }
            is PhotoUploadingEvent.onUploaded -> {
                Timber.e("onUploaded")
            }
            is PhotoUploadingEvent.onFailedToUpload -> {
                Timber.e("onFailedToUpload")
            }
            is PhotoUploadingEvent.onUnknownError -> {
                Timber.e("onUnknownError")
            }
            is PhotoUploadingEvent.onEnd -> {
                Timber.e("onEnd")
            }
        }
    }

    override fun onActivityDestroy() {
        getViewModel().detach()

        service?.let { srvc ->
            srvc.detachCallback()
            unbindService(connection)
            service = null
        }
    }

    override fun showToast(message: String, duration: Int) {
        onShowToast(message, duration)
    }

    override fun resolveDaggerDependency() {
        (application as PhotoExchangeApplication).applicationComponent
            .plus(AllPhotosActivityModule(this))
            .inject(this)
    }

    private val connection = object : ServiceConnection {

        override fun onServiceConnected(className: ComponentName, _service: IBinder) {
            Timber.e("Service connected")

            service = (_service as UploadPhotoService.UploadPhotosBinder).getService()
            onServiceConnectedSubject.onNext(Unit)
        }

        override fun onServiceDisconnected(arg0: ComponentName) {
            Timber.e("Service disconnected")

            unbindService(this)
            service = null
        }
    }
}