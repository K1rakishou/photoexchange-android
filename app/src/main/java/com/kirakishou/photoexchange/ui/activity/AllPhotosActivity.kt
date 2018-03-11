package com.kirakishou.photoexchange.ui.activity

import android.arch.lifecycle.ViewModelProviders
import android.content.Intent
import android.os.Bundle
import com.kirakishou.fixmypc.photoexchange.R
import com.kirakishou.photoexchange.PhotoExchangeApplication
import com.kirakishou.photoexchange.base.BaseActivity
import com.kirakishou.photoexchange.di.module.AllPhotosActivityModule
import com.kirakishou.photoexchange.helper.service.UploadPhotoService
import com.kirakishou.photoexchange.mvp.view.AllPhotosActivityView
import com.kirakishou.photoexchange.mvp.viewmodel.AllPhotosActivityViewModel
import com.kirakishou.photoexchange.mvp.viewmodel.factory.AllPhotosActivityViewModelFactory
import javax.inject.Inject
import android.content.ComponentName
import android.content.Context
import android.os.IBinder
import android.content.ServiceConnection
import com.kirakishou.photoexchange.helper.concurrency.coroutine.CoroutineThreadPoolProvider
import kotlinx.coroutines.experimental.async
import kotlinx.coroutines.experimental.delay
import timber.log.Timber
import java.util.concurrent.TimeUnit


class AllPhotosActivity : BaseActivity<AllPhotosActivityViewModel>(), AllPhotosActivityView, ServiceCallback {

    @Inject
    lateinit var viewModelFactory: AllPhotosActivityViewModelFactory

    @Inject
    lateinit var coroutinesPool: CoroutineThreadPoolProvider

    private var service: UploadPhotoService? = null

    override fun initViewModel(): AllPhotosActivityViewModel? {
        return ViewModelProviders.of(this, viewModelFactory).get(AllPhotosActivityViewModel::class.java)
    }

    override fun getContentView(): Int = R.layout.activity_all_photos

    override fun onActivityCreate(savedInstanceState: Bundle?, intent: Intent) {
        val serviceIntent = Intent(this, UploadPhotoService::class.java)

        startService(serviceIntent)
        bindService(serviceIntent, connection, Context.BIND_AUTO_CREATE)

        async(coroutinesPool.BG()) {
            while (service == null) {
                delay(1, TimeUnit.SECONDS)
            }

            service?.setCallback(this@AllPhotosActivity)
        }
    }

    override fun onProgress(progress: Int) {
        Timber.e("Progress: $progress")
    }

    override fun onActivityDestroy() {
        getViewModel().detach()

        service?.removeCallback()

        if (service != null) {
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

        override fun onServiceConnected(className: ComponentName,
                                        _service: IBinder) {
            val binder = _service as UploadPhotoService.UploadPhotosBinder
            service = binder.getService()
        }

        override fun onServiceDisconnected(arg0: ComponentName) {
        }
    }


}

interface ServiceCallback {
    fun onProgress(progress: Int)
}