package com.kirakishou.photoexchange.helper.service

import android.app.Service
import android.content.Intent
import android.os.IBinder
import com.kirakishou.photoexchange.PhotoExchangeApplication
import com.kirakishou.photoexchange.di.component.DaggerMainActivityComponent
import com.kirakishou.photoexchange.helper.api.ApiClient
import com.kirakishou.photoexchange.helper.rx.scheduler.SchedulerProvider
import com.kirakishou.photoexchange.mvvm.model.ServiceCommand
import timber.log.Timber
import javax.inject.Inject

class SendPhotoService : Service() {

    @Inject
    lateinit var apiClient: ApiClient

    @Inject
    lateinit var schedulers: SchedulerProvider

    private lateinit var presenter: SendPhotoServicePresenter

    override fun onCreate() {
        Timber.e("SendPhotoService start")
        super.onCreate()

        resolveDaggerDependency()

        presenter = SendPhotoServicePresenter(apiClient, schedulers)
    }

    override fun onDestroy() {
        Timber.e("SendPhotoService destroy")
        presenter.detach()

        super.onDestroy()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent != null) {
            handleCommand(intent)
        }

        return START_NOT_STICKY
    }

    private fun handleCommand(intent: Intent) {
        val commandRaw = intent.getIntExtra("command", -1)
        check(commandRaw != -1)

        val serviceCommand = ServiceCommand.from(commandRaw)

        when (serviceCommand) {
            ServiceCommand.SEND_PHOTO -> {

            }
        }
    }

    override fun onBind(intent: Intent): IBinder? = null

    private fun resolveDaggerDependency() {
        DaggerMainActivityComponent.builder()
                .applicationComponent(PhotoExchangeApplication.applicationComponent)
                .build()
                .inject(this)
    }
}
